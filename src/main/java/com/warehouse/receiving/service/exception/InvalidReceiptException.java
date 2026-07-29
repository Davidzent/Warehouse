package com.warehouse.receiving.service.exception;

// The request body is structurally fine (bean validation passed) but its
// CONTENT is wrong in ways only the database can reveal — e.g. it references
// a PO line that does not belong to that PO, or lists the same line twice. 
public class InvalidReceiptException extends RuntimeException {

    public InvalidReceiptException(String message) {
        super(message);
    }
}
