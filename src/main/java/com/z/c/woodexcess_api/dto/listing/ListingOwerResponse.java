package com.z.c.woodexcess_api.dto.listing;

import lombok.Builder;

import java.util.UUID;

@Builder
public record ListingOwerResponse(
        UUID id,
        String name,
        String email,
        String phone
        ){


}
