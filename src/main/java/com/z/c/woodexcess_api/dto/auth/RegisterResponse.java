package com.z.c.woodexcess_api.dto.auth;

import com.z.c.woodexcess_api.enums.UserRole;
import java.util.UUID;

public record RegisterResponse(
        UUID id,
        String name,
        String email,
        String phone,
        Boolean active,
        UserRole role) {
}
