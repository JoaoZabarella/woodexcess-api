package com.z.c.woodexcess_api.dto.auth;

import com.z.c.woodexcess_api.role.UserRole;

public record LoginResponse(
        String token,
        String name,
        String email,
        String phone,
        Boolean active,
        UserRole role

){}
