package com.z.c.woodexcess_api.service;

import com.z.c.woodexcess_api.dto.notification.CreateNotificationCommand;
import com.z.c.woodexcess_api.dto.offer.*;
import com.z.c.woodexcess_api.exception.BusinessException;
import com.z.c.woodexcess_api.exception.ResourceNotFoundException;
import com.z.c.woodexcess_api.mapper.OfferMapper;
import com.z.c.woodexcess_api.model.MaterialListing;
import com.z.c.woodexcess_api.model.Offer;
import com.z.c.woodexcess_api.model.User;
import com.z.c.woodexcess_api.model.enums.ListingStatus;
import com.z.c.woodexcess_api.model.enums.NotificationType;
import com.z.c.woodexcess_api.model.enums.OfferStatus;
import com.z.c.woodexcess_api.repository.MaterialListingRepository;
import com.z.c.woodexcess_api.repository.OfferRepository;
import com.z.c.woodexcess_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OfferService {

    private final OfferRepository offerRepository;
    private final MaterialListingRepository listingRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final Clock clock;

    @Value("${app.offer.default-expiration-hours:48}")
    private int defaultOfferExpirationHours;

    @Transactional
    public OfferResponse createOffer(CreateOfferRequest request, UUID buyerId) {
        log.info("Creating offer for listing: {}, buyer: {}", request.listingId(), buyerId);

        if (request.offeredPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Offered price must be greater than zero");
        }

        if (request.quantity() <= 0) {
            throw new BusinessException("Quantity must be greater than zero");
        }

        MaterialListing listing = listingRepository.findById(request.listingId())
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found: " + request.listingId()));

        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new BusinessException("Cannot make offer on inactive listing");
        }

        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer not found: " + buyerId));

        User seller = listing.getOwner();

        if (buyer.getId().equals(seller.getId())) {
            throw new BusinessException("Cannot make offer on your own listing");
        }

        if (request.quantity() > listing.getQuantity()) {
            throw new BusinessException("Requested quantity exceeds available quantity");
        }

        if (offerRepository.existsByBuyerIdAndListingIdAndStatus(buyerId, request.listingId(), OfferStatus.PENDING)) {
            throw new BusinessException("You already have a pending offer for this listing");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime expiresAt = now.plusHours(defaultOfferExpirationHours);

        Offer offer = Offer.builder()
                .listing(listing)
                .buyer(buyer)
                .seller(seller)
                .offeredPrice(request.offeredPrice())
                .quantity(request.quantity())
                .message(request.message())
                .status(OfferStatus.PENDING)
                .expiresAt(expiresAt)
                .build();

        Offer savedOffer = offerRepository.save(offer);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("offer_id", savedOffer.getId().toString());
        metadata.put("listing_id", request.listingId().toString());
        metadata.put("listing_title", listing.getTitle());
        metadata.put("buyer_name", buyer.getName());
        metadata.put("offered_price", request.offeredPrice());
        metadata.put("quantity", request.quantity());
        metadata.put("original_price", listing.getPrice());

        CreateNotificationCommand notificationCommand = CreateNotificationCommand.builder()
                .userId(seller.getId())
                .type(NotificationType.NEW_OFFER)
                .title("New Offer Received!")
                .message(String.format("%s made an offer of R$ %.2f for %d unit(s) of %s",
                        buyer.getName(), request.offeredPrice(), request.quantity(), listing.getTitle()))
                .linkUrl("/offers/" + savedOffer.getId())
                .metadata(metadata)
                .build();

        notificationService.createNotification(notificationCommand);

        log.info("Offer created successfully: {}", savedOffer.getId());
        return OfferMapper.toResponse(savedOffer);
    }

    @Transactional
    public OfferResponse createCounterOffer(UUID originalOfferId, CounterOfferRequest request, UUID sellerId) {
        log.info("Creating counter-offer for offer: {}, seller: {}", originalOfferId, sellerId);

        if (request.counterPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Counter price must be greater than zero");
        }

        Offer originalOffer = offerRepository.findByIdWithDetails(originalOfferId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found: " + originalOfferId));

        if (!originalOffer.getSeller().getId().equals(sellerId)) {
            throw new BusinessException("Only the seller can make a counter-offer");
        }

        if (!originalOffer.canBeCountered()) {
            throw new BusinessException("Offer cannot be countered (expired or not pending)");
        }

        originalOffer.setStatus(OfferStatus.COUNTER_OFFERED);
        offerRepository.save(originalOffer);

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime expiresAt = now.plusHours(defaultOfferExpirationHours);

        Offer counterOffer = Offer.builder()
                .listing(originalOffer.getListing())
                .buyer(originalOffer.getBuyer())
                .seller(originalOffer.getSeller())
                .offeredPrice(request.counterPrice())
                .quantity(originalOffer.getQuantity())
                .message(request.message())
                .status(OfferStatus.PENDING)
                .parentOffer(originalOffer)
                .expiresAt(expiresAt)
                .build();

        Offer savedCounterOffer = offerRepository.save(counterOffer);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("offer_id", savedCounterOffer.getId().toString());
        metadata.put("original_offer_id", originalOfferId.toString());
        metadata.put("listing_id", originalOffer.getListing().getId().toString());
        metadata.put("counter_price", request.counterPrice());

        CreateNotificationCommand notificationCommand = CreateNotificationCommand.builder()
                .userId(originalOffer.getBuyer().getId())
                .type(NotificationType.COUNTER_OFFER)
                .title("Counter Offer Received!")
                .message(String.format("The seller made a counter offer of R$ %.2f", request.counterPrice()))
                .linkUrl("/offers/" + savedCounterOffer.getId())
                .metadata(metadata)
                .build();

        notificationService.createNotification(notificationCommand);

        log.info("Counter-offer created successfully: {}", savedCounterOffer.getId());
        return OfferMapper.toResponse(savedCounterOffer);
    }

    @Transactional
    public OfferResponse acceptOffer(UUID offerId, UUID sellerId) {
        log.info("Accepting offer: {}, seller: {}", offerId, sellerId);

        Offer offer = offerRepository.findByIdWithPessimisticLock(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found: " + offerId));

        if (!offer.getSeller().getId().equals(sellerId)) {
            throw new BusinessException("Only the seller can accept this offer");
        }

        if (!offer.canBeAccepted()) {
            throw new BusinessException("Offer cannot be accepted (expired or not pending)");
        }

        List<Offer> allPendingOffers = offerRepository.findByListingIdAndStatusWithPessimisticLock(
                offer.getListing().getId(),
                OfferStatus.PENDING
        );

        List<Offer> offersToReject = allPendingOffers.stream()
                .filter(o -> !o.getId().equals(offerId))
                .map(o -> {
                    o.setStatus(OfferStatus.REJECTED);
                    return o;
                })
                .toList();

        offer.setStatus(OfferStatus.ACCEPTED);
        Offer acceptedOffer = offerRepository.save(offer);

        if (!offersToReject.isEmpty()) {
            offerRepository.saveAll(offersToReject);
        }

        publishOfferAcceptedEvent(acceptedOffer, offersToReject);

        log.info("Offer accepted successfully: {}", offerId);
        return OfferMapper.toResponse(acceptedOffer);
    }

    @Transactional
    public OfferResponse rejectOffer(UUID offerId, RejectOfferRequest request, UUID sellerId) {
        log.info("Rejecting offer: {}, seller: {}", offerId, sellerId);

        Offer offer = offerRepository.findByIdWithDetails(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found: " + offerId));

        if (!offer.getSeller().getId().equals(sellerId)) {
            throw new BusinessException("Only the seller can reject this offer");
        }

        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new BusinessException("Only pending offers can be rejected");
        }

        offer.setStatus(OfferStatus.REJECTED);
        Offer rejectedOffer = offerRepository.save(offer);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("offer_id", offerId.toString());
        if (request.reason() != null) {
            metadata.put("reason", request.reason());
        }

        CreateNotificationCommand notificationCommand = CreateNotificationCommand.builder()
                .userId(offer.getBuyer().getId())
                .type(NotificationType.OFFER_REJECTED)
                .title("Offer Rejected")
                .message(request.reason() != null ? request.reason() : "Your offer was rejected by the seller")
                .linkUrl("/offers/" + offerId)
                .metadata(metadata)
                .build();

        notificationService.createNotification(notificationCommand);

        log.info("Offer rejected successfully: {}", offerId);
        return OfferMapper.toResponse(rejectedOffer);
    }

    @Transactional
    public OfferResponse cancelOffer(UUID offerId, UUID buyerId) {
        log.info("Cancelling offer: {}, buyer: {}", offerId, buyerId);

        Offer offer = offerRepository.findByIdWithDetails(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found: " + offerId));

        if (!offer.getBuyer().getId().equals(buyerId)) {
            throw new BusinessException("Only the buyer can cancel this offer");
        }

        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new BusinessException("Only pending offers can be cancelled");
        }

        offer.setStatus(OfferStatus.CANCELLED);
        Offer cancelledOffer = offerRepository.save(offer);

        CreateNotificationCommand notificationCommand = CreateNotificationCommand.builder()
                .userId(offer.getSeller().getId())
                .type(NotificationType.OFFER_CANCELLED)
                .title("Offer Cancelled")
                .message(String.format("The offer of R$ %.2f was cancelled by the buyer", offer.getOfferedPrice()))
                .linkUrl("/offers/" + offerId)
                .metadata(Map.of("offer_id", offerId.toString()))
                .build();

        notificationService.createNotification(notificationCommand);

        log.info("Offer cancelled successfully: {}", offerId);
        return OfferMapper.toResponse(cancelledOffer);
    }

    @Transactional(readOnly = true)
    public OfferResponse getOfferById(UUID offerId, UUID userId) {
        log.debug("Fetching offer: {}, user: {}", offerId, userId);

        Offer offer = offerRepository.findByIdWithDetails(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found: " + offerId));

        if (!offer.getBuyer().getId().equals(userId) && !offer.getSeller().getId().equals(userId)) {
            throw new BusinessException("You don't have permission to view this offer");
        }

        return OfferMapper.toResponse(offer);
    }

    @Transactional(readOnly = true)
    public Page<OfferSummaryResponse> getOffersSent(UUID buyerId, OfferStatus status, Pageable pageable) {
        log.debug("Fetching offers sent by buyer: {}, status: {}", buyerId, status);

        Page<Offer> offers;

        if (status != null) {
            offers = offerRepository.findByBuyerIdAndStatusOrderByCreatedAtDesc(buyerId, status, pageable);
        } else {
            offers = offerRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId, pageable);
        }

        return offers.map(offer -> OfferMapper.toSummaryResponse(offer, buyerId));
    }

    @Transactional(readOnly = true)
    public Page<OfferSummaryResponse> getOffersReceived(UUID sellerId, OfferStatus status, Pageable pageable) {
        log.debug("Fetching offers received by seller: {}, status: {}", sellerId, status);

        Page<Offer> offers;
        if (status != null) {
            offers = offerRepository.findBySellerIdAndStatusOrderByCreatedAtDesc(sellerId, status, pageable);
        } else {
            offers = offerRepository.findBySellerIdOrderByCreatedAtDesc(sellerId, pageable);
        }
        return offers.map(offer -> OfferMapper.toSummaryResponse(offer, sellerId));
    }

    @Transactional(readOnly = true)
    public List<OfferResponse> getOfferChain(UUID offerId, UUID userId) {
        log.debug("Fetching offer chain: {}", offerId);

        List<Offer> chain = offerRepository.findOfferChainWithDetails(offerId);

        if (chain.isEmpty()) {
            throw new ResourceNotFoundException("Offer not found: " + offerId);
        }

        Offer firstOffer = chain.get(0);
        if (!firstOffer.getBuyer().getId().equals(userId) && !firstOffer.getSeller().getId().equals(userId)) {
            throw new BusinessException("You don't have permission to view this offer chain");
        }

        return chain.stream()
                .map(OfferMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countPendingOffersReceived(UUID sellerId) {
        return offerRepository.countBySellerIdAndStatus(sellerId, OfferStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public long countPendingOffersSent(UUID buyerId) {
        return offerRepository.countByBuyerIdAndStatus(buyerId, OfferStatus.PENDING);
    }

    @Transactional
    public void expireOffers() {
        log.info("Running offer expiration job");

        LocalDateTime now = LocalDateTime.now(clock);
        List<Offer> expiredOffers = offerRepository.findExpiredOffersWithDetails(now);

        expiredOffers.forEach(offer -> {
            offer.setStatus(OfferStatus.EXPIRED);

            CreateNotificationCommand notificationCommand = CreateNotificationCommand.builder()
                    .userId(offer.getBuyer().getId())
                    .type(NotificationType.OFFER_EXPIRED)
                    .title("Offer Expired")
                    .message(String.format("Your offer expired after %d hours without a response", defaultOfferExpirationHours))
                    .linkUrl("/offers/" + offer.getId())
                    .metadata(Map.of("offer_id", offer.getId().toString()))
                    .build();

            notificationService.createNotification(notificationCommand);

            log.debug("Offer expired: {}", offer.getId());
        });

        offerRepository.saveAll(expiredOffers);

        log.info("Expired {} offers", expiredOffers.size());
    }

    private void publishOfferAcceptedEvent(Offer acceptedOffer, List<Offer> rejectedOffers) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("offer_id", acceptedOffer.getId().toString());
        metadata.put("listing_id", acceptedOffer.getListing().getId().toString());

        CreateNotificationCommand acceptedCommand = CreateNotificationCommand.builder()
                .userId(acceptedOffer.getBuyer().getId())
                .type(NotificationType.OFFER_ACCEPTED)
                .title("Offer Accepted!")
                .message(String.format("Your offer of R$ %.2f was accepted!", acceptedOffer.getOfferedPrice()))
                .linkUrl("/offers/" + acceptedOffer.getId())
                .metadata(metadata)
                .build();

        notificationService.createNotification(acceptedCommand);

        rejectedOffers.forEach(o -> {
            CreateNotificationCommand rejectedCommand = CreateNotificationCommand.builder()
                    .userId(o.getBuyer().getId())
                    .type(NotificationType.OFFER_REJECTED)
                    .title("Offer Rejected")
                    .message("Your offer was rejected because another offer was accepted")
                    .linkUrl("/offers/" + o.getId())
                    .metadata(Map.of("offer_id", o.getId().toString()))
                    .build();

            notificationService.createNotification(rejectedCommand);
        });
    }
}
