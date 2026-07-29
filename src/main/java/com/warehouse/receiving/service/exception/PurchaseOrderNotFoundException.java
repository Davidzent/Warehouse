package com.warehouse.receiving.service.exception;

// No purchase order with the given id. -> 404
public class PurchaseOrderNotFoundException extends RuntimeException {

    public PurchaseOrderNotFoundException(long poId) {
        super("Purchase order %d does not exist".formatted(poId));
    }
}
