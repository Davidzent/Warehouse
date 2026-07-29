package com.warehouse.receiving.service.exception;

import com.warehouse.receiving.domain.PoStatus;

// Receiving attempted against a CLOSED or CANCELLED purchase order. -> 409

public class PoNotReceivableException extends BusinessConflictException {

    public PoNotReceivableException(String poNumber, PoStatus status) {
        super("Purchase order %s is %s and cannot accept receipts".formatted(poNumber, status));
    }
}
