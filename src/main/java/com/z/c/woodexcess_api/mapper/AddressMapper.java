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
                .street(dto.getStreet())
                .number(dto.getNumber())
                .complement(dto.getComplement())
                .district(dto.getDistrict())
                .city(dto.getCity())
                .state(dto.getState().toUpperCase())
                .zipCode(dto.getZipCode())
                .country(dto.getCountry() != null ? dto.getCountry() : "Brasil")
                .isPrimary(dto.getIsPrimary() != null ? dto.getIsPrimary() : false)
                .active(true)
                .build();
    }

    /**
     * Converte ViaCepResponse + complementos para Address entity
     */
    public Address toEntityFromCep(ViaCepResponse viaCep, AddressFromCepRequest dto, User user) {
        return Address.builder()
                .user(user)
                .street(viaCep.getStreet())
                .number(dto.getNumber())
                .complement(dto.getComplement())
                .district(viaCep.getDistrict())
                .city(viaCep.getCity())
                .state(viaCep.getState().toUpperCase())
                .zipCode(viaCep.getCep())
                .country("Brasil")
                .isPrimary(dto.getIsPrimary() != null ? dto.getIsPrimary() : false)
                .active(true)
                .build();
    }

    /**
     * Converte Address entity para AddressResponse DTO
     * ⭐ NÃO É STATIC - método de instância
     */
    public AddressResponse toResponseDTO(Address address) {  // ⭐ REMOVE 'static'
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

    /**
     * Atualiza uma entidade Address existente com dados do DTO
     */
    public void updateEntity(Address address, AddressRequest dto) {
        address.setStreet(dto.getStreet());
        address.setNumber(dto.getNumber());
        address.setComplement(dto.getComplement());
        address.setDistrict(dto.getDistrict());
        address.setCity(dto.getCity());
        address.setState(dto.getState().toUpperCase());
        address.setZipCode(dto.getZipCode());

        if (dto.getCountry() != null) {
            address.setCountry(dto.getCountry());
        }
    }
}
