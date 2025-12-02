package com.z.c.woodexcess_api.mapper;

import com.z.c.woodexcess_api.dto.address.AddressFromCepRequest;
import com.z.c.woodexcess_api.dto.address.AddressRequest;
import com.z.c.woodexcess_api.dto.address.AddressResponse;
import com.z.c.woodexcess_api.dto.address.ViaCepResponse;
import com.z.c.woodexcess_api.model.Address;
import com.z.c.woodexcess_api.model.User;
import org.springframework.stereotype.Component;

@Component
public class AddressMapper {

    public Address toEntity(AddressRequest dto, User user) {
        return Address.builder()
                .user(user)
                .street(dto.street())
                .number(dto.number())
                .complement(dto.complement())
                .district(dto.district())
                .city(dto.city())
                .state(dto.state().toUpperCase())
                .zipCode(formatZipCode(dto.zipCode()))
                .country(dto.country() != null ? dto.country() : "Brasil")
                .isPrimary(dto.isPrimary() != null ? dto.isPrimary() : false)
                .active(true)
                .build();
    }

    public Address toEntityFromCep(ViaCepResponse viaCep, AddressFromCepRequest dto, User user) {
        return Address.builder()
                .user(user)
                .street(viaCep.street())
                .number(dto.number())
                .complement(dto.complement())
                .district(viaCep.district())
                .city(viaCep.city())
                .state(viaCep.state().toUpperCase())
                .zipCode(viaCep.cep())
                .country("Brasil")
                .isPrimary(dto.isPrimary() != null ? dto.isPrimary() : false)
                .active(true)
                .build();
    }

    public AddressResponse toResponseDTO(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .userId(address.getUser().getId())
                .street(address.getStreet())
                .number(address.getNumber())
                .complement(address.getComplement())
                .district(address.getDistrict())
                .city(address.getCity())
                .state(address.getState())
                .zipCode(address.getFormatedZipCode())
                .country(address.getCountry())
                .active(address.getActive())
                .isPrimary(address.getIsPrimary())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .fullAddress(address.getFullAddress())
                .build();
    }

    public void updateEntity(Address address, AddressRequest dto) {
        address.setStreet(dto.street());
        address.setNumber(dto.number());
        address.setComplement(dto.complement());
        address.setDistrict(dto.district());
        address.setCity(dto.city());
        address.setState(dto.state().toUpperCase());
        address.setZipCode(formatZipCode(dto.zipCode()));

        if (dto.country() != null) {
            address.setCountry(dto.country());
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
