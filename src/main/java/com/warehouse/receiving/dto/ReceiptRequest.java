package com.warehouse.receiving.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Client-supplied input for recording a receipt.
 *
 * Deliberately narrower than {@link com.warehouse.receiving.domain.Receipt} to prevent over-posting: 
 * @param receiptId     is assigned by the database, 
 * @param receivedBy    comes from the verified JWT subject, and 
 * @param receivedAt    from the server clock. 
 * 
 * Fields absent from this type cannot be set by a client. 
 * Quantities and status are derived server-side as well —
 * clients report events, not state.
 *
 * @Valid on the list element type cascades validation into each line; without it
 * the line-level constraints are skipped.
 */
public record ReceiptRequest(

        @NotNull(message = "purchaseOrderId is required")
        Long purchaseOrderId,

        @Size(max = 50, message = "carrierReference is limited to 50 characters")
        String carrierReference,

        @Size(max = 500, message = "notes is limited to 500 characters")
        String notes,

        @NotEmpty(message = "a receipt must contain at least one line")
        List<@Valid ReceiptLineRequest> lines
) {}
