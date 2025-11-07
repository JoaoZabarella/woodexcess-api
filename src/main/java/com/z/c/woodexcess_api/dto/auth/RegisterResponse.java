package com.z.c.woodexcess_api.dto.auth;

public record RegisterResponse(
        String id,
        String name,
        String email,
        String role
) {}

