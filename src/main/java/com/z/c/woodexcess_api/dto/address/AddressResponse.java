package com.z.c.woodexcess_api.dto.address;

import lombok.Builder;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record AddressResponse(
        UUID id,
        UUID userId,
        String street,
        String number,
        String complement,
        String district,
        String city,
        String state,
        String zipCode,
        String country,
        Boolean active,
        Boolean isPrimary,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String fullAddress) {
}
