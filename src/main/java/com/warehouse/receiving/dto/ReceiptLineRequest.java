package com.warehouse.receiving.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;


public record ReceiptLineRequest(

        @NotNull(message = "poLineId is required")
        Long poLineId,

        @NotNull(message = "quantityReceived is required")
        @Positive(message = "quantityReceived must be at least 1")
        Integer quantityReceived,

        @NotNull(message = "quantityDamaged is required")
        @PositiveOrZero(message = "quantityDamaged cannot be negative")
        Integer quantityDamaged,

        @NotNull(message = "locationId is required")
        Long locationId
) {
    @AssertTrue(message = "quantityDamaged cannot exceed quantityReceived")
    public boolean isDamagedWithinReceived() {
        if (quantityReceived == null || quantityDamaged == null) {
            return true;
        }
        return quantityDamaged <= quantityReceived;
    }

    /**
     * GOOD units = physically arrived minus damaged-on-arrival. 
     */
    public int goodQuantity() {
        return quantityReceived - quantityDamaged;
    }
}
