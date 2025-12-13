package com.z.c.woodexcess_api.dto.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.UUID;

@Builder
public record MessageRequest(
        @NotNull(message = "Recipient ID is required")
        UUID recipientId,

        @NotNull(message = "Listing ID is required")
        UUID listingId,

        @NotBlank(message = "Message content cannot be empty")
        @Size(min = 1, max = 5000, message = "Message must be between 1 and 5000 characters")
        String content
) {}
