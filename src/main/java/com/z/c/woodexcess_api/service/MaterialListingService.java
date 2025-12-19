package com.z.c.woodexcess_api.service;

import com.z.c.woodexcess_api.dto.listing.CreateListingRequest;
import com.z.c.woodexcess_api.dto.listing.ListingFilterRequest;
import com.z.c.woodexcess_api.dto.listing.ListingResponse;
import com.z.c.woodexcess_api.dto.listing.UpdateListingRequest;
import com.z.c.woodexcess_api.exception.ResourceNotFoundException;
import com.z.c.woodexcess_api.model.enums.ListingStatus;
import com.z.c.woodexcess_api.model.enums.UserRole;
import com.z.c.woodexcess_api.exception.BusinessException;
import com.z.c.woodexcess_api.exception.address.AddressNotFoundException;
import com.z.c.woodexcess_api.exception.listing.ListingNotFoundException;
import com.z.c.woodexcess_api.exception.listing.UnauthorizedListingAccessException;
import com.z.c.woodexcess_api.mapper.MaterialListingMapper;
import com.z.c.woodexcess_api.model.Address;
import com.z.c.woodexcess_api.model.MaterialListing;
import com.z.c.woodexcess_api.model.User;
import com.z.c.woodexcess_api.repository.AddressRepository;
import com.z.c.woodexcess_api.repository.MaterialListingRepository;
import com.z.c.woodexcess_api.repository.UserRepository;
import com.z.c.woodexcess_api.specification.MaterialListingSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class MaterialListingService {

    private final MaterialListingRepository listingRepository;
    private final AddressRepository addressRepository;
    private final MaterialListingMapper mapper;
    private final UserRepository userRepository;

    @Transactional
    public ListingResponse createListing(CreateListingRequest request, UUID ownerId) {
        log.info("Creating listing for user: {}", ownerId);

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + ownerId));

        if (!owner.getIsActive()) {
            throw new BusinessException("Cannot create listing: user account is inactive");
        }

        Address address = null;
        if (request.addressId() != null) {
            address = addressRepository.findById(request.addressId())
                    .orElseThrow(() -> new AddressNotFoundException(request.addressId()));

            if (!address.getUser().getId().equals(owner.getId())) {
                throw new UnauthorizedListingAccessException("Address does not belong to current user");
            }

            if (!address.isActive()) {
                throw new BusinessException("Cannot use inactive address for listing");
            }
        }

        if (address == null) {
            address = addressRepository.findByUserAndIsActiveAndIsPrimary(owner, true, true)
                    .orElseThrow(() -> new BusinessException(
                            "User must have at least one active address to create a listing"));
        }

        MaterialListing listing = mapper.toEntity(request, owner, address);

        MaterialListing savedListing = listingRepository.save(listing);

        log.info("Listing created successfully with id: {}", savedListing.getId());
        return mapper.toResponse(savedListing);
    }

    @Transactional
    public ListingResponse updateListing(UUID listingId, UpdateListingRequest request, UUID ownerId) {
        log.info("Updating listing {} by user: {}", listingId, ownerId);

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + ownerId));

        MaterialListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ListingNotFoundException(listingId));

        validateOwnershipOrAdmin(listing, owner);

        if (request.title() != null) {
            listing.setTitle(request.title());
        }
        if (request.description() != null) {
            listing.setDescription(request.description());
        }
        if (request.materialType() != null) {
            listing.setMaterialType(request.materialType());
        }
        if (request.price() != null) {
            listing.setPrice(request.price());
        }
        if (request.quantity() != null) {
            listing.setQuantity(request.quantity());
        }
        if (request.condition() != null) {
            listing.setCondition(request.condition());
        }
        if (request.addressId() != null) {
            Address newAddress = addressRepository.findById(request.addressId())
                    .orElseThrow(() -> new AddressNotFoundException(request.addressId()));

            if (!newAddress.getUser().getId().equals(listing.getOwner().getId())) {
                throw new UnauthorizedListingAccessException("Address does not belong to listing owner");
            }

            listing.setAddress(newAddress);
            listing.setCity(newAddress.getCity());
            listing.setState(newAddress.getState());
        }

        MaterialListing updatedListing = listingRepository.save(listing);

        log.info("Listing {} updated successfully", listingId);
        return mapper.toResponse(updatedListing);
    }

    @Transactional
    public void deactivateListing(UUID listingId, UUID ownerId) {
        log.info("Deactivating listing {} by user: {}", listingId, ownerId);

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + ownerId));

        MaterialListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ListingNotFoundException(listingId));

        validateOwnershipOrAdmin(listing, owner);

        listing.deactivate();
        listingRepository.save(listing);

        log.info("Listing {} deactivated successfully", listingId);
    }

    @Transactional(readOnly = true)
    public ListingResponse getListingById(UUID listingId) {
        log.debug("Fetching listing by id: {}", listingId);

        MaterialListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ListingNotFoundException(listingId));

        return mapper.toResponse(listing);
    }

    @Transactional(readOnly = true)
    public Page<ListingResponse> getAllListings(ListingFilterRequest filters, Pageable pageable) {
        log.debug("Fetching listings with filters: {}", filters);

        Specification<MaterialListing> spec = MaterialListingSpecification.withFilters(
                filters.status() != null ? filters.status() : ListingStatus.ACTIVE,
                filters.materialType(),
                filters.city(),
                filters.state(),
                filters.minPrice(),
                filters.maxPrice(),
                filters.condition());

        Page<MaterialListing> listings = listingRepository.findAll(spec, pageable);
        return listings.map(mapper::toResponse);
    }

    private void validateOwnershipOrAdmin(MaterialListing listing, User owner) {
        boolean isOwner = listing.isOwnedBy(owner.getId());
        boolean isAdmin = owner.getRole() == UserRole.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new UnauthorizedListingAccessException();
        }
    }
}
