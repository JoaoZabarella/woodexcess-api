package com.z.c.woodexcess_api.mapper;

import com.z.c.woodexcess_api.dto.favorite.FavoriteResponse;
import com.z.c.woodexcess_api.dto.favorite.FavoriteStatsResponse;
import com.z.c.woodexcess_api.model.Favorite;
import com.z.c.woodexcess_api.model.ListingImage;
import com.z.c.woodexcess_api.model.MaterialListing;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FavoriteMapper {

    @Mapping(target = "favoriteId", source = "favorite.id")
    @Mapping(target = "favoritedAt", source = "favorite.createdAt")
    @Mapping(target = "listingId", source = "favorite.listing.id")
    @Mapping(target = "title", source = "favorite.listing.title")
    @Mapping(target = "description", source = "favorite.listing.description")
    @Mapping(target = "materialType", source = "favorite.listing.materialType")
    @Mapping(target = "price", source = "favorite.listing.price")
    @Mapping(target = "quantity", source = "favorite.listing.quantity")
    @Mapping(target = "condition", source = "favorite.listing.condition")
    @Mapping(target = "city", source = "favorite.listing.city")
    @Mapping(target = "state", source = "favorite.listing.state")
    @Mapping(target = "primaryImageUrl", expression = "java(getPrimaryImageUrl(favorite.getListing()))")
    @Mapping(target = "ownerName", source = "favorite.listing.owner.name")
    @Mapping(target = "ownerId", source = "favorite.listing.owner.id")
    @Mapping(target = "totalFavorites", source = "totalFavorites")
    @Mapping(target = "isActive", expression = "java(favorite.getListing().getStatus() == com.z.c.woodexcess_api.model.enums.ListingStatus.ACTIVE)")
    FavoriteResponse toResponse(Favorite favorite, Long totalFavorites);

    default String getPrimaryImageUrl(MaterialListing listing) {
        return listing.getImages().stream()
                .filter(ListingImage::getIsPrimary)
                .findFirst()
                .map(ListingImage::getImageUrl)
                .orElse(null);
    }

    default FavoriteStatsResponse toStatsResponse(long totalFavorites, boolean isFavorited) {
        return new FavoriteStatsResponse(totalFavorites, isFavorited);
    }
}
