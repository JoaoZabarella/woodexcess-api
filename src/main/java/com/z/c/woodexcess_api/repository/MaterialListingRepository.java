package com.z.c.woodexcess_api.repository;

import com.z.c.woodexcess_api.enums.ListingStatus;
import com.z.c.woodexcess_api.model.MaterialListing;
import com.z.c.woodexcess_api.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MaterialListingRepository extends JpaRepository<MaterialListing, UUID> {

        List<MaterialListing> findByOwnerAndStatus(User owner, ListingStatus status);

        List<MaterialListing> findByOwnerAndStatusOrderByCreatedAtDesc(User owner, ListingStatus status);

        Optional<MaterialListing> findByIdAndStatus(UUID id, ListingStatus status);

        long countByOwnerAndStatus(User owner, ListingStatus status);

        Page<MaterialListing> findByStatus(ListingStatus status, Pageable pageable);

        @Query("SELECT l FROM MaterialListing l WHERE " +
                        "(:status IS NULL OR l.status = :status) AND " +
                        "(:materialType IS NULL OR l.materialType = :materialType) AND " +
                        "(:city IS NULL OR LOWER(l.city) LIKE LOWER(CONCAT('%', :city, '%'))) AND " +
                        "(:state IS NULL OR UPPER(l.state) = UPPER(:state)) AND " +
                        "(:minPrice IS NULL OR l.price >= :minPrice) AND " +
                        "(:maxPrice IS NULL OR l.price <= :maxPrice) AND " +
                        "(:condition IS NULL OR l.condition = :condition)")
        Page<MaterialListing> findByFilters(
                        @Param("status") String status,
                        @Param("materialType") String materialType,
                        @Param("city") String city,
                        @Param("state") String state,
                        @Param("minPrice") java.math.BigDecimal minPrice,
                        @Param("maxPrice") java.math.BigDecimal maxPrice,
                        @Param("condition") String condition,
                        Pageable pageable);
}
