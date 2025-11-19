package com.z.c.woodexcess_api.dto.address;

import com.z.c.woodexcess_api.model.Address;
import lombok.Builder;

import java.util.UUID;
import java.time.LocalDateTime;

@Builder
public record AddressResponse(
        UUID id,
        String street,
        String number,
        String complement,
        String district,
        String city,
        String state,
        String zipCode,
        String country,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public AddressResponse (Address address){
        this(
                address.getId(),
                address.getStreet(),
                address.getNumber(),
                address.getComplement(),
                address.getDistrict(),
                address.getCity(),
                address.getState(),
                address.getZipCode(),
                address.getCountry(),
                address.getActive(),
                address.getCreatedAt(),
                address.getUpdatedAt()
        );
    }
}

