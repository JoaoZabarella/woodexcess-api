package com.z.c.woodexcess_api.dto.listing;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.z.c.woodexcess_api.dto.address.AddressResponse;
import com.z.c.woodexcess_api.enums.Condition;
import com.z.c.woodexcess_api.enums.ListingStatus;
import com.z.c.woodexcess_api.enums.MaterialType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record ListingResponse(
        UUID id,
        String title,
        String description,
        MaterialType materialType,
        BigDecimal price,
        Integer quantity,
        Condition condition,
        @JsonProperty("city") String city,
        @JsonProperty("state") String state,
        ListingStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        ListingOwerResponse owner,
        AddressResponse address) {
}
