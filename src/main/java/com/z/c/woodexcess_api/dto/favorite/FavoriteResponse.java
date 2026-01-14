package com.z.c.woodexcess_api.dto.favorite;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.z.c.woodexcess_api.model.enums.Condition;
import com.z.c.woodexcess_api.model.enums.MaterialType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Favorite with complete listing details")
public record FavoriteResponse(
        UUID favoriteId,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime favoritedAt,

        UUID listingId,
        String title,
        String description,
        MaterialType materialType,
        BigDecimal price,
        Integer quantity,
        Condition condition,
        String city,
        String state,
        String primaryImageUrl,
        String ownerName,
        UUID ownerId,
        Long totalFavorites,
        Boolean isActive
) {}
