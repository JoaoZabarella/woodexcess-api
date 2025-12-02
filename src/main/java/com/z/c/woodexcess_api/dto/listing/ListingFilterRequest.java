package com.z.c.woodexcess_api.dto.listing;

import com.z.c.woodexcess_api.enums.Condition;
import com.z.c.woodexcess_api.enums.ListingStatus;
import com.z.c.woodexcess_api.enums.MaterialType;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ListingFilterRequest(
        MaterialType materialType,
        String city,
        String state,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Condition condition,
        ListingStatus status) {
}
