package com.z.c.woodexcess_api.dto.auth;

import com.z.c.woodexcess_api.enums.UserRole;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn

) {
    public LoginResponse(String accessToken, String refreshToken, Long expiresIn) {
        this(accessToken, refreshToken, "Bearer", expiresIn);
    }
}
