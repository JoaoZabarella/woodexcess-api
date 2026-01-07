package com.z.c.woodexcess_api.repository;

import com.z.c.woodexcess_api.model.Offer;
import com.z.c.woodexcess_api.model.enums.OfferStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OfferRepository extends JpaRepository<Offer, UUID> {

    @EntityGraph(attributePaths = {"listing", "buyer", "seller"})
    Page<Offer> findByBuyerIdOrderByCreatedAtDesc(UUID buyerId, Pageable pageable);

    @EntityGraph(attributePaths = {"listing", "buyer", "seller"})
    Page<Offer> findBySellerIdOrderByCreatedAtDesc(UUID sellerId, Pageable pageable);

    @EntityGraph(attributePaths = {"listing", "buyer", "seller"})
    Page<Offer> findByBuyerIdAndStatusOrderByCreatedAtDesc(
            UUID buyerId,
            OfferStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"listing", "buyer", "seller"})
    Page<Offer> findBySellerIdAndStatusOrderByCreatedAtDesc(
            UUID sellerId,
            OfferStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"listing", "buyer", "seller"})
    Page<Offer> findByListingIdOrderByCreatedAtDesc(UUID listingId, Pageable pageable);

    @Query("SELECT o FROM Offer o " +
            "LEFT JOIN FETCH o.listing l " +
            "LEFT JOIN FETCH o.buyer b " +
            "LEFT JOIN FETCH o.seller s " +
            "WHERE o.listing.id = :listingId AND o.status = :status " +
            "ORDER BY o.createdAt DESC")
    List<Offer> findByListingIdAndStatusWithDetails(
            @Param("listingId") UUID listingId,
            @Param("status") OfferStatus status
    );

    @Query("SELECT o FROM Offer o " +
            "LEFT JOIN FETCH o.listing l " +
            "LEFT JOIN FETCH o.buyer b " +
            "LEFT JOIN FETCH o.seller s " +
            "WHERE o.status = 'PENDING' AND o.expiresAt < :now")
    List<Offer> findExpiredOffersWithDetails(@Param("now") LocalDateTime now);

    @Query("SELECT o FROM Offer o " +
            "LEFT JOIN FETCH o.listing l " +
            "LEFT JOIN FETCH o.buyer b " +
            "LEFT JOIN FETCH o.seller s " +
            "WHERE o.id = :offerId")
    Optional<Offer> findByIdWithDetails(@Param("offerId") UUID offerId);

    @Query("SELECT o FROM Offer o " +
            "LEFT JOIN FETCH o.listing l " +
            "LEFT JOIN FETCH o.buyer b " +
            "LEFT JOIN FETCH o.seller s " +
            "LEFT JOIN FETCH o.parentOffer p " +
            "WHERE o.id = :offerId OR o.parentOffer.id = :offerId " +
            "ORDER BY o.createdAt")
    List<Offer> findOfferChainWithDetails(@Param("offerId") UUID offerId);

    long countBySellerIdAndStatus(UUID sellerId, OfferStatus status);

    long countByBuyerIdAndStatus(UUID buyerId, OfferStatus status);

    boolean existsByBuyerIdAndListingIdAndStatus(UUID buyerId, UUID listingId, OfferStatus status);

    @Query("SELECT o.id FROM Offer o WHERE o.status = 'PENDING' AND o.expiresAt < :now")
    List<UUID> findExpiredOfferIds(@Param("now") LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Offer o " +
            "LEFT JOIN FETCH o.listing l " +
            "LEFT JOIN FETCH o.buyer b " +
            "LEFT JOIN FETCH o.seller s " +
            "WHERE o.listing.id = :listingId AND o.status = :status " +
            "ORDER BY o.createdAt DESC")
    List<Offer> findByListingIdAndStatusWithPessimisticLock(
            @Param("listingId") UUID listingId,
            @Param("status") OfferStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Offer o " +
            "LEFT JOIN FETCH o.listing l " +
            "LEFT JOIN FETCH o.buyer b " +
            "LEFT JOIN FETCH o.seller s " +
            "WHERE o.id = :offerId")
    Optional<Offer> findByIdWithPessimisticLock(@Param("offerId") UUID offerId);

}
