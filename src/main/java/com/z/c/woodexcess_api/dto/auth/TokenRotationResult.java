package com.z.c.woodexcess_api.dto.auth;

import lombok.Builder;

import java.util.UUID;

@Builder
public record TokenRotationResult(
        String rawToken,
        UUID userId,
        String tokenHash) {
}
