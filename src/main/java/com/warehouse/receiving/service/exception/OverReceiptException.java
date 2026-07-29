package com.warehouse.receiving.service.exception;

// A line would exceed 110% of its ordered quantity. -> 409

public class OverReceiptException extends BusinessConflictException {

    public OverReceiptException(long poLineId, int ordered, int alreadyReceived,
                                int attempting, int maxAllowed) {
        super(("Over-receipt blocked for PO line %d: ordered %d, already received %d, "
                + "attempting %d more; max allowed in total is %d (110%%)")
                .formatted(poLineId, ordered, alreadyReceived, attempting, maxAllowed));
    }
}
