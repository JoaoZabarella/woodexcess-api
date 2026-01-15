package com.z.c.woodexcess_api.repository;

import com.z.c.woodexcess_api.model.Favorite;
import com.z.c.woodexcess_api.model.MaterialListing;
import com.z.c.woodexcess_api.model.User;
import com.z.c.woodexcess_api.model.enums.ListingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;
import java.util.stream.Collectors;

public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {

    boolean existsByUserAndListing(User user, MaterialListing listing);

    Optional<Favorite> findByUserAndListing(User user, MaterialListing listing);

    @Query("""
        SELECT f FROM Favorite f
        JOIN FETCH f.listing l 
        LEFT JOIN FETCH l.images img
        WHERE f.user = :user
        AND l.status = :status
        ORDER BY f.createdAt DESC
    """)
    Page<Favorite> findByUserWithDetails(
            @Param("user") User user,
            @Param("status") ListingStatus status,
            Pageable pageable
    );

    long countByUser(User user);

    long countByListing(MaterialListing listing);

    @Query("""
        SELECT f.listing.id as listingId, COUNT(f) as count
        FROM Favorite f
        WHERE f.listing.id IN :listingIds
        GROUP BY f.listing.id
    """)
    List<Object[]> countByListingIdsRaw(@Param("listingIds") Set<UUID> listingIds);

    default Map<UUID, Long> countByListingIds(Set<UUID> listingIds) {
        if (listingIds == null || listingIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return countByListingIdsRaw(listingIds).stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (Long) row[1]
                ));
    }
}
