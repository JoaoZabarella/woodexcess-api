package com.z.c.woodexcess_api.dto.listing;

import com.z.c.woodexcess_api.enums.Condition;
import com.z.c.woodexcess_api.enums.ListingStatus;
import com.z.c.woodexcess_api.enums.MaterialType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingFilterRequest {
    private MaterialType materialType;
    private String city;
    private String state;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Condition condition;
    private ListingStatus status;
}
