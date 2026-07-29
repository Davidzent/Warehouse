package com.warehouse.receiving.service;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.warehouse.receiving.domain.PoStatus;
import com.warehouse.receiving.domain.PurchaseOrder;
import com.warehouse.receiving.domain.PurchaseOrderLine;
import com.warehouse.receiving.dto.ReceiptLineRequest;
import com.warehouse.receiving.dto.ReceiptRequest;
import com.warehouse.receiving.dto.ReceiptResponse;
import com.warehouse.receiving.mapper.InventoryMapper;
import com.warehouse.receiving.mapper.PurchaseOrderMapper;
import com.warehouse.receiving.mapper.ReceiptMapper;
import com.warehouse.receiving.service.exception.InvalidReceiptException;
import com.warehouse.receiving.service.exception.OverReceiptException;
import com.warehouse.receiving.service.exception.PoNotReceivableException;
import com.warehouse.receiving.service.exception.PurchaseOrderNotFoundException;

@ExtendWith(MockitoExtension.class)
class ReceivingServiceTest {

    private static final long PO_ID = 1000L;
    private static final long LINE_ID = 2000L;
    private static final long PRODUCT_ID = 10L;
    private static final long LOCATION_ID = 101L;
    private static final String CLERK = "clerk-1";

    @Mock private PurchaseOrderMapper purchaseOrderMapper;
    @Mock private ReceiptMapper receiptMapper;
    @Mock private InventoryMapper inventoryMapper;

    private ReceivingService service;

    @BeforeEach
    void setUp() {
        service = new ReceivingService(purchaseOrderMapper, receiptMapper, inventoryMapper);
    }

    // =====================================================================
    // Rule 1: no receiving against CLOSED or CANCELLED purchase orders,
    // and — just as important — a rejected request writes NOTHING.
    // =====================================================================

    @ParameterizedTest
    @EnumSource(value = PoStatus.class, names = {"CLOSED", "CANCELLED"})
    void receivingAgainstNonReceivablePoThrowsAndWritesNothing(PoStatus terminalStatus) {
        // Arrange: the mapper will report a PO in a terminal state.
        when(purchaseOrderMapper.findById(PO_ID))
                .thenReturn(po(terminalStatus, line(LINE_ID, PRODUCT_ID, 50, 0)));

        assertThatThrownBy(() -> service.receive(request(lineRequest(LINE_ID, 5, 0)), CLERK))
                .isInstanceOf(PoNotReceivableException.class)
                .hasMessageContaining(terminalStatus.name());

        // Assert: nothing was written anywhere.
        verify(receiptMapper, never()).insertReceipt(any());
        verify(receiptMapper, never()).insertReceiptLine(any());
        verify(purchaseOrderMapper, never()).addToLineReceivedQuantity(anyLong(), anyInt());
        verify(purchaseOrderMapper, never()).updateStatus(anyLong(), any(), anyString());
        verifyNoInteractions(inventoryMapper);
    }

    @Test
    void receivingAgainstUnknownPoThrowsNotFound() {
        when(purchaseOrderMapper.findById(PO_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.receive(request(lineRequest(LINE_ID, 5, 0)), CLERK))
                .isInstanceOf(PurchaseOrderNotFoundException.class);

        verifyNoInteractions(receiptMapper, inventoryMapper);
    }

    // =====================================================================
    // Rule 2: gross over-receipt beyond 110% of ordered is rejected.
    // =====================================================================

    @Test
    void overReceiptBeyondToleranceThrowsAndWritesNothing() {
        when(purchaseOrderMapper.findById(PO_ID))
                .thenReturn(po(PoStatus.PARTIALLY_RECEIVED, line(LINE_ID, PRODUCT_ID, 10, 8)));

        assertThatThrownBy(() -> service.receive(request(lineRequest(LINE_ID, 4, 0)), CLERK))
                .isInstanceOf(OverReceiptException.class)
                .hasMessageContaining("max allowed in total is 11");

        verify(receiptMapper, never()).insertReceipt(any());
        verifyNoInteractions(inventoryMapper);
    }

    /**
     * BOUNDARY test: exactly 110% must PASS. Off-by-one bugs live on
     * boundaries; a test suite that only probes far from the edge will
     * happily certify "> where >= was meant" (and vice versa).
     */
    @Test
    void receiptAtExactlyOneHundredTenPercentIsAccepted() {
        when(purchaseOrderMapper.findById(PO_ID))
                .thenReturn(po(PoStatus.PARTIALLY_RECEIVED, line(LINE_ID, PRODUCT_ID, 10, 8)));

        // 8 + 3 = 11 = exactly 110% of 10 -> allowed.
        ReceiptResponse response = service.receive(request(lineRequest(LINE_ID, 3, 0)), CLERK);

        // 11 >= 10: the line is complete, so the PO must CLOSE.
        assertThat(response.purchaseOrderStatusAfter()).isEqualTo(PoStatus.CLOSED);
        verify(purchaseOrderMapper).addToLineReceivedQuantity(LINE_ID, 3);
        verify(purchaseOrderMapper).updateStatus(PO_ID, PoStatus.CLOSED, CLERK);
    }

    // =====================================================================
    // Rule 3: damaged units count toward the
    // PO's running total but must NOT enter inventory.
    // =====================================================================


    // 5 arrive, 2 damaged -> inventory gains 3. GOOD units only.
    @Test
    void damagedUnitsDoNotEnterInventory() {
        when(purchaseOrderMapper.findById(PO_ID))
                .thenReturn(po(PoStatus.OPEN, line(LINE_ID, PRODUCT_ID, 50, 0)));

        service.receive(request(lineRequest(LINE_ID, 5, 2)), CLERK);

        ArgumentCaptor<Integer> quantity = ArgumentCaptor.forClass(Integer.class);
        verify(inventoryMapper).upsertAddQuantity(eq(PRODUCT_ID), eq(LOCATION_ID), quantity.capture());
        assertThat(quantity.getValue())
                .as("inventory must receive only GOOD units (received 5 - damaged 2)")
                .isEqualTo(3);

        verify(purchaseOrderMapper).addToLineReceivedQuantity(LINE_ID, 5);
    }

    // Everything damaged -> inventory untouched entirely (no zero-quantity rows).
    @Test
    void fullyDamagedDeliveryTouchesNoInventory() {
        when(purchaseOrderMapper.findById(PO_ID))
                .thenReturn(po(PoStatus.OPEN, line(LINE_ID, PRODUCT_ID, 50, 0)));

        service.receive(request(lineRequest(LINE_ID, 2, 2)), CLERK);

        verify(inventoryMapper, never()).upsertAddQuantity(anyLong(), anyLong(), anyInt());
        
        // but the delivery still happened: receipt + running total recorded.
        verify(receiptMapper).insertReceipt(any());
        verify(purchaseOrderMapper).addToLineReceivedQuantity(LINE_ID, 2);
    }

    // =====================================================================
    // Rule 4: header status stays in sync with line fulfillment.
    // =====================================================================

    @Test
    void partialReceiptMovesPoToPartiallyReceived() {
        when(purchaseOrderMapper.findById(PO_ID))
                .thenReturn(po(PoStatus.OPEN, line(LINE_ID, PRODUCT_ID, 50, 0)));

        ReceiptResponse response = service.receive(request(lineRequest(LINE_ID, 5, 0)), CLERK);

        assertThat(response.purchaseOrderStatusAfter()).isEqualTo(PoStatus.PARTIALLY_RECEIVED);
        verify(purchaseOrderMapper).updateStatus(PO_ID, PoStatus.PARTIALLY_RECEIVED, CLERK);
    }

    @Test
    void completingEveryLineClosesThePo() {
        // Two lines, one already fully received; the receipt finishes the other.
        when(purchaseOrderMapper.findById(PO_ID)).thenReturn(po(PoStatus.PARTIALLY_RECEIVED,
                line(LINE_ID, PRODUCT_ID, 20, 20),
                line(2001L, 11L, 10, 4)));

        ReceiptResponse response = service.receive(request(lineRequest(2001L, 6, 0)), CLERK);

        assertThat(response.purchaseOrderStatusAfter()).isEqualTo(PoStatus.CLOSED);
        verify(purchaseOrderMapper).updateStatus(PO_ID, PoStatus.CLOSED, CLERK);
    }

    // =====================================================================
    // Request-content rules the DTO annotations cannot express.
    // =====================================================================

    @Test
    void lineNotBelongingToThePoIsRejected() {
        when(purchaseOrderMapper.findById(PO_ID))
                .thenReturn(po(PoStatus.OPEN, line(LINE_ID, PRODUCT_ID, 50, 0)));

        assertThatThrownBy(() -> service.receive(request(lineRequest(9999L, 5, 0)), CLERK))
                .isInstanceOf(InvalidReceiptException.class)
                .hasMessageContaining("9999");

        verify(receiptMapper, never()).insertReceipt(any());
    }

    @Test
    void duplicateLineInOneRequestIsRejected() {
        when(purchaseOrderMapper.findById(PO_ID))
                .thenReturn(po(PoStatus.OPEN, line(LINE_ID, PRODUCT_ID, 50, 0)));

        assertThatThrownBy(() -> service.receive(
                request(lineRequest(LINE_ID, 3, 0), lineRequest(LINE_ID, 2, 0)), CLERK))
                .isInstanceOf(InvalidReceiptException.class)
                .hasMessageContaining("more than once");

        verify(receiptMapper, never()).insertReceipt(any());
    }

    // =====================================================================
    // Tiny fixture builders. Tests read top-down as scenarios; the noise of
    // object construction is pushed down here.
    // =====================================================================

    private static PurchaseOrder po(PoStatus status, PurchaseOrderLine... lines) {
        PurchaseOrder po = new PurchaseOrder();
        po.setPoId(PO_ID);
        po.setPoNumber("PO-" + PO_ID);
        po.setStatus(status);
        po.setLines(new ArrayList<>(List.of(lines)));
        return po;
    }

    private static PurchaseOrderLine line(long poLineId, long productId, int ordered, int received) {
        PurchaseOrderLine line = new PurchaseOrderLine();
        line.setPoLineId(poLineId);
        line.setPoId(PO_ID);
        line.setProductId(productId);
        line.setSku("SKU-" + productId);
        line.setQuantityOrdered(ordered);
        line.setQuantityReceived(received);
        return line;
    }

    private static ReceiptLineRequest lineRequest(long poLineId, int received, int damaged) {
        return new ReceiptLineRequest(poLineId, received, damaged, LOCATION_ID);
    }

    private static ReceiptRequest request(ReceiptLineRequest... lines) {
        return new ReceiptRequest(PO_ID, null, null, List.of(lines));
    }
}
