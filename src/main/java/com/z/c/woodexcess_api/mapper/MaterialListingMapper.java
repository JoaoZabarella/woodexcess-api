package com.z.c.woodexcess_api.mapper;

import com.z.c.woodexcess_api.dto.listing.CreateListingRequest;
import com.z.c.woodexcess_api.dto.listing.ListingResponse;
import com.z.c.woodexcess_api.enums.ListingStatus;
import com.z.c.woodexcess_api.model.Address;
import com.z.c.woodexcess_api.model.MaterialListing;
import com.z.c.woodexcess_api.model.User;
import org.springframework.stereotype.Component;

@Component
public class MaterialListingMapper {

    private final UserMapper userMapper;
    private final AddressMapper addressMapper;

    public MaterialListingMapper(UserMapper userMapper, AddressMapper addressMapper) {
        this.userMapper = userMapper;
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
                .status(ListingStatus.ACTIVE)
                .build();

        if (address != null) {
            listing.setCity(address.getCity());
            listing.setState(address.getState());
        }

        return listing;
    }

    public ListingResponse toResponse(MaterialListing listing) {
        return ListingResponse.builder()
                .id(listing.getId())
                .title(listing.getTitle())
                .description(listing.getDescription())
                .materialType(listing.getMaterialType())
                .price(listing.getPrice())
                .quantity(listing.getQuantity())
                .condition(listing.getCondition())
                .city(listing.getCity())
                .state(listing.getState())
                .status(listing.getStatus())
                .createdAt(listing.getCreatedAt())
                .updatedAt(listing.getUpdatedAt())
                .owner(userMapper.toUserResponse(listing.getOwner()))
                .address(listing.getAddress() != null ? addressMapper.toResponseDTO(listing.getAddress()) : null)
                .build();
    }
}
