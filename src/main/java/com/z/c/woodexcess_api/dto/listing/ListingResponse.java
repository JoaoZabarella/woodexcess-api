package com.z.c.woodexcess_api.dto.listing;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.z.c.woodexcess_api.dto.address.AddressResponse;
import com.z.c.woodexcess_api.dto.user.UserResponse;
import com.z.c.woodexcess_api.enums.Condition;
import com.z.c.woodexcess_api.enums.ListingStatus;
import com.z.c.woodexcess_api.enums.MaterialType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingResponse {
    private UUID id;
    private String title;
    private String description;
    private MaterialType materialType;
    private BigDecimal price;
    private Integer quantity;
    private Condition condition;
    @JsonProperty("city")
    private String city;
    @JsonProperty("state")
    private String state;
    private ListingStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Nested objects
    private ListingOwerResponse owner;
    private AddressResponse address;
}
