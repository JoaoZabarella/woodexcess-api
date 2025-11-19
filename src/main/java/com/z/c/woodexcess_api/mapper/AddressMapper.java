package com.z.c.woodexcess_api.mapper;

import com.z.c.woodexcess_api.dto.address.AddressRequest;
import com.z.c.woodexcess_api.dto.address.AddressResponse;
import com.z.c.woodexcess_api.model.Address;
import com.z.c.woodexcess_api.model.User;
import org.springframework.stereotype.Component;

@Component
public class AddressMapper {

    public Address toEntity(AddressRequest request, User user) {
        return Address.builder()
                .street(request.street())
                .number(request.number())
                .complement(request.complement())
                .district(request.district())
                .city(request.city())
                .state(request.state().toUpperCase())
                .zipCode(formatZipCode(request.zipCode()))
                .country(request.country() != null ? request.country() : "Brazil")
                .user(user)
                .active(true)
                .build();
    }

    public AddressResponse toResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .street(address.getStreet())
                .number(address.getNumber())
                .complement(address.getComplement())
                .district(address.getDistrict())
                .city(address.getCity())
                .state(address.getState())
                .zipCode(address.getZipCode())
                .country(address.getCountry())
                .active(address.getActive())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
    }

    public void updateEntity(Address address, AddressRequest request) {
        address.setStreet(request.street());
        address.setNumber(request.number());
        address.setComplement(request.complement());
        address.setDistrict(request.district());
        address.setCity(request.city());
        address.setState(request.state().toUpperCase());
        address.setZipCode(formatZipCode(request.zipCode()));
        if (request.country() != null) {
            address.setCountry(request.country());
        }
    }

    private String formatZipCode(String zipCode) {
        String cleaned = zipCode.replaceAll("[^0-9]", "");
        if (cleaned.length() == 8) {
            return cleaned.substring(0, 5) + "-" + cleaned.substring(5);
        }
        return zipCode;
    }
}
