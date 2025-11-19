package com.z.c.woodexcess_api.dto.user;

import com.z.c.woodexcess_api.dto.address.AddressResponse;
import com.z.c.woodexcess_api.model.User;
import com.z.c.woodexcess_api.role.UserRole;

import java.util.UUID;

import java.util.List;

public record UserResponse (
        UUID id,
        String name,
        String email,
        String phone,
        Boolean active,
        UserRole role,
        List<AddressResponse> addresses
) {
    public UserResponse(User user){
        this(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getActive(),
                user.getRole(),
                user.getAddresses().stream()
                        .map(AddressResponse::new)
                        .toList()
        );
    }
}

