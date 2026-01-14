package com.z.c.woodexcess_api.service.favorite;

import com.z.c.woodexcess_api.dto.favorite.FavoriteResponse;
import com.z.c.woodexcess_api.dto.favorite.FavoriteStatsResponse;
import com.z.c.woodexcess_api.exception.BusinessException;
import com.z.c.woodexcess_api.exception.ResourceNotFoundException;
import com.z.c.woodexcess_api.mapper.FavoriteMapper;
import com.z.c.woodexcess_api.model.Favorite;
import com.z.c.woodexcess_api.model.MaterialListing;
import com.z.c.woodexcess_api.model.User;
import com.z.c.woodexcess_api.model.enums.ListingStatus;
import com.z.c.woodexcess_api.repository.FavoriteRepository;
import com.z.c.woodexcess_api.repository.MaterialListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final MaterialListingRepository listingRepository;
    private final FavoriteMapper mapper;


    @Transactional
    public FavoriteResponse addFavorite(User user, UUID listingId) {
        log.info("Adding favorite - User: {}, Listing: {}", user.getEmail(), listingId);

        MaterialListing listing = findListingById(listingId);

        validateFavoriteCreation(user, listing);

        Favorite favorite = Favorite.builder()
                .user(user)
                .listing(listing)
                .build();

        Favorite saved = favoriteRepository.save(favorite);
        log.info("Favorite added successfully - ID: {}", saved.getId());

        return mapper.toResponse(saved);
    }


    @Transactional
    public void removeFavorite(User user, UUID listingId) {
        log.info("Removing favorite - User: {}, Listing: {}", user.getEmail(), listingId);

        MaterialListing listing = findListingById(listingId);

        Favorite favorite = favoriteRepository.findByUserAndListing(user, listing)
                .orElseThrow(() -> new ResourceNotFoundException("Favorite not found"));

        favoriteRepository.delete(favorite);
        log.info("Favorite removed successfully - User: {}, Listing: {}", user.getEmail(), listingId);
    }


    @Transactional(readOnly = true)
    public Page<FavoriteResponse> getUserFavorites(User user, Pageable pageable) {
        log.info("Fetching favorites for user: {} - Page: {}, Size: {}",
                user.getEmail(), pageable.getPageNumber(), pageable.getPageSize());

        Page<Favorite> favorites = favoriteRepository.findByUserOrderByCreatedAtDesc(user, pageable);

        log.debug("Found {} favorites for user: {}", favorites.getTotalElements(), user.getEmail());

        return favorites.map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public boolean isFavorited(User user, UUID listingId) {
        MaterialListing listing = findListingById(listingId);
        return favoriteRepository.existsByUserAndListing(user, listing);
    }


    @Transactional(readOnly = true)
    public FavoriteStatsResponse getListingStats(UUID listingId, User user) {
        MaterialListing listing = findListingById(listingId);

        long totalFavorites = favoriteRepository.countByListing(listing);
        boolean isFavorited = user != null &&
                favoriteRepository.existsByUserAndListing(user, listing);

        log.debug("Listing {} stats - Total: {}, IsFavorited: {}",
                listingId, totalFavorites, isFavorited);

        return mapper.toStatsResponse(totalFavorites, isFavorited);
    }

    @Transactional(readOnly = true)
    public long getUserFavoriteCount(User user) {
        long count = favoriteRepository.countByUser(user);
        log.debug("User {} has {} favorites", user.getEmail(), count);
        return count;
    }

    private MaterialListing findListingById(UUID listingId) {
        return listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + listingId));
    }


    private void validateFavoriteCreation(User user, MaterialListing listing) {
        if (listing.getOwner().getId().equals(user.getId())) {
            log.warn("User {} attempted to favorite own listing {}", user.getEmail(), listing.getId());
            throw new BusinessException("You cannot favorite your own listing");
        }

        if (favoriteRepository.existsByUserAndListing(user, listing)) {
            log.warn("User {} attempted to favorite listing {} again", user.getEmail(), listing.getId());
            throw new BusinessException("Listing already favorited");
        }

        if (listing.getStatus() != ListingStatus.ACTIVE) {
            log.warn("User {} attempted to favorite inactive listing {}", user.getEmail(), listing.getId());
            throw new BusinessException("Cannot favorite an inactive listing");
        }
    }
}
