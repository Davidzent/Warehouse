package com.warehouse.receiving.domain;

public enum PoStatus {
    OPEN,
    PARTIALLY_RECEIVED,
    CLOSED,
    CANCELLED;

    public boolean isReceivable() {
        return this == OPEN || this == PARTIALLY_RECEIVED;
    }
}
