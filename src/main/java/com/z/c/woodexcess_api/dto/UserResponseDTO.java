package com.z.c.woodexcess_api.dto;

import com.z.c.woodexcess_api.role.UserRole;

import java.util.UUID;

public record UserResponseDTO(
        UUID id,
        String name,
        String email,
        UserRole role
) {
}
