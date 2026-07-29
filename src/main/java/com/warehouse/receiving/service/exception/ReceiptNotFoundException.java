package com.warehouse.receiving.service.exception;

// No receipt with the given id. -> 404
public class ReceiptNotFoundException extends RuntimeException {

    public ReceiptNotFoundException(long receiptId) {
        super("Receipt %d does not exist".formatted(receiptId));
    }
}
