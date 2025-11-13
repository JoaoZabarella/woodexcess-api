package com.z.c.woodexcess_api.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TokenRotationResult {
    private String rawToken;
    private UUID userId;
    private String tokenHash;
}

