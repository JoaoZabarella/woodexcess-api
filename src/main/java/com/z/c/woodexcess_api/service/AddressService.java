package com.z.c.woodexcess_api.service;

import com.z.c.woodexcess_api.dto.address.AddressRequest;
import com.z.c.woodexcess_api.dto.address.AddressResponse;
import com.z.c.woodexcess_api.exception.BusinessException;
import com.z.c.woodexcess_api.exception.ResourceNotFoundException;
import com.z.c.woodexcess_api.mapper.AddressMapper;
import com.z.c.woodexcess_api.model.Address;
import com.z.c.woodexcess_api.model.User;
import com.z.c.woodexcess_api.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressService {

    private static final int MAX_ADDRESSES_PER_USER = 10;

    private final AddressRepository addressRepository;
    private final UserService userService;
    private final AddressMapper addressMapper;

    @Transactional
    public AddressResponse create(UUID userId, AddressRequest request) {
        User user = userService.findEntityById(userId);

        validateMaxAddresses(userId);
        validateDuplicateAddress(userId, request.zipCode(), request.number());

        Address address = addressMapper.toEntity(request, user);
        Address saved = addressRepository.save(address);

        return addressMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AddressResponse findById(UUID id) {
        Address address = findEntityById(id);
        return addressMapper.toResponse(address);
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> findAllByUser(UUID userId) {
        userService.findEntityById(userId);

        return addressRepository.findByUserIdAndActiveTrue(userId)
                .stream()
                .map(addressMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AddressResponse update(UUID id, AddressRequest request) {
        Address address = findEntityById(id);

        if (!address.getZipCode().equals(formatZipCode(request.zipCode())) ||
                !address.getNumber().equals(request.number())) {
            validateDuplicateAddress(address.getUser().getId(), request.zipCode(), request.number());
        }

        addressMapper.updateEntity(address, request);
        Address updated = addressRepository.save(address);

        return addressMapper.toResponse(updated);
    }

    @Transactional
    public void delete(UUID id) {
        Address address = findEntityById(id);
        address.setActive(false);
        addressRepository.save(address);
    }

    @Transactional(readOnly = true)
    public Address findEntityById(UUID id) {
        return addressRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + id));
    }

    private void validateMaxAddresses(UUID userId) {
        long count = addressRepository.countActiveAddressesByUser(userId);
        if (count >= MAX_ADDRESSES_PER_USER) {
            throw new BusinessException("Maximum number of addresses reached (" + MAX_ADDRESSES_PER_USER + ")");
        }
    }

    private void validateDuplicateAddress(UUID userId, String zipCode, String number) {
        String formattedZipCode = formatZipCode(zipCode);
        addressRepository.findDuplicateAddress(userId, formattedZipCode, number)
                .ifPresent(addr -> {
                    throw new BusinessException("Address already exists for this user");
                });
    }

    private String formatZipCode(String zipCode) {
        String cleaned = zipCode.replaceAll("[^0-9]", "");
        if (cleaned.length() == 8) {
            return cleaned.substring(0, 5) + "-" + cleaned.substring(5);
        }
        return zipCode;
    }
}
