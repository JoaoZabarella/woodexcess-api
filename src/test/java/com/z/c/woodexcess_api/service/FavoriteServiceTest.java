package com.z.c.woodexcess_api.service;

import com.z.c.woodexcess_api.dto.favorite.FavoriteResponse;
import com.z.c.woodexcess_api.dto.favorite.FavoriteStatsResponse;
import com.z.c.woodexcess_api.exception.BusinessException;
import com.z.c.woodexcess_api.exception.ResourceNotFoundException;
import com.z.c.woodexcess_api.mapper.FavoriteMapper;
import com.z.c.woodexcess_api.model.Favorite;
import com.z.c.woodexcess_api.model.MaterialListing;
import com.z.c.woodexcess_api.model.User;
import com.z.c.woodexcess_api.model.enums.Condition;
import com.z.c.woodexcess_api.model.enums.ListingStatus;
import com.z.c.woodexcess_api.model.enums.MaterialType;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FavoriteService Unit Tests")
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
    private FavoriteResponse favoriteResponse;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .email("user@test.com")
                .name("Test User")
                .build();

        owner = User.builder()
                .id(UUID.randomUUID())
                .email("owner@test.com")
                .name("Owner")
                .build();

        listing = MaterialListing.builder()
                .id(UUID.randomUUID())
                .title("Test Listing")
                .owner(owner)
                .status(ListingStatus.ACTIVE)
                .images(new ArrayList<>())
                .build();

        favorite = Favorite.builder()
                .id(UUID.randomUUID())
                .user(user)
                .listing(listing)
                .build();

        favoriteResponse = new FavoriteResponse(
                favorite.getId(),
                LocalDateTime.now(),
                listing.getId(),
                "Test Listing",
                "Description",
                MaterialType.WOOD,
                BigDecimal.valueOf(100),
                10,
                Condition.NEW,
                "São Paulo",
                "SP",
                null,
                "Owner",
                owner.getId(),
                1L,
                true
        );
    }

    @Test
    @DisplayName("Should add favorite successfully")
    void shouldAddFavoriteSuccessfully() {
        // Arrange
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));
        when(favoriteRepository.existsByUserAndListing(user, listing)).thenReturn(false);
        when(favoriteRepository.save(any(Favorite.class))).thenReturn(favorite);
        when(favoriteMapper.toResponse(favorite)).thenReturn(favoriteResponse);

        // Act
        FavoriteResponse response = favoriteService.addFavorite(user, listing.getId());

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.listingId()).isEqualTo(listing.getId());
        verify(favoriteRepository, times(1)).save(any(Favorite.class));
        verify(favoriteMapper, times(1)).toResponse(favorite);
    }

    @Test
    @DisplayName("Should throw exception when listing not found")
    void shouldThrowExceptionWhenListingNotFound() {
        // Arrange
        UUID listingId = UUID.randomUUID();
        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> favoriteService.addFavorite(user, listingId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Listing not found with id:");
    }

    @Test
    @DisplayName("Should throw exception when user tries to favorite own listing")
    void shouldThrowExceptionWhenSelfFavoriting() {
        // Arrange
        listing.setOwner(user);
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));

        // Act & Assert
        assertThatThrownBy(() -> favoriteService.addFavorite(user, listing.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("You cannot favorite your own listing");
    }

    @Test
    @DisplayName("Should throw exception when listing already favorited")
    void shouldThrowExceptionWhenAlreadyFavorited() {
        // Arrange
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));
        when(favoriteRepository.existsByUserAndListing(user, listing)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> favoriteService.addFavorite(user, listing.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Listing already favorited");
    }

    @Test
    @DisplayName("Should throw exception when listing is inactive")
    void shouldThrowExceptionWhenListingInactive() {
        // Arrange
        listing.setStatus(ListingStatus.INACTIVE);
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));
        when(favoriteRepository.existsByUserAndListing(user, listing)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> favoriteService.addFavorite(user, listing.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Cannot favorite an inactive listing");
    }

    @Test
    @DisplayName("Should remove favorite successfully")
    void shouldRemoveFavoriteSuccessfully() {
        // Arrange
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));
        when(favoriteRepository.findByUserAndListing(user, listing)).thenReturn(Optional.of(favorite));

        // Act
        favoriteService.removeFavorite(user, listing.getId());

        // Assert
        verify(favoriteRepository, times(1)).delete(favorite);
    }

    @Test
    @DisplayName("Should throw exception when removing non-existent favorite")
    void shouldThrowExceptionWhenRemovingNonExistentFavorite() {
        // Arrange
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));
        when(favoriteRepository.findByUserAndListing(user, listing)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> favoriteService.removeFavorite(user, listing.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Favorite not found");
    }

    @Test
    @DisplayName("Should get user favorites with pagination")
    void shouldGetUserFavoritesWithPagination() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        Page<Favorite> favoritesPage = new PageImpl<>(List.of(favorite));
        when(favoriteRepository.findByUserOrderByCreatedAtDesc(user, pageable)).thenReturn(favoritesPage);
        when(favoriteMapper.toResponse(any(Favorite.class))).thenReturn(favoriteResponse);

        // Act
        Page<FavoriteResponse> response = favoriteService.getUserFavorites(user, pageable);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).listingId()).isEqualTo(listing.getId());
        verify(favoriteMapper, times(1)).toResponse(any(Favorite.class));
    }

    @Test
    @DisplayName("Should check if listing is favorited")
    void shouldCheckIfListingIsFavorited() {
        // Arrange
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));
        when(favoriteRepository.existsByUserAndListing(user, listing)).thenReturn(true);

        // Act
        boolean isFavorited = favoriteService.isFavorited(user, listing.getId());

        // Assert
        assertThat(isFavorited).isTrue();
    }

    @Test
    @DisplayName("Should get listing stats")
    void shouldGetListingStats() {
        // Arrange
        FavoriteStatsResponse statsResponse = new FavoriteStatsResponse(10L, true);
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));
        when(favoriteRepository.countByListing(listing)).thenReturn(10L);
        when(favoriteRepository.existsByUserAndListing(user, listing)).thenReturn(true);
        when(favoriteMapper.toStatsResponse(10L, true)).thenReturn(statsResponse);

        // Act
        FavoriteStatsResponse stats = favoriteService.getListingStats(listing.getId(), user);

        // Assert
        assertThat(stats.totalFavorites()).isEqualTo(10L);
        assertThat(stats.isFavorited()).isTrue();
        verify(favoriteMapper, times(1)).toStatsResponse(10L, true);
    }

    @Test
    @DisplayName("Should get user favorites count")
    void shouldGetUserFavoritesCount() {
        // Arrange
        when(favoriteRepository.countByUser(user)).thenReturn(15L);

        // Act
        long count = favoriteService.getUserFavoriteCount(user);

        // Assert
        assertThat(count).isEqualTo(15L);
    }
}
