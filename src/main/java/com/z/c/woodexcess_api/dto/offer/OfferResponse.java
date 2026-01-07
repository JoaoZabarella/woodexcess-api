package com.z.c.woodexcess_api.dto.offer;

import com.z.c.woodexcess_api.model.enums.OfferStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record OfferResponse(
        UUID id,


        UUID listingId,
        String listingTitle,
        BigDecimal listingPrice,
        String listingImageUrl,


        UUID buyerId,
        String buyerName,
        String buyerAvatarUrl,


        UUID sellerId,
        String sellerName,
        String sellerAvatarUrl,


        BigDecimal offeredPrice,
        Integer quantity,
        String message,
        OfferStatus status,


        LocalDateTime expiresAt,
        UUID parentOfferId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,


        BigDecimal totalPrice,
        BigDecimal discountAmount,
        Double discountPercentage,
        Boolean isExpired,
        Boolean canBeAccepted,
        Boolean canBeCountered
) {
    public static OfferResponse from(
            UUID id,
            UUID listingId,
            String listingTitle,
            BigDecimal listingPrice,
            String listingImageUrl,
            UUID buyerId,
            String buyerName,
            String buyerAvatarUrl,
            UUID sellerId,
            String sellerName,
            String sellerAvatarUrl,
            BigDecimal offeredPrice,
            Integer quantity,
            String message,
            OfferStatus status,
            LocalDateTime expiresAt,
            UUID parentOfferId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Boolean isExpired,
            Boolean canBeAccepted,
            Boolean canBeCountered
    ) {
        BigDecimal totalPrice = offeredPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal listingTotal = listingPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal discountAmount = listingTotal.subtract(totalPrice);
        Double discountPercentage = discountAmount
                .divide(listingTotal, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();

        return new OfferResponse(
                id, listingId, listingTitle, listingPrice, listingImageUrl,
                buyerId, buyerName, buyerAvatarUrl,
                sellerId, sellerName, sellerAvatarUrl,
                offeredPrice, quantity, message, status,
                expiresAt, parentOfferId, createdAt, updatedAt,
                totalPrice, discountAmount, discountPercentage,
                isExpired, canBeAccepted, canBeCountered
        );
    }
}
