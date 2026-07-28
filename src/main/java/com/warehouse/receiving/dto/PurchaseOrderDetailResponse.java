package com.warehouse.receiving.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import com.warehouse.receiving.domain.PoStatus;
import com.warehouse.receiving.domain.PurchaseOrder;

/**
 *   remainingQuantity  — ordered minus received (floor 0), what the clerk
 *                        still expects.
 *   maxReceivableNow   — how many more units the server will accept before
 *                        the 110% over-receipt rule trips.
 */
public record PurchaseOrderDetailResponse(
        long poId,
        String poNumber,
        String vendorCode,
        String vendorName,
        PoStatus status,
        boolean receivable,
        LocalDate orderDate,
        LocalDate expectedDate,
        OffsetDateTime createdAt,
        String createdBy,
        OffsetDateTime updatedAt,
        String updatedBy,
        List<Line> lines
) {
    public record Line(
            long poLineId,
            int lineNumber,
            long productId,
            String sku,
            String productDescription,
            int quantityOrdered,
            int quantityReceived,
            int remainingQuantity,
            int maxReceivableNow
    ) {}

    /** Mapping entity -> DTO in one visible place (no mapper-framework magic). */
    public static PurchaseOrderDetailResponse from(PurchaseOrder po) {
        List<Line> lines = po.getLines().stream()
                .map(l -> new Line(
                        l.getPoLineId(),
                        l.getLineNumber(),
                        l.getProductId(),
                        l.getSku(),
                        l.getProductDescription(),
                        l.getQuantityOrdered(),
                        l.getQuantityReceived(),
                        Math.max(0, l.getQuantityOrdered() - l.getQuantityReceived()),

                        // cap = floor(ordered * 1.1), remaining headroom under the cap.
                        Math.max(0, (l.getQuantityOrdered() * 11) / 10 - l.getQuantityReceived())
                ))
                .toList();
        return new PurchaseOrderDetailResponse(
                po.getPoId(),
                po.getPoNumber(),
                po.getVendor() != null ? po.getVendor().getVendorCode() : null,
                po.getVendor() != null ? po.getVendor().getName() : null,
                po.getStatus(),
                po.getStatus().isReceivable(),
                po.getOrderDate(),
                po.getExpectedDate(),
                po.getCreatedAt(),
                po.getCreatedBy(),
                po.getUpdatedAt(),
                po.getUpdatedBy(),
                lines);
    }
}
