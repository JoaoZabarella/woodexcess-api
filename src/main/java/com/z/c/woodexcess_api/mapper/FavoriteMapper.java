package com.z.c.woodexcess_api.mapper;

import com.z.c.woodexcess_api.dto.favorite.FavoriteResponse;
import com.z.c.woodexcess_api.dto.favorite.FavoriteStatsResponse;
import com.z.c.woodexcess_api.model.Favorite;
import com.z.c.woodexcess_api.model.ListingImage;
import com.z.c.woodexcess_api.model.MaterialListing;
import com.z.c.woodexcess_api.model.enums.ListingStatus;
import org.springframework.stereotype.Component;

@Component
public class FavoriteMapper {

    public FavoriteResponse toResponse(Favorite favorite, long totalFavorites) {
        MaterialListing listing = favorite.getListing();

        String primaryImageUrl = listing.getImages().stream()
                .filter(ListingImage::getIsPrimary)
                .findFirst()
                .map(ListingImage::getImageUrl)
                .orElse(null);

        return new FavoriteResponse(
                favorite.getId(),
                favorite.getCreatedAt(),
                listing.getId(),
                listing.getTitle(),
                listing.getDescription(),
                listing.getMaterialType(),
                listing.getPrice(),
                listing.getQuantity(),
                listing.getCondition(),
                listing.getCity(),
                listing.getState(),
                primaryImageUrl,
                listing.getOwner().getName(),
                listing.getOwner().getId(),
                totalFavorites,
                listing.getStatus() == ListingStatus.ACTIVE
        );
    }

    public FavoriteStatsResponse toStatsResponse(long totalFavorites, boolean isFavorited) {
        return new FavoriteStatsResponse(totalFavorites, isFavorited);
    }

}
