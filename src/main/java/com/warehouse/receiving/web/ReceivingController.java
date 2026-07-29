package com.warehouse.receiving.web;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.warehouse.receiving.dto.ReceiptDetailResponse;
import com.warehouse.receiving.dto.ReceiptRequest;
import com.warehouse.receiving.dto.ReceiptResponse;
import com.warehouse.receiving.service.ReceivingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/receipts")
public class ReceivingController {

    private final ReceivingService receivingService;

    public ReceivingController(ReceivingService receivingService) {
        this.receivingService = receivingService;
    }

    /**
     * SECURITY:
     * 1. @PreAuthorize: only WAREHOUSE_CLERK tokens may post receipts
     * 2. @Valid: violations raise MethodArgumentNotValidException -> 400 via ApiExceptionHandler.
     */
    @PostMapping
    @PreAuthorize("hasRole('WAREHOUSE_CLERK')")
    public ResponseEntity<ReceiptResponse> createReceipt(@Valid @RequestBody ReceiptRequest request,
                                                         @AuthenticationPrincipal Jwt jwt) {
        ReceiptResponse response = receivingService.receive(request, jwt.getSubject());
        return ResponseEntity
                .created(URI.create("/api/receipts/" + response.receiptId()))
                .body(response);
    }

    // GET /api/receipts/{id} — 200 with the receipt, or 404. Any authenticated role.
    @GetMapping("/{receiptId}")
    public ReceiptDetailResponse getReceipt(@PathVariable long receiptId) {
        return ReceiptDetailResponse.from(receivingService.getReceipt(receiptId));
    }
}
