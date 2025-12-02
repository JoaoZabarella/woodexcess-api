package com.z.c.woodexcess_api.dto.user;

import com.z.c.woodexcess_api.dto.address.AddressResponse;
import com.z.c.woodexcess_api.mapper.AddressMapper;
import com.z.c.woodexcess_api.model.User;
import com.z.c.woodexcess_api.enums.UserRole;
import lombok.Builder;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Builder
public record UserResponse(
        UUID id,
        String name,
        String email,
        String phone,
        Boolean active,
        UserRole role,
        List<AddressResponse> addresses) {
    public UserResponse(User user, AddressMapper addressMapper) {
        this(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getActive(),
                user.getRole(),
                user.getAddresses() != null
                        ? user.getAddresses().stream()
                                .map(addressMapper::toResponseDTO)
                                .collect(Collectors.toList())
                        : List.of());
    }
}
