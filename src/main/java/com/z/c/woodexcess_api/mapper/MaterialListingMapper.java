package com.z.c.woodexcess_api.mapper;

import com.z.c.woodexcess_api.dto.listing.CreateListingRequest;
import com.z.c.woodexcess_api.dto.listing.ListingOwerResponse;
import com.z.c.woodexcess_api.dto.listing.ListingResponse;
import com.z.c.woodexcess_api.enums.ListingStatus;
import com.z.c.woodexcess_api.model.Address;
import com.z.c.woodexcess_api.model.MaterialListing;
import com.z.c.woodexcess_api.model.User;
import org.springframework.stereotype.Component;

@Component
public class MaterialListingMapper {

    private final AddressMapper addressMapper;

    public MaterialListingMapper(AddressMapper addressMapper) {
        this.addressMapper = addressMapper;
    }

    public MaterialListing toEntity(CreateListingRequest request, User owner, Address address) {
        MaterialListing listing = MaterialListing.builder()
                .title(request.title())
                .description(request.description())
                .materialType(request.materialType())
                .price(request.price())
                .quantity(request.quantity())
                .condition(request.condition())
                .owner(owner)
                .address(address)
                .city(address.getCity())
                .state(address.getState())
                .status(ListingStatus.ACTIVE)
                .build();

        return listing;
    }

    public ListingResponse toResponse(MaterialListing listing) {
        String city = listing.getCity();
        String state = listing.getState();
        if (city == null && listing.getAddress() != null) {
            city = listing.getAddress().getCity();
        }
        if (state == null && listing.getAddress() != null) {
            state = listing.getAddress().getState();
        }

        return ListingResponse.builder()
                .id(listing.getId())
                .title(listing.getTitle())
                .description(listing.getDescription())
                .materialType(listing.getMaterialType())
                .price(listing.getPrice())
                .quantity(listing.getQuantity())
                .condition(listing.getCondition())
                .city(city)
                .state(state)
                .status(listing.getStatus())
                .createdAt(listing.getCreatedAt())
                .updatedAt(listing.getUpdatedAt())
                .owner(toOwerResponse(listing.getOwner()))
                .address(listing.getAddress() != null ? addressMapper.toResponseDTO(listing.getAddress()) : null)
                .build();
    }

    private ListingOwerResponse toOwerResponse(User ower) {
        if(ower == null) return null;

        return ListingOwerResponse.builder()
                .id(ower.getId())
                .name(ower.getName())
                .email(ower.getEmail())
                .phone(ower.getPhone())
                .build();

    }
}
