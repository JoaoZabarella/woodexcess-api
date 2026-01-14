package com.z.c.woodexcess_api.controller;

import com.z.c.woodexcess_api.dto.favorite.FavoriteResponse;
import com.z.c.woodexcess_api.dto.favorite.FavoriteStatsResponse;
import com.z.c.woodexcess_api.model.User;
import com.z.c.woodexcess_api.security.CustomUserDetails;
import com.z.c.woodexcess_api.service.favorite.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
@Tag(name = "Favorites", description = "Endpoints for managing user favorites/watchlist")
@SecurityRequirement(name = "bearer-jwt")
public class FavoriteController {

    private final FavoriteService service;

    @PostMapping("/{listingId}")
    @Operation(
            summary = "Add listing to favorites",
            description = "Add a material listing to user's favorites/watchlist"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Listing added to favorites successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request (already favorited, self-favorite, inactive listing)"),
            @ApiResponse(responseCode = "404", description = "Listing not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<FavoriteResponse> addFavorite(
            @Parameter(description = "Listing ID to favorite")
            @PathVariable UUID listingId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        User user = userDetails.user();
        FavoriteResponse response = service.addFavorite(user, listingId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{listingId}")
    @Operation(
            summary = "Remove listing from favorites",
            description = "Remove a material listing from user's favorites"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Listing removed from favorites successfully"),
            @ApiResponse(responseCode = "404", description = "Listing or favorite not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Void> removeFavorite(
            @Parameter(description = "Listing ID to unfavorite")
            @PathVariable UUID listingId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        service.removeFavorite(userDetails.user(), listingId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(
            summary = "Get user favorites",
            description = "Get all favorites for the authenticated user with pagination"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Favorites retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Page<FavoriteResponse>> getUserFavorites(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            @Parameter(description = "Pagination parameters (page, size, sort)")
            Pageable pageable
    ) {
        Page<FavoriteResponse> favorites = service.getUserFavorites(userDetails.user(), pageable);
        return ResponseEntity.ok(favorites);
    }

    @GetMapping("/check/{listingId}")
    @Operation(
            summary = "Check if listing is favorited",
            description = "Check if user has favorited a specific listing"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Check completed successfully"),
            @ApiResponse(responseCode = "404", description = "Listing not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Boolean> isFavorited(
            @Parameter(description = "Listing ID to check")
            @PathVariable UUID listingId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        boolean favorited = service.isFavorited(userDetails.user(), listingId);
        return ResponseEntity.ok(favorited);
    }

    @GetMapping("/stats/{listingId}")
    @Operation(
            summary = "Get listing favorite stats",
            description = "Get total favorites count and user favorite status for a listing"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stats retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Listing not found")
    })
    public ResponseEntity<FavoriteStatsResponse> getListingStats(
            @Parameter(description = "Listing ID")
            @PathVariable UUID listingId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        FavoriteStatsResponse stats = service.getListingStats(listingId, userDetails.user());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/count")
    @Operation(
            summary = "Get user favorites count",
            description = "Get total number of favorites for authenticated user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Count retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Long> getUserFavoritesCount(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        long count = service.getUserFavoriteCount(userDetails.user());
        return ResponseEntity.ok(count);
    }
}
