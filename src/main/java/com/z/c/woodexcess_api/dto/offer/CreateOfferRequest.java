package com.z.c.woodexcess_api.dto.offer;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record CreateOfferRequest(
        @NotNull(message = "Listing ID is required")
        UUID listingId,

        @NotNull(message = "Offered price is required")
        @DecimalMin(value = "0.01", message = "Offered price must be greater than 0")
        @Digits(integer = 8, fraction = 2, message = "Invalid price format")
        BigDecimal offeredPrice,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 100000, message = "Quantity cannot exceed 100000")
        Integer quantity,

        @Size(max = 1000, message = "Message cannot exceed 1000 characters")
        String message
) {}
