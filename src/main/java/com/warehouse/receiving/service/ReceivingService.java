package com.warehouse.receiving.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.warehouse.receiving.domain.PoStatus;
import com.warehouse.receiving.domain.PurchaseOrder;
import com.warehouse.receiving.domain.PurchaseOrderLine;
import com.warehouse.receiving.domain.Receipt;
import com.warehouse.receiving.domain.ReceiptLine;
import com.warehouse.receiving.dto.PurchaseOrderSearchCriteria;
import com.warehouse.receiving.dto.PurchaseOrderSummary;
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
import com.warehouse.receiving.service.exception.ReceiptNotFoundException;


@Service
public class ReceivingService {

    /** Over-receipt tolerance: total received may reach 110% of ordered, not more. */
    private static final int OVER_RECEIPT_PERCENT = 110;

    private final PurchaseOrderMapper purchaseOrderMapper;
    private final ReceiptMapper receiptMapper;
    private final InventoryMapper inventoryMapper;

    public ReceivingService(PurchaseOrderMapper purchaseOrderMapper,
                            ReceiptMapper receiptMapper,
                            InventoryMapper inventoryMapper) {
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.receiptMapper = receiptMapper;
        this.inventoryMapper = inventoryMapper;
    }

    /**
     * Record one physical delivery against a purchase order.
     *
     * @param receivedBy identity of the clerk — taken from the VERIFIED JWT
     *                   subject by the controller, never from the request body.
     */
    @Transactional
    public ReceiptResponse receive(ReceiptRequest request, String receivedBy) {
        long poId = request.purchaseOrderId();

        // Lock the row
        purchaseOrderMapper.lockHeader(poId);

        // Load the aggregate and run the cheap gate checks
        PurchaseOrder po = purchaseOrderMapper.findById(poId);
        if (po == null) {
            throw new PurchaseOrderNotFoundException(poId); // -> 404
        }
        if (!po.getStatus().isReceivable()) {
            throw new PoNotReceivableException(po.getPoNumber(), po.getStatus()); // -> 409
        }

        Map<Long, PurchaseOrderLine> linesById = po.getLines().stream()
                .collect(Collectors.toMap(PurchaseOrderLine::getPoLineId, Function.identity()));

        // Validate
        Set<Long> seenLineIds = new HashSet<>();
        for (ReceiptLineRequest lineReq : request.lines()) {
            if (!seenLineIds.add(lineReq.poLineId())) {
                throw new InvalidReceiptException(
                        "PO line %d appears more than once in the request; merge the quantities"
                                .formatted(lineReq.poLineId())); // -> 400
            }
            PurchaseOrderLine poLine = linesById.get(lineReq.poLineId());
            if (poLine == null) {
                throw new InvalidReceiptException(
                        "PO line %d does not belong to purchase order %d"
                                .formatted(lineReq.poLineId(), poId)); // -> 400
            }
            checkOverReceipt(poLine, lineReq.quantityReceived()); // -> 409 if beyond 110%
        }

        // Write the receipt header
        Receipt receipt = new Receipt();
        receipt.setPoId(poId);
        receipt.setReceivedBy(receivedBy);
        receipt.setReceivedAt(OffsetDateTime.now());
        receipt.setCarrierReference(request.carrierReference());
        receipt.setNotes(request.notes());
        receiptMapper.insertReceipt(receipt);

        // Per line: receipt detail row, running-total bump, inventory
        List<ReceiptResponse.Line> responseLines = new ArrayList<>();
        for (ReceiptLineRequest lineReq : request.lines()) {
            PurchaseOrderLine poLine = linesById.get(lineReq.poLineId());

            ReceiptLine receiptLine = new ReceiptLine();
            receiptLine.setReceiptId(receipt.getReceiptId());
            receiptLine.setPoLineId(lineReq.poLineId());
            receiptLine.setLocationId(lineReq.locationId());
            receiptLine.setQuantityReceived(lineReq.quantityReceived());
            receiptLine.setQuantityDamaged(lineReq.quantityDamaged());
            receiptMapper.insertReceiptLine(receiptLine);

            // Running total counts GROSS units (damaged included)
            purchaseOrderMapper.addToLineReceivedQuantity(lineReq.poLineId(), lineReq.quantityReceived());
           
            // Mirror the increment on the copy
            poLine.setQuantityReceived(poLine.getQuantityReceived() + lineReq.quantityReceived());

            // Only GOOD units become usable stock
            int goodQuantity = lineReq.goodQuantity();
            if (goodQuantity > 0) {
                inventoryMapper.upsertAddQuantity(
                        poLine.getProductId(), lineReq.locationId(), goodQuantity);
            }

            responseLines.add(new ReceiptResponse.Line(
                    lineReq.poLineId(),
                    poLine.getSku(),
                    lineReq.quantityReceived(),
                    lineReq.quantityDamaged(),
                    goodQuantity,
                    lineReq.locationId()));
        }

        // Keep the header status in sync with its lines.
        PoStatus statusAfter = statusAfterReceipt(po);
        if (statusAfter != po.getStatus()) {
            purchaseOrderMapper.updateStatus(poId, statusAfter, receivedBy);
        }

        return new ReceiptResponse(
                receipt.getReceiptId(), poId, statusAfter,
                receivedBy, receipt.getReceivedAt(), responseLines);
    }

    /**
     * Gross over-receipt guard. Integer arithmetic on purpose:
     * "total*100 > ordered*110" is exactly "total > ordered*1.1" without
     * floating point
     */
    private void checkOverReceipt(PurchaseOrderLine poLine, int quantityArriving) {
        long totalAfter = (long) poLine.getQuantityReceived() + quantityArriving;
        // total > ordered * 110%  <=>  total * 100 > ordered * 110
        if (totalAfter * 100L > (long) poLine.getQuantityOrdered() * OVER_RECEIPT_PERCENT) {
            int maxAllowed = (int) ((long) poLine.getQuantityOrdered() * OVER_RECEIPT_PERCENT / 100);
            throw new OverReceiptException(
                    poLine.getPoLineId(),
                    poLine.getQuantityOrdered(),
                    poLine.getQuantityReceived(),
                    quantityArriving,
                    maxAllowed);
        }
    }

    /**
     * CLOSED when every line reached its ordered quantity, otherwise
     * PARTIALLY_RECEIVED (we know at least one unit just arrived, so plain
     * OPEN is no longer accurate). Small note: over-tolerance receipts
     * (105% etc.) also count as complete.
     */
    private PoStatus statusAfterReceipt(PurchaseOrder po) {
        boolean everyLineComplete = po.getLines().stream()
                .allMatch(l -> l.getQuantityReceived() >= l.getQuantityOrdered());
        return everyLineComplete ? PoStatus.CLOSED : PoStatus.PARTIALLY_RECEIVED;
    }

    // ------------------------------------------------------------------
    // Read operations
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PurchaseOrder getPurchaseOrder(long poId) {
        PurchaseOrder po = purchaseOrderMapper.findById(poId);
        if (po == null) {
            throw new PurchaseOrderNotFoundException(poId); // -> 404
        }
        return po;
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderSummary> searchPurchaseOrders(PurchaseOrderSearchCriteria criteria) {
        return purchaseOrderMapper.search(criteria);
    }

    @Transactional(readOnly = true)
    public Receipt getReceipt(long receiptId) {
        Receipt receipt = receiptMapper.findById(receiptId);
        if (receipt == null) {
            throw new ReceiptNotFoundException(receiptId); // -> 404
        }
        return receipt;
    }
}
