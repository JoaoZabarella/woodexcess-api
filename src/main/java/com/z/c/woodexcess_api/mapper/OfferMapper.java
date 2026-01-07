package com.z.c.woodexcess_api.mapper;

import com.z.c.woodexcess_api.dto.offer.OfferResponse;
import com.z.c.woodexcess_api.dto.offer.OfferSummaryResponse;
import com.z.c.woodexcess_api.model.Offer;
import com.z.c.woodexcess_api.model.User;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.util.UUID;

@UtilityClass
public class OfferMapper {

    public OfferResponse toResponse(Offer offer, LocalDateTime now) {
        if (offer == null) {
            return null;
        }

        return OfferResponse.from(
                offer.getId(),
                offer.getListing().getId(),
                offer.getListing().getTitle(),
                offer.getListing().getPrice(),
                offer.getListing().getImages() != null && !offer.getListing().getImages().isEmpty()
                        ? String.valueOf(offer.getListing().getImages().get(0))
                        : null,
                offer.getBuyer().getId(),
                offer.getBuyer().getName(),
                offer.getBuyer().getAvatarUrl(),
                offer.getSeller().getId(),
                offer.getSeller().getName(),
                offer.getSeller().getAvatarUrl(),
                offer.getOfferedPrice(),
                offer.getQuantity(),
                offer.getMessage(),
                offer.getStatus(),
                offer.getExpiresAt(),
                offer.getParentOffer() != null ? offer.getParentOffer().getId() : null,
                offer.getCreatedAt(),
                offer.getUpdatedAt(),
                offer.isExpiredAt(now),
                offer.canBeAcceptedAt(now),
                offer.canBeCounteredAt(now)
        );
    }

    public OfferSummaryResponse toSummaryResponse(Offer offer, UUID currentUserId, LocalDateTime now) {
        if (offer == null) {
            return null;
        }

        boolean isBuyer = offer.getBuyer().getId().equals(currentUserId);
        User otherParty = isBuyer ? offer.getSeller() : offer.getBuyer();

        return OfferSummaryResponse.builder()
                .id(offer.getId())
                .listingId(offer.getListing().getId())
                .listingTitle(offer.getListing().getTitle())
                .listingImageUrl(
                        offer.getListing().getImages() != null && !offer.getListing().getImages().isEmpty()
                                ? String.valueOf(offer.getListing().getImages().get(0))
                                : null
                )
                .buyerId(offer.getBuyer().getId())
                .sellerId(offer.getSeller().getId())
                .otherPartyName(otherParty.getName())
                .otherPartyAvatarUrl(otherParty.getAvatarUrl())
                .offeredPrice(offer.getOfferedPrice())
                .quantity(offer.getQuantity())
                .status(offer.getStatus())
                .expiresAt(offer.getExpiresAt())
                .createdAt(offer.getCreatedAt())
                .isExpired(offer.isExpiredAt(now))
                .build();
    }
}
