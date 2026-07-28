package com.warehouse.receiving.dto;

import java.time.OffsetDateTime;
import java.util.List;

import com.warehouse.receiving.domain.Receipt;

public record ReceiptDetailResponse(
        long receiptId,
        long purchaseOrderId,
        String receivedBy,
        OffsetDateTime receivedAt,
        String carrierReference,
        String notes,
        List<Line> lines
) {
    public record Line(
            long receiptLineId,
            long poLineId,
            String sku,
            long locationId,
            String locationCode,
            int quantityReceived,
            int quantityDamaged
    ) {}

    public static ReceiptDetailResponse from(Receipt receipt) {
        List<Line> lines = receipt.getLines().stream()
                .map(l -> new Line(
                        l.getReceiptLineId(),
                        l.getPoLineId(),
                        l.getSku(),
                        l.getLocationId(),
                        l.getLocationCode(),
                        l.getQuantityReceived(),
                        l.getQuantityDamaged()))
                .toList();
        return new ReceiptDetailResponse(
                receipt.getReceiptId(),
                receipt.getPoId(),
                receipt.getReceivedBy(),
                receipt.getReceivedAt(),
                receipt.getCarrierReference(),
                receipt.getNotes(),
                lines);
    }
}
