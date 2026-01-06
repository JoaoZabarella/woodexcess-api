package com.z.c.woodexcess_api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.z.c.woodexcess_api.dto.offer.CounterOfferRequest;
import com.z.c.woodexcess_api.dto.offer.CreateOfferRequest;
import com.z.c.woodexcess_api.dto.offer.RejectOfferRequest;
import com.z.c.woodexcess_api.model.*;
import com.z.c.woodexcess_api.model.enums.*;
import com.z.c.woodexcess_api.repository.*;
import com.z.c.woodexcess_api.security.JwtProvider;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("OfferController Integration Tests")
class OfferControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private MaterialListingRepository listingRepository;

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    private String buyerToken;
    private String sellerToken;
    private UUID buyerId;
    private UUID sellerId;
    private UUID otherBuyerId;
    private String otherBuyerToken;
    private UUID listingId;
    private User buyer;
    private User seller;
    private User otherBuyer;
    private MaterialListing listing;

    @BeforeEach
    @Transactional
    void setUp() {

        notificationRepository.deleteAll();
        offerRepository.deleteAll();
        listingRepository.deleteAll();
        addressRepository.deleteAll();
        userRepository.deleteAll();

        seller = User.builder()
                .name("Jane Seller")
                .email("seller@test.com")
                .phone("11987650000")
                .password(passwordEncoder.encode("password123"))
                .role(UserRole.USER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        seller = userRepository.save(seller);
        sellerId = seller.getId();
        sellerToken = "Bearer " + jwtProvider.generateJwtToken(seller);


        buyer = User.builder()
                .name("John Buyer")
                .email("buyer@test.com")
                .phone("11987650001")
                .password(passwordEncoder.encode("password123"))
                .role(UserRole.USER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        buyer = userRepository.save(buyer);
        buyerId = buyer.getId();
        buyerToken = "Bearer " + jwtProvider.generateJwtToken(buyer);


        otherBuyer = User.builder()
                .name("Another Buyer")
                .email("other@test.com")
                .phone("11987650002")
                .password(passwordEncoder.encode("password123"))
                .role(UserRole.USER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        otherBuyer = userRepository.save(otherBuyer);
        otherBuyerId = otherBuyer.getId();
        otherBuyerToken = "Bearer " + jwtProvider.generateJwtToken(otherBuyer);


        Address address = Address.builder()
                .user(seller)
                .street("Test Street")
                .number("123")
                .district("Test District")
                .city("São Paulo")
                .state("SP")
                .zipCode("01234-567")
                .country("Brasil")
                .isPrimary(true)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        address = addressRepository.save(address);

        // Listing
        listing = MaterialListing.builder()
                .title("Oak Wood Planks")
                .description("High quality oak wood")
                .materialType(MaterialType.WOOD)
                .price(BigDecimal.valueOf(150.50))
                .quantity(10)
                .condition(Condition.USED)
                .owner(seller)
                .address(address)
                .city("São Paulo")
                .state("SP")
                .status(ListingStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        listing = listingRepository.save(listing);
        listingId = listing.getId();
    }


    @Test
    @Order(1)
    @DisplayName("POST /api/offers - Should create offer successfully")
    void createOffer_Success() throws Exception {
        CreateOfferRequest request = new CreateOfferRequest(
                listingId,
                new BigDecimal("120.00"),
                5,
                "Interested in your material"
        );

        mockMvc.perform(post("/api/offers")
                        .header("Authorization", buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.listingId").value(listingId.toString()))
                .andExpect(jsonPath("$.buyerId").value(buyerId.toString()))
                .andExpect(jsonPath("$.sellerId").value(sellerId.toString()))
                .andExpect(jsonPath("$.offeredPrice").value(120.00))
                .andExpect(jsonPath("$.quantity").value(5))
                .andExpect(jsonPath("$.status").value("PENDING"));

        assertThat(offerRepository.count()).isEqualTo(1);
        assertThat(notificationRepository.countByUserIdAndIsReadFalse(sellerId)).isEqualTo(1);
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/offers - Should fail without authentication")
    void createOffer_Unauthorized() throws Exception {
        CreateOfferRequest request = new CreateOfferRequest(
                listingId,
                new BigDecimal("120.00"),
                5,
                "Interested in your material"
        );

        mockMvc.perform(post("/api/offers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/offers - Should fail when buyer is owner")
    void createOffer_BuyerIsOwner() throws Exception {
        CreateOfferRequest request = new CreateOfferRequest(
                listingId,
                new BigDecimal("120.00"),
                5,
                "Interested in your material"
        );

        mockMvc.perform(post("/api/offers")
                        .header("Authorization", sellerToken) // seller tentando ofertar
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Cannot make offer on your own listing")));
    }



    @Test
    @Order(4)
    @DisplayName("POST /api/offers/{id}/counter - Should create counter-offer successfully")
    void createCounterOffer_Success() throws Exception {
        Offer originalOffer = createOfferEntity(buyer, seller, new BigDecimal("120.00"), OfferStatus.PENDING);

        CounterOfferRequest request = new CounterOfferRequest(
                new BigDecimal("135.00"),
                "Counter offer from seller"
        );

        mockMvc.perform(post("/api/offers/{offerId}/counter", originalOffer.getId())
                        .header("Authorization", sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentOfferId").value(originalOffer.getId().toString()))
                .andExpect(jsonPath("$.offeredPrice").value(135.00))
                .andExpect(jsonPath("$.status").value("PENDING"));

        Offer reloadedOriginal = offerRepository.findById(originalOffer.getId()).orElseThrow();
        assertThat(reloadedOriginal.getStatus()).isEqualTo(OfferStatus.COUNTER_OFFERED);

        List<Offer> allOffers = offerRepository.findAll();
        assertThat(allOffers).hasSize(2);
        assertThat(notificationRepository.countByUserIdAndIsReadFalse(buyerId)).isEqualTo(1);
    }

    @Test
    @Order(5)
    @DisplayName("POST /api/offers/{id}/counter - Should fail when non-seller tries to counter")
    void createCounterOffer_NonSeller() throws Exception {
        Offer originalOffer = createOfferEntity(buyer, seller, new BigDecimal("120.00"), OfferStatus.PENDING);

        CounterOfferRequest request = new CounterOfferRequest(
                new BigDecimal("135.00"),
                "Counter offer from not seller"
        );

        mockMvc.perform(post("/api/offers/{offerId}/counter", originalOffer.getId())
                        .header("Authorization", buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Only the seller")));
    }


    @Test
    @Order(6)
    @DisplayName("POST /api/offers/{id}/accept - Should accept offer and reject others")
    void acceptOffer_Success() throws Exception {
        Offer offer1 = createOfferEntity(buyer, seller, new BigDecimal("120.00"), OfferStatus.PENDING);
        Offer offer2 = createOfferEntity(otherBuyer, seller, new BigDecimal("130.00"), OfferStatus.PENDING);

        mockMvc.perform(post("/api/offers/{offerId}/accept", offer1.getId())
                        .header("Authorization", sellerToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(offer1.getId().toString()))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        Offer accepted = offerRepository.findById(offer1.getId()).orElseThrow();
        Offer rejected = offerRepository.findById(offer2.getId()).orElseThrow();

        assertThat(accepted.getStatus()).isEqualTo(OfferStatus.ACCEPTED);
        assertThat(rejected.getStatus()).isEqualTo(OfferStatus.REJECTED);

        assertThat(notificationRepository.countByUserIdAndIsReadFalse(buyerId)).isEqualTo(1);
        assertThat(notificationRepository.countByUserIdAndIsReadFalse(otherBuyerId)).isEqualTo(1);
    }

    @Test
    @Order(7)
    @DisplayName("POST /api/offers/{id}/accept - Should fail when non-seller tries to accept")
    void acceptOffer_NonSeller() throws Exception {
        Offer offer = createOfferEntity(buyer, seller, new BigDecimal("120.00"), OfferStatus.PENDING);

        mockMvc.perform(post("/api/offers/{offerId}/accept", offer.getId())
                        .header("Authorization", buyerToken))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Only the seller")));
    }



    @Test
    @Order(8)
    @DisplayName("POST /api/offers/{id}/reject - Should reject offer and notify buyer")
    void rejectOffer_Success() throws Exception {
        Offer offer = createOfferEntity(buyer, seller, new BigDecimal("120.00"), OfferStatus.PENDING);

        RejectOfferRequest request = new RejectOfferRequest("Price too low");

        mockMvc.perform(post("/api/offers/{offerId}/reject", offer.getId())
                        .header("Authorization", sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        Offer rejected = offerRepository.findById(offer.getId()).orElseThrow();
        assertThat(rejected.getStatus()).isEqualTo(OfferStatus.REJECTED);
        assertThat(notificationRepository.countByUserIdAndIsReadFalse(buyerId)).isEqualTo(1);
    }


    @Test
    @Order(9)
    @DisplayName("DELETE /api/offers/{id} - Should cancel offer and notify seller")
    void cancelOffer_Success() throws Exception {
        Offer offer = createOfferEntity(buyer, seller, new BigDecimal("120.00"), OfferStatus.PENDING);

        mockMvc.perform(delete("/api/offers/{offerId}", offer.getId())
                        .header("Authorization", buyerToken))
                .andDo(print())
                .andExpect(status().isNoContent());

        Offer cancelled = offerRepository.findById(offer.getId()).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(OfferStatus.CANCELLED);
        assertThat(notificationRepository.countByUserIdAndIsReadFalse(sellerId)).isEqualTo(1);
    }



    @Test
    @Order(10)
    @DisplayName("GET /api/offers/sent - Should get sent offers")
    void getSentOffers_Success() throws Exception {
        createOfferEntity(buyer, seller, new BigDecimal("120.00"), OfferStatus.PENDING);
        createOfferEntity(buyer, seller, new BigDecimal("130.00"), OfferStatus.REJECTED);

        mockMvc.perform(get("/api/offers/sent")
                        .header("Authorization", buyerToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].buyerId").value(buyerId.toString()));
    }

    @Test
    @Order(11)
    @DisplayName("GET /api/offers/received - Should get received offers")
    void getReceivedOffers_Success() throws Exception {
        createOfferEntity(buyer, seller, new BigDecimal("120.00"), OfferStatus.PENDING);
        createOfferEntity(otherBuyer, seller, new BigDecimal("130.00"), OfferStatus.PENDING);

        mockMvc.perform(get("/api/offers/received")
                        .header("Authorization", sellerToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].sellerId").value(sellerId.toString()));
    }

    @Test
    @Order(12)
    @DisplayName("GET /api/offers/{id}/chain - Should get offer chain")
    void getOfferChain_Success() throws Exception {
        Offer original = createOfferEntity(buyer, seller, new BigDecimal("120.00"), OfferStatus.PENDING);
        Offer counter = offerRepository.save(
                Offer.builder()
                        .listing(listing)
                        .buyer(buyer)
                        .seller(seller)
                        .offeredPrice(new BigDecimal("130.00"))
                        .quantity(5)
                        .status(OfferStatus.PENDING)
                        .parentOffer(original)
                        .expiresAt(LocalDateTime.now().plusHours(48))
                        .build()
        );

        mockMvc.perform(get("/api/offers/{id}/chain", original.getId())
                        .header("Authorization", buyerToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    private Offer createOfferEntity(User buyer, User seller, BigDecimal price, OfferStatus status) {
        Offer offer = Offer.builder()
                .listing(listing)
                .buyer(buyer)
                .seller(seller)
                .offeredPrice(price)
                .quantity(5)
                .status(status)
                .expiresAt(LocalDateTime.now().plusHours(48))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return offerRepository.save(offer);
    }
}
