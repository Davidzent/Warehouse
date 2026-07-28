package com.warehouse.receiving.dto;

import java.time.OffsetDateTime;
import java.util.List;

import com.warehouse.receiving.domain.PoStatus;

public record ReceiptResponse(
        long receiptId,
        long purchaseOrderId,
        PoStatus purchaseOrderStatusAfter,
        String receivedBy,
        OffsetDateTime receivedAt,
        List<Line> lines
) {
    public record Line(
            long poLineId,
            String sku,
            int quantityReceived,
            int quantityDamaged,
            int goodQuantity,      // received - damaged: what actually hit inventory
            long locationId
    ) {}
}
