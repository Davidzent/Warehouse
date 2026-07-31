package com.warehouse.receiving.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.warehouse.receiving.service.exception.BusinessConflictException;
import com.warehouse.receiving.service.exception.InvalidReceiptException;
import com.warehouse.receiving.service.exception.PurchaseOrderNotFoundException;
import com.warehouse.receiving.service.exception.ReceiptNotFoundException;

/**
 * BUSINESS to HTTP status codes. The status-code contract of this API: 201
 * resource created (POST /api/receipts success) 200 read success 400 the
 * REQUEST is bad (malformed JSON, failed validation, bad refs) — fix the
 * request and retry. 401 no/invalid token (issued by the security filter, not
 * this class) 403 valid token, insufficient role 404 the resource does not
 * exist 409 request fine, resource exists, but business STATE forbids it —
 * receiving on a CLOSED PO, breaching the 110% cap. 500 our bug. Never the
 * client's fault, never leaks internals.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    // ---- 404 -------------------------------------------------------------
    @ExceptionHandler({PurchaseOrderNotFoundException.class, ReceiptNotFoundException.class})
    public ProblemDetail handleNotFound(RuntimeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Not Found");
        return problem;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResource(NoResourceFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, "No endpoint at this path");
        problem.setTitle("Not Found");
        return problem;
    }

    // ---- 409 -------------------------------------------------------------
    @ExceptionHandler(BusinessConflictException.class)
    public ProblemDetail handleConflict(BusinessConflictException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Business Conflict");
        return problem;
    }

    // ---- 400 -------------------------------------------------------------
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe
                -> // merge: keep the first message if a field has several violations
                fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Request validation failed");
        problem.setTitle("Validation Error");
        problem.setProperty("fieldErrors", fieldErrors);
        return problem;
    }

    // Bad references / duplicate lines detected by the service.
    @ExceptionHandler(InvalidReceiptException.class)
    public ProblemDetail handleInvalidReceipt(InvalidReceiptException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid Receipt");
        return problem;
    }

    // Unparseable JSON body.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadable(HttpMessageNotReadableException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Malformed request body");
        problem.setTitle("Malformed Request");
        return problem;
    }

    // e.g. ?sort=NOT_A_COLUMN or /api/purchase-orders/abc.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Parameter '%s' has an invalid value".formatted(ex.getName()));
        problem.setTitle("Invalid Parameter");
        return problem;
    }

    /**
     * Database constraint violations that slipped past service validation —
     * e.g. a locationId that does not exist trips the receipt_line FK.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "A referenced entity does not exist or a database constraint was violated");
        problem.setTitle("Constraint Violation");
        return problem;
    }

    // ---- 403 -------------------------------------------------------------
    /**
     * Thrown by @PreAuthorize when the token's roles are insufficient. Handled
     * explicitly so it does not fall into the 500 catch-all below. This status
     * is what the "hidden button is not security" demo shows: a VIEWER posting
     * via curl lands here, UI or no UI.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, "Your role does not permit this operation");
        problem.setTitle("Forbidden");
        return problem;
    }

    // ---- 500 -------------------------------------------------------------
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problem.setTitle("Internal Server Error");
        return problem;
    }
}
