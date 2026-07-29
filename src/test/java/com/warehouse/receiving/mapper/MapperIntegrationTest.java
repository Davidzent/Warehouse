package com.warehouse.receiving.mapper;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import com.warehouse.receiving.domain.Inventory;
import com.warehouse.receiving.domain.PoStatus;
import com.warehouse.receiving.domain.PurchaseOrder;
import com.warehouse.receiving.domain.PurchaseOrderLine;
import com.warehouse.receiving.domain.Receipt;
import com.warehouse.receiving.dto.PoSort;
import com.warehouse.receiving.dto.PurchaseOrderSearchCriteria;
import com.warehouse.receiving.dto.PurchaseOrderSummary;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(EmbeddedPostgresTestConfig.class)
@Sql({"/schema.sql", "/data.sql"})
class MapperIntegrationTest {

    @Autowired private PurchaseOrderMapper purchaseOrderMapper;
    @Autowired private ReceiptMapper receiptMapper;
    @Autowired private InventoryMapper inventoryMapper;

    @Nested
    class FindPurchaseOrderById {

        @Test
        void assemblesHeaderVendorAndLinesFromJoinedRows() {
            PurchaseOrder po = purchaseOrderMapper.findById(1000L);

            assertThat(po).isNotNull();
            // Header columns land on the root object...
            assertThat(po.getPoNumber()).isEqualTo("PO-1000");
            assertThat(po.getStatus()).isEqualTo(PoStatus.OPEN);
            assertThat(po.getCreatedBy()).isEqualTo("seed");
            // ...the <association> builds the single nested Vendor...
            assertThat(po.getVendor()).isNotNull();
            assertThat(po.getVendor().getVendorCode()).isEqualTo("ACME");
            assertThat(po.getVendor().getName()).isEqualTo("Acme Industrial Supply");
            // ...and the <collection> folds 3 joined rows into 3 children.
            assertThat(po.getLines()).hasSize(3);
            assertThat(po.getLines())
                    .extracting(PurchaseOrderLine::getLineNumber)
                    .containsExactly(1, 2, 3); // ORDER BY line_number honored
            PurchaseOrderLine first = po.getLines().get(0);
            assertThat(first.getPoLineId()).isEqualTo(2000L);
            assertThat(first.getQuantityOrdered()).isEqualTo(50);
            assertThat(first.getQuantityReceived()).isEqualTo(0);
            assertThat(first.getSku()).isEqualTo("SKU-BOLT-M8"); // joined from product
        }

        @Test
        void returnsNullForUnknownId() {
            assertThat(purchaseOrderMapper.findById(999_999L)).isNull();
        }
    }

    @Nested
    class SearchDynamicSql {

        @Test
        void emptyCriteriaReturnsAllPurchaseOrders() {
            List<PurchaseOrderSummary> all = purchaseOrderMapper.search(criteria(null, null, null));
            assertThat(all).hasSize(4);
            // Aggregates come back too: PO-1000 has 3 lines, 290 ordered.
            PurchaseOrderSummary po1000 = all.stream()
                    .filter(s -> s.getPoNumber().equals("PO-1000")).findFirst().orElseThrow();
            assertThat(po1000.getLineCount()).isEqualTo(3);
            assertThat(po1000.getTotalOrdered()).isEqualTo(290);
            assertThat(po1000.getVendorName()).isEqualTo("Acme Industrial Supply");
        }

        @Test
        void statusListFilterUsesForeachInClause() {
            List<PurchaseOrderSummary> receivable = purchaseOrderMapper.search(
                    criteria(List.of(PoStatus.OPEN, PoStatus.PARTIALLY_RECEIVED), null, null));
            assertThat(receivable)
                    .extracting(PurchaseOrderSummary::getPoNumber)
                    .containsExactly("PO-1000", "PO-1001"); // sorted by po_number
        }

        @Test
        void filtersCombineWithAnd() {
            // vendor ACME(1) + text "100": matches PO-1000 and PO-1002 by
            // vendor, then "100" keeps both (PO-1000, PO-1002 contain "100").
            List<PurchaseOrderSummary> result = purchaseOrderMapper.search(
                    criteria(null, 1L, "100"));
            assertThat(result)
                    .extracting(PurchaseOrderSummary::getPoNumber)
                    .containsExactly("PO-1000", "PO-1002");
            // narrower text keeps only one
            assertThat(purchaseOrderMapper.search(criteria(null, 1L, "1002")))
                    .extracting(PurchaseOrderSummary::getPoNumber)
                    .containsExactly("PO-1002");
        }

        @Test
        void sortWhitelistOrdersByRequestedColumn() {
            List<PurchaseOrderSummary> byDate = purchaseOrderMapper.search(
                    new PurchaseOrderSearchCriteria(null, null, null, PoSort.ORDER_DATE));
            assertThat(byDate)
                    .extracting(PurchaseOrderSummary::getPoNumber)
                    .containsExactly("PO-1002", "PO-1003", "PO-1001", "PO-1000");
        }

        private PurchaseOrderSearchCriteria criteria(List<PoStatus> statuses, Long vendorId, String text) {
            return new PurchaseOrderSearchCriteria(statuses, vendorId, text, PoSort.PO_NUMBER);
        }
    }

    @Nested
    class RunningTotalUpdate {

        /** Proves the SQL-side increment actually adds (not overwrites). */
        @Test
        void addToLineReceivedQuantityIncrementsInPlace() {
            // Seed state: line 2010 (PO-1001) already has quantity_received = 5.
            int rows = purchaseOrderMapper.addToLineReceivedQuantity(2010L, 7);

            assertThat(rows).as("exactly one row updated").isEqualTo(1);
            PurchaseOrderLine after = purchaseOrderMapper.findLineById(2010L);
            assertThat(after.getQuantityReceived()).isEqualTo(5 + 7);
        }

        @Test
        void updateStatusAlsoMaintainsAuditColumns() {
            OffsetDateTime before = purchaseOrderMapper.findById(1000L).getUpdatedAt();

            int rows = purchaseOrderMapper.updateStatus(1000L, PoStatus.PARTIALLY_RECEIVED, "it-tester");

            assertThat(rows).isEqualTo(1);
            PurchaseOrder after = purchaseOrderMapper.findById(1000L);
            assertThat(after.getStatus()).isEqualTo(PoStatus.PARTIALLY_RECEIVED);
            assertThat(after.getUpdatedBy()).isEqualTo("it-tester");
            assertThat(after.getUpdatedAt()).isAfterOrEqualTo(before);
        }
    }

    @Nested
    class GeneratedKeys {

        /** useGeneratedKeys: the DB-assigned id appears ON the object after insert. */
        @Test
        void insertReceiptPopulatesGeneratedId() {
            Receipt receipt = new Receipt();
            receipt.setPoId(1000L);
            receipt.setReceivedBy("it-tester");
            receipt.setReceivedAt(OffsetDateTime.now());
            receipt.setCarrierReference("BOL-TEST");

            assertThat(receipt.getReceiptId()).isZero(); // before: nothing
            int rows = receiptMapper.insertReceipt(receipt);
            assertThat(rows).isEqualTo(1);
            // data.sql setval'd the sequence to 5000, so real ids are > 5000.
            assertThat(receipt.getReceiptId()).isGreaterThan(5000L); // after: real id

            // And the receipt resultMap round-trips it (second <collection> proof).
            Receipt reloaded = receiptMapper.findById(receipt.getReceiptId());
            assertThat(reloaded).isNotNull();
            assertThat(reloaded.getReceivedBy()).isEqualTo("it-tester");
            assertThat(reloaded.getLines()).isEmpty(); // no lines inserted here
        }
    }

    @Nested
    class InventoryUpsert {

        @Test
        void insertsThenAccumulatesOnConflict() {
            // (product 13, location 102) does not exist in seed data.
            inventoryMapper.upsertAddQuantity(13L, 102L, 5);
            inventoryMapper.upsertAddQuantity(13L, 102L, 7);

            Inventory row = inventoryMapper.findByProductAndLocation(13L, 102L);
            assertThat(row).isNotNull();
            assertThat(row.getQuantityOnHand()).isEqualTo(12); // 5 + 7, ONE row
        }

        @Test
        void accumulatesOntoSeededRow() {
            // Seeded: product 12 @ location 101 = 4 on hand.
            inventoryMapper.upsertAddQuantity(12L, 101L, 6);

            Inventory row = inventoryMapper.findByProductAndLocation(12L, 101L);
            assertThat(row.getQuantityOnHand()).isEqualTo(10);
        }
    }
}
