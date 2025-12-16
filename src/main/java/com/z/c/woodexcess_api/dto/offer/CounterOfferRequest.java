package com.z.c.woodexcess_api.dto.offer;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CounterOfferRequest(
        @NotNull(message = "Counter price is required")
        @DecimalMin(value = "0.01", message = "Counter price must be greater than 0")
        @Digits(integer = 8, fraction = 2, message = "Invalid price format")
        BigDecimal counterPrice,

        @Size(max = 1000, message = "Message cannot exceed 1000 characters")
        String message
) {}
