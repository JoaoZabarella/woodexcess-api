package com.z.c.woodexcess_api.dto.auth;

public record LoginResponse(
        String token

) {
    public LoginResponse(String token) {
        this.token = token; }
}
