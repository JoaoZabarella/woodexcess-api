package com.z.c.woodexcess_api.repository;

import com.z.c.woodexcess_api.model.ListingImage;
import com.z.c.woodexcess_api.model.MaterialListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ListingImageRepository extends JpaRepository<ListingImage, UUID> {

    List<ListingImage> findByListingOrderByDisplayOrderAsc(MaterialListing listing);

    List<ListingImage> findByListingIdOrderByDisplayOrderAsc(UUID listingId);

    Optional<ListingImage> findByListingAndIsPrimaryTrue(MaterialListing listing);

    List<ListingImage> findAllByListingAndIsPrimaryTrue(MaterialListing listing);

    @Modifying
    @Query("UPDATE ListingImage i SET i.isPrimary = false WHERE i.listing = :listing AND i.isPrimary = true")
    int removeAllPrimaryFlags(@Param("listing") MaterialListing listing);

    long countByListing(MaterialListing listing);

    @Query("SELECT MAX(i.displayOrder) FROM ListingImage i WHERE i.listing = :listing")
    Optional<Integer> findMaxDisplayOrderByListing(@Param("listing") MaterialListing listing);

    List<ListingImage> findByFileExtension(String extension);

    boolean existsByListingAndIsPrimaryTrue(MaterialListing listing);

}
