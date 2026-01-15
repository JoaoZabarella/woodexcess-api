package com.z.c.woodexcess_api.service;

import com.z.c.woodexcess_api.dto.favorite.FavoriteResponse;
import com.z.c.woodexcess_api.dto.favorite.FavoriteStatsResponse;
import com.z.c.woodexcess_api.mapper.FavoriteMapper;
import com.z.c.woodexcess_api.model.Favorite;
import com.z.c.woodexcess_api.model.MaterialListing;
import com.z.c.woodexcess_api.model.User;
import com.z.c.woodexcess_api.model.enums.ListingStatus;
import com.z.c.woodexcess_api.repository.FavoriteRepository;
import com.z.c.woodexcess_api.repository.MaterialListingRepository;
import com.z.c.woodexcess_api.service.favorite.FavoriteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Favorite Service Unit Tests")
class FavoriteServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private MaterialListingRepository listingRepository;

    @Mock
    private FavoriteMapper favoriteMapper;

    @InjectMocks
    private FavoriteService favoriteService;

    private User user;
    private User owner;
    private MaterialListing listing;
    private Favorite favorite;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("user@test.com")
                .build();

        owner = User.builder()
                .id(UUID.randomUUID())
                .name("Owner")
                .email("owner@test.com")
                .build();

        listing = MaterialListing.builder()
                .id(UUID.randomUUID())
                .title("Test Listing")
                .owner(owner)
                .build();

        favorite = Favorite.builder()
                .id(UUID.randomUUID())
                .user(user)
                .listing(listing)
                .build();
    }

    @Test
    @DisplayName("Should add favorite successfully")
    void shouldAddFavoriteSuccessfully() {
        // Given
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));
        when(favoriteRepository.existsByUserAndListing(user, listing)).thenReturn(false);
        when(favoriteRepository.save(any(Favorite.class))).thenReturn(favorite);
        when(favoriteRepository.countByListing(listing)).thenReturn(5L);

        FavoriteResponse expectedResponse = new FavoriteResponse(
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
                null,
                owner.getName(),
                owner.getId(),
                5L,
                true
        );
        when(favoriteMapper.toResponse(any(Favorite.class), eq(5L)))
                .thenReturn(expectedResponse);

        // When
        FavoriteResponse response = favoriteService.addFavorite(user, listing.getId());

        // Then
        assertThat(response).isNotNull();
        assertThat(response.totalFavorites()).isEqualTo(5L);

        verify(favoriteRepository).save(any(Favorite.class));
        verify(favoriteMapper).toResponse(any(Favorite.class), eq(5L));
    }

    @Test
    @DisplayName("Should get user favorites with bulk count")
    void shouldGetUserFavoritesWithBulkCount() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        List<Favorite> favoritesList = Arrays.asList(favorite);
        Page<Favorite> favoritesPage = new PageImpl<>(favoritesList);

        when(favoriteRepository.findByUserWithDetails(user, ListingStatus.ACTIVE, pageable))
                .thenReturn(favoritesPage);

        Map<UUID, Long> countsMap = Map.of(listing.getId(), 10L);
        when(favoriteRepository.countByListingIds(any(Set.class)))
                .thenReturn(countsMap);

        FavoriteResponse expectedResponse = new FavoriteResponse(
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
                null,
                owner.getName(),
                owner.getId(),
                10L,
                true
        );

        when(favoriteMapper.toResponse(favorite, 10L))
                .thenReturn(expectedResponse);

        // When
        Page<FavoriteResponse> result = favoriteService.getUserFavorites(user, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).totalFavorites()).isEqualTo(10L);

        verify(favoriteRepository, times(1)).countByListingIds(any(Set.class));
        verify(favoriteRepository, never()).countByListing(any(MaterialListing.class));
    }

    @Test
    @DisplayName("Should get listing stats")
    void shouldGetListingStats() {
        // Given
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));
        when(favoriteRepository.countByListing(listing)).thenReturn(15L);
        when(favoriteRepository.existsByUserAndListing(user, listing)).thenReturn(true);

        FavoriteStatsResponse expectedResponse = new FavoriteStatsResponse(15L, true);
        when(favoriteMapper.toStatsResponse(15L, true)).thenReturn(expectedResponse);

        // When
        FavoriteStatsResponse response = favoriteService.getListingStats(user, listing.getId());

        // Then
        assertThat(response).isNotNull();
        assertThat(response.totalFavorites()).isEqualTo(15L);
        assertThat(response.isFavorited()).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when favoriting own listing")
    void shouldThrowExceptionWhenFavoritingOwnListing() {
        // Given
        MaterialListing ownListing = MaterialListing.builder()
                .id(UUID.randomUUID())
                .title("Own Listing")
                .owner(user)
                .build();

        when(listingRepository.findById(ownListing.getId()))
                .thenReturn(Optional.of(ownListing));

        // When/Then
        assertThatThrownBy(() -> favoriteService.addFavorite(user, ownListing.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("You cannot favorite your own listing");

        verify(favoriteRepository, never()).save(any(Favorite.class));
    }

    @Test
    @DisplayName("Should throw exception when listing already favorited")
    void shouldThrowExceptionWhenListingAlreadyFavorited() {
        // Given
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));
        when(favoriteRepository.existsByUserAndListing(user, listing)).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> favoriteService.addFavorite(user, listing.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Listing already favorited");

        verify(favoriteRepository, never()).save(any(Favorite.class));
    }
}
