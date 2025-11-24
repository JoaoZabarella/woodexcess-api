package com.z.c.woodexcess_api.controller;

import com.z.c.woodexcess_api.dto.listing.CreateListingRequest;
import com.z.c.woodexcess_api.dto.listing.ListingFilterRequest;
import com.z.c.woodexcess_api.dto.listing.ListingResponse;
import com.z.c.woodexcess_api.dto.listing.UpdateListingRequest;
import com.z.c.woodexcess_api.model.User;
import com.z.c.woodexcess_api.service.MaterialListingService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/listings")
@Slf4j
public class MaterialListingController extends BaseController {

    private final MaterialListingService listingService;

    public MaterialListingController(MaterialListingService listingService) {
        this.listingService = listingService;
    }

    @PostMapping
    public ResponseEntity<ListingResponse> createListing(
            @Valid @RequestBody CreateListingRequest request,
            @AuthenticationPrincipal User currentUser) {
        log.info("POST /api/listings - Creating listing for user: {}", currentUser.getEmail());

        ListingResponse response = listingService.createListing(request, currentUser);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<ListingResponse>> getAllListings(
            @ModelAttribute ListingFilterRequest filters,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.debug("GET /api/listings - Filters: {}, Page: {}", filters, pageable);

        Page<ListingResponse> listings = listingService.getAllListings(filters, pageable);

        return ResponseEntity.ok(listings);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ListingResponse> getListingById(@PathVariable UUID id) {
        log.debug("GET /api/listings/{} - Fetching listing", id);

        ListingResponse listing = listingService.getListingById(id);

        return ResponseEntity.ok(listing);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ListingResponse> updateListing(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateListingRequest request,
            @AuthenticationPrincipal User currentUser) {
        log.info("PUT /api/listings/{} - Updating listing for user: {}", id, currentUser.getEmail());

        ListingResponse response = listingService.updateListing(id, request, currentUser);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateListing(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        log.info("PATCH /api/listings/{}/deactivate - Deactivating listing for user: {}", id, currentUser.getEmail());

        listingService.deactivateListing(id, currentUser);

        return ResponseEntity.noContent().build();
    }
}
