package com.z.c.woodexcess_api.repository;

import com.z.c.woodexcess_api.model.MaterialListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ListingRepository extends JpaRepository<MaterialListing, UUID> {
}
