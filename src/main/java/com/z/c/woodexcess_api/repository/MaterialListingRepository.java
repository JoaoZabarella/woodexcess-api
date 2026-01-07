package com.z.c.woodexcess_api.repository;

import com.z.c.woodexcess_api.model.enums.ListingStatus;
import com.z.c.woodexcess_api.model.MaterialListing;
import com.z.c.woodexcess_api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MaterialListingRepository extends
        JpaRepository<MaterialListing, UUID>,
        JpaSpecificationExecutor<MaterialListing> {

    List<MaterialListing> findByOwnerAndStatus(User owner, ListingStatus status);
    List<MaterialListing> findByOwnerAndStatusOrderByCreatedAtDesc(User owner, ListingStatus status);
}
