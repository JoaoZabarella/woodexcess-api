package com.z.c.woodexcess_api.service.favorite;

import com.z.c.woodexcess_api.dto.favorite.FavoriteResponse;
import com.z.c.woodexcess_api.dto.favorite.FavoriteStatsResponse;
import com.z.c.woodexcess_api.exception.ResourceNotFoundException;
import com.z.c.woodexcess_api.mapper.FavoriteMapper;
import com.z.c.woodexcess_api.model.Favorite;
import com.z.c.woodexcess_api.model.MaterialListing;
import com.z.c.woodexcess_api.model.User;
import com.z.c.woodexcess_api.model.enums.ListingStatus;
import com.z.c.woodexcess_api.repository.FavoriteRepository;
import com.z.c.woodexcess_api.repository.MaterialListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final MaterialListingRepository listingRepository;
    private final FavoriteMapper favoriteMapper;

    @Transactional
    public FavoriteResponse addFavorite(User user, UUID listingId) {
        MaterialListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));

        if (listing.getOwner().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You cannot favorite your own listing");
        }

        if (favoriteRepository.existsByUserAndListing(user, listing)) {
            throw new IllegalArgumentException("Listing already favorited");
        }

        Favorite favorite = Favorite.builder()
                .user(user)
                .listing(listing)
                .build();

        Favorite savedFavorite = favoriteRepository.save(favorite);
        long totalFavorites = favoriteRepository.countByListing(listing);

        return favoriteMapper.toResponse(savedFavorite, totalFavorites);
    }

    @Transactional
    public void removeFavorite(User user, UUID listingId) {
        MaterialListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));

        Favorite favorite = favoriteRepository.findByUserAndListing(user, listing)
                .orElseThrow(() -> new ResourceNotFoundException("Favorite not found"));

        favoriteRepository.delete(favorite);
    }

    @Transactional(readOnly = true)
    public Page<FavoriteResponse> getUserFavorites(User user, Pageable pageable) {
        Page<Favorite> favorites = favoriteRepository.findByUserWithDetails(
                user,
                ListingStatus.ACTIVE,
                pageable
        );

        // ✅ Buscar counts em bulk (1 query)
        Set<UUID> listingIds = favorites.getContent().stream()
                .map(f -> f.getListing().getId())
                .collect(Collectors.toSet());

        Map<UUID, Long> favoriteCounts = favoriteRepository.countByListingIds(listingIds);

        // ✅ Mapear com counts
        return favorites.map(favorite ->
                favoriteMapper.toResponse(
                        favorite,
                        favoriteCounts.getOrDefault(favorite.getListing().getId(), 0L)
                )
        );
    }

    @Transactional(readOnly = true)
    public boolean isFavorited(User user, UUID listingId) {
        MaterialListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));

        return favoriteRepository.existsByUserAndListing(user, listing);
    }

    @Transactional(readOnly = true)
    public FavoriteStatsResponse getListingStats(User user, UUID listingId) {
        MaterialListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));

        long totalFavorites = favoriteRepository.countByListing(listing);
        boolean isFavorited = favoriteRepository.existsByUserAndListing(user, listing);

        return favoriteMapper.toStatsResponse(totalFavorites, isFavorited);
    }

    @Transactional(readOnly = true)
    public long getUserFavoritesCount(User user) {
        return favoriteRepository.countByUser(user);
    }
}
