package com.z.c.woodexcess_api.service.address;

import com.z.c.woodexcess_api.dto.address.AddressFromCepRequest;
import com.z.c.woodexcess_api.dto.address.AddressRequest;
import com.z.c.woodexcess_api.dto.address.AddressResponse;
import com.z.c.woodexcess_api.dto.address.ViaCepResponse;
import com.z.c.woodexcess_api.exception.BusinessException;
import com.z.c.woodexcess_api.exception.address.AddressNotFoundException;
import com.z.c.woodexcess_api.mapper.AddressMapper;
import com.z.c.woodexcess_api.model.Address;
import com.z.c.woodexcess_api.model.User;
import com.z.c.woodexcess_api.repository.AddressRepository;
import com.z.c.woodexcess_api.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddressService {

    private static final int MAX_ADDRESSES_PER_USER = 10;

    private final AddressRepository addressRepository;
    private final UserService userService;
    private final AddressMapper addressMapper;
    private final CepService cepService;

    @Transactional
    public AddressResponse create(UUID userId, AddressRequest request) {
        log.info("Creating address for user {}", userId);

        User user = userService.findEntityById(userId);
        validateMaxAddresses(userId);
        validateDuplicateAddress(userId, request.getZipCode(), request.getNumber());

        Address address = addressMapper.toEntity(request, user);

        if (Boolean.TRUE.equals(request.getIsPrimary())) {
            removePrimaryFromAllAddresses(userId);
        }

        Address saved = addressRepository.save(address);
        log.info("Address {} created successfully", saved.getId());

        return addressMapper.toResponseDTO(saved);
    }

    @Transactional
    public AddressResponse createFromCep(UUID userId, AddressFromCepRequest request) {
        log.info("Creating address from CEP {} for user {}", request.getZipCode(), userId);

        User user = userService.findEntityById(userId);
        validateMaxAddresses(userId);
        validateDuplicateAddress(userId, request.getZipCode(), request.getNumber());

        ViaCepResponse viaCepData = cepService.findAddressByCep(request.getZipCode());

        Address address = addressMapper.toEntityFromCep(viaCepData, request, user);

        if (Boolean.TRUE.equals(request.getIsPrimary())) {
            removePrimaryFromAllAddresses(userId);
        }

        Address saved = addressRepository.save(address);
        log.info("Address {} created from CEP successfully", saved.getId());

        return addressMapper.toResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public AddressResponse findById(UUID id) {
        Address address = findEntityById(id);
        return addressMapper.toResponseDTO(address);
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> findAllByUser(UUID userId) {
        log.info("Finding all addresses for user {}", userId);
        userService.findEntityById(userId);

        return addressRepository.findByUserIdAndActiveTrue(userId)
                .stream()
                .map(addressMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AddressResponse findPrimaryByUser(UUID userId) {
        log.info("Finding primary address for user {}", userId);

        return addressRepository.findByUserIdAndIsPrimaryTrueAndActiveTrue(userId)
                .map(addressMapper::toResponseDTO)
                .orElseThrow(() -> new AddressNotFoundException("Primary address not found for user: " + userId));
    }

    @Transactional
    public AddressResponse update(UUID id, AddressRequest request) {
        log.info("Updating address {}", id);

        Address address = findEntityById(id);

        if (!address.getZipCode().equals(formatZipCode(request.getZipCode())) ||
                !address.getNumber().equals(request.getNumber())) {
            validateDuplicateAddress(address.getUser().getId(), request.getZipCode(), request.getNumber());
        }

        addressMapper.updateEntity(address, request);
        Address updated = addressRepository.save(address);

        log.info("Address {} updated successfully", id);
        return addressMapper.toResponseDTO(updated);
    }

    @Transactional
    public AddressResponse setPrimary(UUID id) {
        log.info("Setting address {} as primary", id);

        Address address = findEntityById(id);
        removePrimaryFromAllAddresses(address.getUser().getId());

        address.setPrimary(true);
        Address updated = addressRepository.save(address);

        log.info("Address {} set as primary", id);
        return addressMapper.toResponseDTO(updated);
    }

    @Transactional
    public void delete(UUID id) {
        log.info("Deleting address {}", id);

        Address address = findEntityById(id);
        address.desactivate();
        addressRepository.save(address);

        log.info("Address {} deleted successfully", id);
    }

    @Transactional(readOnly = true)
    public Address findEntityById(UUID id) {
        return addressRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new AddressNotFoundException("Address not found with id: " + id));
    }

    private void validateMaxAddresses(UUID userId) {
        long count = addressRepository.countActiveAddressesByUser(userId);
        if (count >= MAX_ADDRESSES_PER_USER) {
            throw new BusinessException("Maximum number of addresses reached (" + MAX_ADDRESSES_PER_USER + ")");
        }
    }

    private void validateDuplicateAddress(UUID userId, String zipCode, String number) {
        String formatted = formatZipCode(zipCode);
        addressRepository.findDuplicateAddress(userId, formatted, number)
                .ifPresent(addr -> {
                    throw new BusinessException("Address already exists for this user");
                });
    }

    private void removePrimaryFromAllAddresses(UUID userId) {
        addressRepository.removePrimaryFromAllUserAddresses(userId);
    }

    private String formatZipCode(String zipCode) {
        String cleaned = zipCode.replaceAll("[^0-9]", "");
        if (cleaned.length() == 8) {
            return cleaned.substring(0, 5) + "-" + cleaned.substring(5);
        }
        return zipCode;
    }
}
