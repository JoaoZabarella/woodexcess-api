package com.z.c.woodexcess_api.service;

import com.z.c.woodexcess_api.dto.offer.*;
import com.z.c.woodexcess_api.exception.BusinessException;
import com.z.c.woodexcess_api.exception.ResourceNotFoundException;
import com.z.c.woodexcess_api.model.*;
import com.z.c.woodexcess_api.model.enums.*;
import com.z.c.woodexcess_api.repository.MaterialListingRepository;
import com.z.c.woodexcess_api.repository.OfferRepository;
import com.z.c.woodexcess_api.repository.UserRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OfferService Complete Unit Tests")
class OfferServiceTest {

    @Mock
    private OfferRepository offerRepository;

    @Mock
    private MaterialListingRepository listingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private Clock clock;

    @InjectMocks
    private OfferService offerService;

    private User buyer;
    private User seller;
    private User anotherUser;
    private Address address;
    private MaterialListing listing;
    private Offer offer;
    private CreateOfferRequest createRequest;
    private CounterOfferRequest counterRequest;
    private RejectOfferRequest rejectRequest;

    private static final Instant FIXED_INSTANT = Instant.parse("2025-12-19T18:00:00Z");
    private static final ZoneId ZONE_ID = ZoneId.systemDefault();
    private static final LocalDateTime FIXED_TIME = LocalDateTime.ofInstant(FIXED_INSTANT, ZONE_ID);

    @BeforeEach
    void setUp() {
        // Mock Clock para todos os testes que usam LocalDateTime.now(clock)
        lenient().when(clock.instant()).thenReturn(FIXED_INSTANT);
        lenient().when(clock.getZone()).thenReturn(ZONE_ID);

        ReflectionTestUtils.setField(offerService, "defaultOfferExpirationHours", 48);

        buyer = User.builder()
                .id(UUID.randomUUID())
                .name("John Buyer")
                .email("buyer@example.com")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        seller = User.builder()
                .id(UUID.randomUUID())
                .name("Jane Seller")
                .email("seller@example.com")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        anotherUser = User.builder()
                .id(UUID.randomUUID())
                .name("Another User")
                .email("another@example.com")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        address = Address.builder()
                .id(UUID.randomUUID())
                .user(seller)
                .street("Rua Teste")
                .number("123")
                .city("São Paulo")
                .state("SP")
                .zipCode("01310-100")
                .country("Brasil")
                .isActive(true)
                .isPrimary(true)
                .build();

        listing = MaterialListing.builder()
                .id(UUID.randomUUID())
                .owner(seller)
                .title("Sobra de Madeira")
                .description("Madeira em bom estado")
                .materialType(MaterialType.WOOD)
                .price(new BigDecimal("150.00"))
                .quantity(10)
                .condition(Condition.USED)
                .address(address)
                .city("São Paulo")
                .state("SP")
                .status(ListingStatus.ACTIVE)
                .build();

        offer = Offer.builder()
                .id(UUID.randomUUID())
                .listing(listing)
                .buyer(buyer)
                .seller(seller)
                .offeredPrice(new BigDecimal("120.00"))
                .quantity(5)
                .message("Interested in your material")
                .status(OfferStatus.PENDING)
                .expiresAt(FIXED_TIME.plusHours(48))
                .build();

        createRequest = new CreateOfferRequest(
                listing.getId(),
                new BigDecimal("120.00"),
                5,
                "Interested in your material"
        );

        counterRequest = new CounterOfferRequest(
                new BigDecimal("135.00"),
                "Counter offer"
        );

        rejectRequest = new RejectOfferRequest("Price too low");
    }



    @Test
    @DisplayName("Should create offer successfully")
    void shouldCreateOfferSuccessfully() {
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));
        when(userRepository.findById(buyer.getId())).thenReturn(Optional.of(buyer));
        when(offerRepository.existsByBuyerIdAndListingIdAndStatus(buyer.getId(), listing.getId(), OfferStatus.PENDING))
                .thenReturn(false);

        when(offerRepository.save(any(Offer.class))).thenAnswer(invocation -> {
            Offer savedOffer = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedOffer, "id", UUID.randomUUID());
            return savedOffer;
        });

        OfferResponse result = offerService.createOffer(createRequest, buyer.getId());

        assertThat(result).isNotNull();
        assertThat(result.offeredPrice()).isEqualByComparingTo(new BigDecimal("120.00"));
        assertThat(result.status()).isEqualTo(OfferStatus.PENDING);
        // Flags de tempo
        assertThat(result.isExpired()).isFalse();
        assertThat(result.canBeAccepted()).isTrue();
        assertThat(result.canBeCountered()).isTrue();

        verify(listingRepository).findById(listing.getId());
        verify(userRepository).findById(buyer.getId());
        verify(offerRepository).save(any(Offer.class));
        verify(notificationService).createNotification(any());
    }

    @Test
    @DisplayName("Should throw exception when offered price is zero or negative")
    void shouldThrowExceptionWhenOfferedPriceInvalid() {
        CreateOfferRequest invalidRequest = new CreateOfferRequest(
                listing.getId(),
                BigDecimal.ZERO,
                5,
                "Message"
        );

        assertThatThrownBy(() -> offerService.createOffer(invalidRequest, buyer.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("greater than zero");

        verify(offerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when quantity is zero or negative")
    void shouldThrowExceptionWhenQuantityInvalid() {
        CreateOfferRequest invalidRequest = new CreateOfferRequest(
                listing.getId(),
                new BigDecimal("100.00"),
                0,
                "Message"
        );

        assertThatThrownBy(() -> offerService.createOffer(invalidRequest, buyer.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Quantity must be greater than zero");

        verify(offerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when listing not found")
    void shouldThrowExceptionWhenListingNotFound() {
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> offerService.createOffer(createRequest, buyer.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Listing not found");

        verify(offerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when listing is not active")
    void shouldThrowExceptionWhenListingNotActive() {
        listing.setStatus(ListingStatus.INACTIVE);

        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> offerService.createOffer(createRequest, buyer.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inactive listing");

        verify(offerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when buyer not found")
    void shouldThrowExceptionWhenBuyerNotFound() {
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));
        when(userRepository.findById(buyer.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> offerService.createOffer(createRequest, buyer.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Buyer not found");

        verify(offerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when buyer tries to offer on own listing")
    void shouldThrowExceptionWhenBuyerIsOwner() {
        listing.setOwner(buyer);

        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));
        when(userRepository.findById(buyer.getId())).thenReturn(Optional.of(buyer));

        assertThatThrownBy(() -> offerService.createOffer(createRequest, buyer.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("own listing");

        verify(offerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when quantity exceeds available")
    void shouldThrowExceptionWhenQuantityExceedsAvailable() {
        CreateOfferRequest invalidRequest = new CreateOfferRequest(
                listing.getId(),
                new BigDecimal("120.00"),
                50,
                "Too much"
        );

        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));
        when(userRepository.findById(buyer.getId())).thenReturn(Optional.of(buyer));

        assertThatThrownBy(() -> offerService.createOffer(invalidRequest, buyer.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("exceeds available quantity");

        verify(offerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when buyer already has pending offer")
    void shouldThrowExceptionWhenPendingOfferExists() {
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));
        when(userRepository.findById(buyer.getId())).thenReturn(Optional.of(buyer));
        when(offerRepository.existsByBuyerIdAndListingIdAndStatus(buyer.getId(), listing.getId(), OfferStatus.PENDING))
                .thenReturn(true);

        assertThatThrownBy(() -> offerService.createOffer(createRequest, buyer.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already have a pending offer");

        verify(offerRepository, never()).save(any());
    }


    @Test
    @DisplayName("Should create counter offer successfully")
    void shouldCreateCounterOfferSuccessfully() {
        when(offerRepository.findByIdWithDetails(offer.getId())).thenReturn(Optional.of(offer));
        when(offerRepository.save(any(Offer.class))).thenAnswer(invocation -> {
            Offer savedOffer = invocation.getArgument(0);
            if (savedOffer.getId() == null) {
                ReflectionTestUtils.setField(savedOffer, "id", UUID.randomUUID());
            }
            return savedOffer;
        });

        OfferResponse result = offerService.createCounterOffer(offer.getId(), counterRequest, seller.getId());

        assertThat(result).isNotNull();
        assertThat(offer.getStatus()).isEqualTo(OfferStatus.COUNTER_OFFERED);

        verify(offerRepository).findByIdWithDetails(offer.getId());
        verify(offerRepository, times(2)).save(any(Offer.class));
        verify(notificationService).createNotification(any());
    }

    @Test
    @DisplayName("Should throw exception when counter price is invalid")
    void shouldThrowExceptionWhenCounterPriceInvalid() {
        CounterOfferRequest invalidRequest = new CounterOfferRequest(BigDecimal.ZERO, "Message");

        assertThatThrownBy(() -> offerService.createCounterOffer(offer.getId(), invalidRequest, seller.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("greater than zero");

        verify(offerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when offer not found for counter")
    void shouldThrowExceptionWhenOfferNotFoundForCounter() {
        when(offerRepository.findByIdWithDetails(offer.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> offerService.createCounterOffer(offer.getId(), counterRequest, seller.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Offer not found");

        verify(offerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when non-seller tries to counter offer")
    void shouldThrowExceptionWhenNonSellerTriesToCounter() {
        when(offerRepository.findByIdWithDetails(offer.getId())).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> offerService.createCounterOffer(offer.getId(), counterRequest, buyer.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Only the seller");

        verify(offerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when offer cannot be countered")
    void shouldThrowExceptionWhenOfferCannotBeCountered() {
        offer.setStatus(OfferStatus.ACCEPTED);

        when(offerRepository.findByIdWithDetails(offer.getId())).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> offerService.createCounterOffer(offer.getId(), counterRequest, seller.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot be countered");

        verify(offerRepository, never()).save(any());
    }


    @Test
    @DisplayName("Should accept offer successfully")
    void shouldAcceptOfferSuccessfully() {
        when(offerRepository.findByIdWithPessimisticLock(offer.getId())).thenReturn(Optional.of(offer));
        when(offerRepository.findByListingIdAndStatusWithPessimisticLock(listing.getId(), OfferStatus.PENDING))
                .thenReturn(List.of(offer));
        when(offerRepository.save(offer)).thenReturn(offer);

        OfferResponse result = offerService.acceptOffer(offer.getId(), seller.getId());

        assertThat(result).isNotNull();
        assertThat(offer.getStatus()).isEqualTo(OfferStatus.ACCEPTED);
        assertThat(result.canBeAccepted()).isFalse();
        assertThat(result.isExpired()).isFalse();

        verify(offerRepository).findByIdWithPessimisticLock(offer.getId());
        verify(offerRepository).save(offer);
        verify(notificationService, atLeastOnce()).createNotification(any());
    }

    @Test
    @DisplayName("Should reject other pending offers when accepting one")
    void shouldRejectOtherPendingOffersWhenAccepting() {
        offer.setStatus(OfferStatus.PENDING);
        offer.setExpiresAt(FIXED_TIME.plusHours(49));

        Offer otherOffer = Offer.builder()
                .id(UUID.randomUUID())
                .listing(listing)
                .buyer(anotherUser)
                .seller(seller)
                .offeredPrice(new BigDecimal("120.00"))
                .quantity(3)
                .status(OfferStatus.PENDING)
                .expiresAt(FIXED_TIME.plusHours(49))
                .build();

        when(offerRepository.findByIdWithPessimisticLock(offer.getId())).thenReturn(Optional.of(offer));
        when(offerRepository.findByListingIdAndStatusWithPessimisticLock(listing.getId(), OfferStatus.PENDING))
                .thenReturn(List.of(offer, otherOffer));
        when(offerRepository.save(any(Offer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        offerService.acceptOffer(offer.getId(), seller.getId());

        assertThat(otherOffer.getStatus()).isEqualTo(OfferStatus.REJECTED);
        verify(offerRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should throw exception when non-seller tries to accept")
    void shouldThrowExceptionWhenNonSellerTriesToAccept() {
        when(offerRepository.findByIdWithPessimisticLock(offer.getId())).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> offerService.acceptOffer(offer.getId(), buyer.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Only the seller can accept");

        verify(offerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when offer cannot be accepted")
    void shouldThrowExceptionWhenOfferCannotBeAccepted() {
        offer.setStatus(OfferStatus.REJECTED);

        when(offerRepository.findByIdWithPessimisticLock(offer.getId())).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> offerService.acceptOffer(offer.getId(), seller.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot be accepted");

        verify(offerRepository, never()).save(any());
    }


    @Test
    @DisplayName("Should reject offer successfully")
    void shouldRejectOfferSuccessfully() {
        when(offerRepository.findByIdWithDetails(offer.getId())).thenReturn(Optional.of(offer));
        when(offerRepository.save(offer)).thenReturn(offer);

        OfferResponse result = offerService.rejectOffer(offer.getId(), rejectRequest, seller.getId());

        assertThat(result).isNotNull();
        assertThat(offer.getStatus()).isEqualTo(OfferStatus.REJECTED);
        assertThat(result.canBeAccepted()).isFalse();
        assertThat(result.canBeCountered()).isFalse();

        verify(offerRepository).save(offer);
        verify(notificationService).createNotification(any());
    }

    @Test
    @DisplayName("Should cancel offer successfully")
    void shouldCancelOfferSuccessfully() {
        when(offerRepository.findByIdWithDetails(offer.getId())).thenReturn(Optional.of(offer));
        when(offerRepository.save(offer)).thenReturn(offer);

        OfferResponse result = offerService.cancelOffer(offer.getId(), buyer.getId());

        assertThat(result).isNotNull();
        assertThat(offer.getStatus()).isEqualTo(OfferStatus.CANCELLED);
        assertThat(result.canBeAccepted()).isFalse();
        assertThat(result.canBeCountered()).isFalse();

        verify(offerRepository).save(offer);
        verify(notificationService).createNotification(any());
    }


    @Test
    @DisplayName("Should get offer by ID successfully")
    void shouldGetOfferByIdForBuyer() {
        when(offerRepository.findByIdWithDetails(offer.getId())).thenReturn(Optional.of(offer));

        OfferResponse result = offerService.getOfferById(offer.getId(), buyer.getId());

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(offer.getId());
        verify(offerRepository).findByIdWithDetails(offer.getId());
    }

    @Test
    @DisplayName("Should get offer chain successfully")
    void shouldGetOfferChainSuccessfully() {
        Offer counterOffer = Offer.builder()
                .id(UUID.randomUUID())
                .parentOffer(offer)
                .listing(listing)
                .buyer(buyer)
                .seller(seller)
                .status(OfferStatus.PENDING)
                .expiresAt(FIXED_TIME.plusHours(48))
                .offeredPrice(new BigDecimal("130.00"))
                .quantity(5)
                .build();

        when(offerRepository.findOfferChainWithDetails(offer.getId())).thenReturn(List.of(offer, counterOffer));

        List<OfferResponse> result = offerService.getOfferChain(offer.getId(), buyer.getId());

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        verify(offerRepository).findOfferChainWithDetails(offer.getId());
    }

    @Test
    @DisplayName("Should get offers sent with summaries")
    void shouldGetOffersSent() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Offer> page = new PageImpl<>(List.of(offer), pageable, 1);

        when(offerRepository.findByBuyerIdOrderByCreatedAtDesc(buyer.getId(), pageable))
                .thenReturn(page);

        Page<OfferSummaryResponse> result = offerService.getOffersSent(buyer.getId(), null, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(offer.getId());
    }

    @Test
    @DisplayName("Should get offers received with summaries")
    void shouldGetOffersReceived() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Offer> page = new PageImpl<>(List.of(offer), pageable, 1);

        when(offerRepository.findBySellerIdOrderByCreatedAtDesc(seller.getId(), pageable))
                .thenReturn(page);

        Page<OfferSummaryResponse> result = offerService.getOffersReceived(seller.getId(), null, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(offer.getId());
    }


    @Test
    @DisplayName("Should count pending offers")
    void shouldCountPendingOffersSent() {
        when(offerRepository.countByBuyerIdAndStatus(buyer.getId(), OfferStatus.PENDING)).thenReturn(5L);

        long result = offerService.countPendingOffersSent(buyer.getId());

        assertThat(result).isEqualTo(5L);
        verify(offerRepository).countByBuyerIdAndStatus(buyer.getId(), OfferStatus.PENDING);
    }

    @Test
    @DisplayName("Should expire old offers")
    void shouldExpireOldOffers() {
        List<Offer> expiredOffers = List.of(offer);

        when(offerRepository.findExpiredOffersWithDetails(any(LocalDateTime.class))).thenReturn(expiredOffers);

        offerService.expireOffers();

        assertThat(offer.getStatus()).isEqualTo(OfferStatus.EXPIRED);
        verify(offerRepository).saveAll(expiredOffers);
        verify(notificationService).createNotification(any());
    }
}
