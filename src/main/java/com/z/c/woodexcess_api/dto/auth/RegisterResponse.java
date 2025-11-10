package com.z.c.woodexcess_api.dto.auth;

import com.z.c.woodexcess_api.role.UserRole;

public record RegisterResponse(
        String id,
        String name,
        String email,
        UserRole role
) {
}

