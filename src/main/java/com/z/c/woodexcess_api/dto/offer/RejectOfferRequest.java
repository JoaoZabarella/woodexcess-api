package com.z.c.woodexcess_api.dto.offer;

import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record RejectOfferRequest(
        @Size(max = 500, message = "Reason cannot exceed 500 characters")
        String reason
) {}