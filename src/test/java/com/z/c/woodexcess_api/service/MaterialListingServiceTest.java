package com.z.c.woodexcess_api.service;

import com.z.c.woodexcess_api.dto.listing.CreateListingRequest;
import com.z.c.woodexcess_api.dto.listing.ListingFilterRequest;
import com.z.c.woodexcess_api.dto.listing.ListingResponse;
import com.z.c.woodexcess_api.dto.listing.UpdateListingRequest;
import com.z.c.woodexcess_api.model.enums.Condition;
import com.z.c.woodexcess_api.model.enums.ListingStatus;
import com.z.c.woodexcess_api.model.enums.MaterialType;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaterialListingServiceTest {

    @Mock
    private MaterialListingRepository listingRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private MaterialListingMapper mapper;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MaterialListingService service;

    private User activeUser;
    private User inactiveUser;
    private User adminUser;
    private Address address;
    private MaterialListing listing;
    private CreateListingRequest createRequest;
    private UpdateListingRequest updateRequest;
    private ListingResponse listingResponse;

    @BeforeEach
    void setUp() {

        activeUser = User.builder()
                .id(UUID.randomUUID())
                .name("John Doe")
                .email("john@example.com")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        inactiveUser = User.builder()
                .id(UUID.randomUUID())
                .name("Inactive User")
                .email("inactive@example.com")
                .role(UserRole.USER)
                .isActive(false)
                .build();

        adminUser = User.builder()
                .id(UUID.randomUUID())
                .name("Admin User")
                .email("admin@example.com")
                .role(UserRole.ADMIN)
                .isActive(true)
                .build();

        address = Address.builder()
                .id(UUID.randomUUID())
                .user(activeUser)
                .street("Rua Teste")
                .number("123")
                .city("São Paulo")
                .state("SP")
                .zipCode("01310-100")
                .country("Brasil")
                .isActive(true)
                .isPrimary(true)
                .build();

        listing = MaterialListing.builder()
                .id(UUID.randomUUID())
                .owner(activeUser)
                .title("Sobra de Madeira")
                .description("Madeira em bom estado")
                .materialType(MaterialType.WOOD)
                .price(new BigDecimal("150.00"))
                .quantity(10)
                .condition(Condition.USED)
                .address(address)
                .city("São Paulo")
                .state("SP")
                .status(ListingStatus.ACTIVE)
                .build();

        createRequest = new CreateListingRequest(
                "Sobra de Madeira",
                "Madeira em bom estado",
                MaterialType.WOOD,
                new BigDecimal("150.00"),
                10,
                Condition.USED,
                address.getId());

        updateRequest = new UpdateListingRequest(
                "Título Atualizado",
                "Descrição atualizada",
                MaterialType.MDF,
                new BigDecimal("200.00"),
                5,
                Condition.NEW,
                null);

        listingResponse = ListingResponse.builder()
                .id(listing.getId())
                .title(listing.getTitle())
                .description(listing.getDescription())
                .materialType(listing.getMaterialType())
                .price(listing.getPrice())
                .quantity(listing.getQuantity())
                .condition(listing.getCondition())
                .status(listing.getStatus())
                .city(listing.getCity())
                .state(listing.getState())
                .build();
    }

    @Test
    @DisplayName("Should use primary address when no address provided")
    void shouldUsePrimaryAddressWhenNoAddressProvided() {
        CreateListingRequest requestWithoutAddress = new CreateListingRequest(
                "Sobra de Madeira",
                "Madeira em bom estado",
                MaterialType.WOOD,
                new BigDecimal("150.00"),
                10,
                Condition.USED,
                null);

        when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));
        when(addressRepository.findByUserAndIsActiveAndIsPrimary(activeUser, true, true))
                .thenReturn(Optional.of(address));
        when(mapper.toEntity(requestWithoutAddress, activeUser, address)).thenReturn(listing);
        when(listingRepository.save(listing)).thenReturn(listing);
        when(mapper.toResponse(listing)).thenReturn(listingResponse);

        ListingResponse result = service.createListing(requestWithoutAddress, activeUser.getId());

        assertThat(result).isNotNull();
        verify(userRepository).findById(activeUser.getId());
        verify(addressRepository).findByUserAndIsActiveAndIsPrimary(activeUser, true, true);
        verify(listingRepository).save(listing);
    }

    @Test
    @DisplayName("Should update listing successfully by owner")
    void shouldUpdateListingSuccessfullyByOwner() {
        when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));
        when(listingRepository.save(listing)).thenReturn(listing);
        when(mapper.toResponse(listing)).thenReturn(listingResponse);

        ListingResponse result = service.updateListing(listing.getId(), updateRequest, activeUser.getId());

        assertThat(result).isNotNull();
        verify(userRepository).findById(activeUser.getId());
        verify(listingRepository).findById(listing.getId());
        verify(listingRepository).save(listing);
    }

    @Test
    @DisplayName("Should update listing successfully by admin")
    void shouldUpdateListingSuccessfullyByAdmin() {
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));
        when(listingRepository.save(listing)).thenReturn(listing);
        when(mapper.toResponse(listing)).thenReturn(listingResponse);

        ListingResponse result = service.updateListing(listing.getId(), updateRequest, adminUser.getId());

        assertThat(result).isNotNull();
        verify(userRepository).findById(adminUser.getId());
        verify(listingRepository).save(listing);
    }

    @Test
    @DisplayName("Should throw exception when non-owner tries to update listing")
    void shouldThrowExceptionWhenNonOwnerTriesToUpdate() {
        User anotherUser = User.builder()
                .id(UUID.randomUUID())
                .name("Another User")
                .email("another@example.com")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        when(userRepository.findById(anotherUser.getId())).thenReturn(Optional.of(anotherUser));
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> service.updateListing(listing.getId(), updateRequest, anotherUser.getId()))
                .isInstanceOf(UnauthorizedListingAccessException.class);

        verify(userRepository).findById(anotherUser.getId());
        verify(listingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should deactivate listing successfully by owner")
    void shouldDeactivateListingSuccessfullyByOwner() {
        when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));
        when(listingRepository.save(listing)).thenReturn(listing);

        service.deactivateListing(listing.getId(), activeUser.getId());

        verify(userRepository).findById(activeUser.getId());
        verify(listingRepository).findById(listing.getId());
        verify(listingRepository).save(listing);
        assertThat(listing.getStatus()).isEqualTo(ListingStatus.INACTIVE);
    }

    @Test
    @DisplayName("Should deactivate listing successfully by admin")
    void shouldDeactivateListingSuccessfullyByAdmin() {
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));
        when(listingRepository.save(listing)).thenReturn(listing);

        service.deactivateListing(listing.getId(), adminUser.getId());

        verify(userRepository).findById(adminUser.getId());
        verify(listingRepository).save(listing);
        assertThat(listing.getStatus()).isEqualTo(ListingStatus.INACTIVE);
    }

    @Test
    @DisplayName("Should throw exception when non-owner tries to deactivate listing")
    void shouldThrowExceptionWhenNonOwnerTriesToDeactivate() {
        User anotherUser = User.builder()
                .id(UUID.randomUUID())
                .name("Another User")
                .email("another@example.com")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        when(userRepository.findById(anotherUser.getId())).thenReturn(Optional.of(anotherUser));
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> service.deactivateListing(listing.getId(), anotherUser.getId()))
                .isInstanceOf(UnauthorizedListingAccessException.class);

        verify(userRepository).findById(anotherUser.getId());
        verify(listingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get listing by ID successfully")
    void shouldGetListingByIdSuccessfully() {
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));
        when(mapper.toResponse(listing)).thenReturn(listingResponse);

        ListingResponse result = service.getListingById(listing.getId());

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(listing.getId());
        verify(listingRepository).findById(listing.getId());
    }

    @Test
    @DisplayName("Should throw exception when listing not found")
    void shouldThrowExceptionWhenListingNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        when(listingRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getListingById(nonExistentId))
                .isInstanceOf(ListingNotFoundException.class);
    }

    @Test
    @DisplayName("Should get all listings with filters successfully")
    void shouldGetAllListingsWithFiltersSuccessfully() {
        ListingFilterRequest filters = ListingFilterRequest.builder()
                .materialType(MaterialType.WOOD)
                .city("São Paulo")
                .minPrice(new BigDecimal("100.00"))
                .maxPrice(new BigDecimal("200.00"))
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<MaterialListing> listingPage = new PageImpl<>(List.of(listing));

        when(listingRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(listingPage);
        when(mapper.toResponse(listing)).thenReturn(listingResponse);

        Page<ListingResponse> result = service.getAllListings(filters, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(listing.getId());
        verify(listingRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("Should default to ACTIVE status when no status provided")
    void shouldDefaultToActiveStatusWhenNoStatusProvided() {
        ListingFilterRequest filters = ListingFilterRequest.builder().build();
        Pageable pageable = PageRequest.of(0, 10);
        Page<MaterialListing> listingPage = new PageImpl<>(List.of(listing));

        when(listingRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(listingPage);
        when(mapper.toResponse(listing)).thenReturn(listingResponse);

        Page<ListingResponse> result = service.getAllListings(filters, pageable);

        assertThat(result).isNotNull();
        verify(listingRepository).findAll(any(Specification.class), eq(pageable));
    }
}
