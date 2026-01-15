package com.z.c.woodexcess_api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.z.c.woodexcess_api.model.Address;
import com.z.c.woodexcess_api.model.Favorite;
import com.z.c.woodexcess_api.model.MaterialListing;
import com.z.c.woodexcess_api.model.User;
import com.z.c.woodexcess_api.model.enums.Condition;
import com.z.c.woodexcess_api.model.enums.ListingStatus;
import com.z.c.woodexcess_api.model.enums.MaterialType;
import com.z.c.woodexcess_api.model.enums.UserRole;
import com.z.c.woodexcess_api.repository.AddressRepository;
import com.z.c.woodexcess_api.repository.FavoriteRepository;
import com.z.c.woodexcess_api.repository.MaterialListingRepository;
import com.z.c.woodexcess_api.repository.UserRepository;
import com.z.c.woodexcess_api.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Favorite Controller Integration Tests")
class FavoriteControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MaterialListingRepository listingRepository;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private JwtProvider jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User buyer;
    private User seller;
    private MaterialListing listing;
    private String buyerToken;

    @BeforeEach
    void setUp() {

        favoriteRepository.deleteAll();
        listingRepository.deleteAll();
        userRepository.deleteAll();

        String uniqueId = UUID.randomUUID().toString().substring(0, 8);

        // Create buyer
        buyer = User.builder()
                .name("Buyer User")
                .email("buyer-" + uniqueId + "@test.com")
                .password(passwordEncoder.encode("Password123!"))
                .phone("11987654321")
                .role(UserRole.USER)
                .isActive(true)
                .build();
        buyer = userRepository.save(buyer);
        buyerToken = jwtService.generateJwtToken(buyer);

        // Create seller
        seller = User.builder()
                .name("Seller User")
                .email("seller-" + uniqueId + "@test.com")
                .password(passwordEncoder.encode("Password123!"))
                .phone("11987654322")
                .role(UserRole.USER)
                .isActive(true)
                .build();
        seller = userRepository.save(seller);

        // Create address
        Address address = Address.builder()
                .user(seller)
                .street("Test Street")
                .number("123")
                .district("Test District")
                .city("São Paulo")
                .state("SP")
                .zipCode("01310-100")
                .isPrimary(true)
                .build();
        address = addressRepository.save(address);

        // Create listing
        listing = MaterialListing.builder()
                .title("Madeira de Ipê")
                .description("Sobras de madeira")
                .materialType(MaterialType.WOOD)
                .price(BigDecimal.valueOf(150.00))
                .quantity(10)
                .condition(Condition.USED)
                .owner(seller)
                .address(address)
                .city("São Paulo")
                .state("SP")
                .status(ListingStatus.ACTIVE)
                .build();
        listing = listingRepository.save(listing);
    }

    @Test
    @DisplayName("Should add favorite successfully")
    void shouldAddFavoriteSuccessfully() throws Exception {
        mockMvc.perform(post("/api/favorites/{listingId}", listing.getId())
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.favoriteId").exists())
                .andExpect(jsonPath("$.listingId").value(listing.getId().toString()))
                .andExpect(jsonPath("$.title").value("Madeira de Ipê"));
    }

    @Test
    @DisplayName("Should return 400 when favoriting own listing")
    void shouldReturn400WhenFavoritingOwnListing() throws Exception {
        String sellerToken = jwtService.generateJwtToken(seller);

        mockMvc.perform(post("/api/favorites/{listingId}", listing.getId())
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You cannot favorite your own listing"));
    }

    @Test
    @DisplayName("Should return 400 when listing already favorited")
    void shouldReturn400WhenListingAlreadyFavorited() throws Exception {
        // Add favorite first
        Favorite favorite = Favorite.builder()
                .user(buyer)
                .listing(listing)
                .build();
        favoriteRepository.save(favorite);

        // Try to add again
        mockMvc.perform(post("/api/favorites/{listingId}", listing.getId())
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Listing already favorited"));
    }

    @Test
    @DisplayName("Should remove favorite successfully")
    void shouldRemoveFavoriteSuccessfully() throws Exception {
        // Add favorite first
        Favorite favorite = Favorite.builder()
                .user(buyer)
                .listing(listing)
                .build();
        favoriteRepository.save(favorite);

        // Remove it
        mockMvc.perform(delete("/api/favorites/{listingId}", listing.getId())
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should get user favorites")
    void shouldGetUserFavorites() throws Exception {
        // Add favorite
        Favorite favorite = Favorite.builder()
                .user(buyer)
                .listing(listing)
                .build();
        favoriteRepository.save(favorite);

        mockMvc.perform(get("/api/favorites")
                        .header("Authorization", "Bearer " + buyerToken)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].listingId").value(listing.getId().toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("Should check if listing is favorited")
    void shouldCheckIfListingIsFavorited() throws Exception {
        // Add favorite
        Favorite favorite = Favorite.builder()
                .user(buyer)
                .listing(listing)
                .build();
        favoriteRepository.save(favorite);

        mockMvc.perform(get("/api/favorites/check/{listingId}", listing.getId())
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @DisplayName("Should get listing favorite stats")
    void shouldGetListingFavoriteStats() throws Exception {
        // Add favorite
        Favorite favorite = Favorite.builder()
                .user(buyer)
                .listing(listing)
                .build();
        favoriteRepository.save(favorite);

        mockMvc.perform(get("/api/favorites/stats/{listingId}", listing.getId())
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFavorites").value(1))
                .andExpect(jsonPath("$.isFavorited").value(true));
    }

    @Test
    @DisplayName("Should get user favorites count")
    void shouldGetUserFavoritesCount() throws Exception {
        // Add 3 favorites
        for (int i = 0; i < 3; i++) {
            MaterialListing newListing = MaterialListing.builder()
                    .title("Listing " + i)
                    .description("Description")
                    .materialType(MaterialType.WOOD)
                    .price(BigDecimal.valueOf(100.00))
                    .quantity(10)
                    .condition(Condition.NEW)
                    .owner(seller)
                    .address(listing.getAddress())
                    .city("São Paulo")
                    .state("SP")
                    .status(ListingStatus.ACTIVE)
                    .build();
            newListing = listingRepository.save(newListing);

            Favorite favorite = Favorite.builder()
                    .user(buyer)
                    .listing(newListing)
                    .build();
            favoriteRepository.save(favorite);
        }

        mockMvc.perform(get("/api/favorites/count")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(content().string("3"));
    }

    @Test
    @DisplayName("Should return 401 when not authenticated")
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/favorites/{listingId}", listing.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
