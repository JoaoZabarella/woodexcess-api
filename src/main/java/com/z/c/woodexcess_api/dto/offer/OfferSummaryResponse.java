package com.z.c.woodexcess_api.dto.offer;

import com.z.c.woodexcess_api.model.enums.OfferStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record OfferSummaryResponse(
        UUID id,
        UUID listingId,
        String listingTitle,
        String listingImageUrl,
        UUID buyerId,
        UUID sellerId,
        String otherPartyName,
        String otherPartyAvatarUrl,
        BigDecimal offeredPrice,
        Integer quantity,
        OfferStatus status,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        Boolean isExpired
) {}
