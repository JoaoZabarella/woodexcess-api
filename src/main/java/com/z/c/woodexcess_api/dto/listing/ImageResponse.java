package com.z.c.woodexcess_api.dto.listing;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder

public record ImageResponse(
        UUID id,
        String imageUrl,
        String thumbnailUrl,
        Integer displayOrder,
        Long fileSize,
        String fileExtension,
        Boolean isPrimary,
        LocalDateTime uploadedAt
) {
}
