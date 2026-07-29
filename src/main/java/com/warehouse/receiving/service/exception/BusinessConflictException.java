package com.warehouse.receiving.service.exception;



// Base class for "your request is well-formed and the resource exists, but
// the CURRENT STATE of the business forbids it" — the meaning of HTTP 409.

public abstract class BusinessConflictException extends RuntimeException {

    protected BusinessConflictException(String message) {
        super(message);
    }
}
