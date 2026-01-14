package com.z.c.woodexcess_api.dto.favorite;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Favorite statistics for a lisiting")
public record FavoriteStatsResponse(
        Long totalFavorites,
        Boolean isFavorited
) {
}
