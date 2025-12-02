package com.z.c.woodexcess_api.controller;

import com.z.c.woodexcess_api.dto.address.AddressFromCepRequest;
import com.z.c.woodexcess_api.dto.address.AddressRequest;
import com.z.c.woodexcess_api.dto.address.AddressResponse;
import com.z.c.woodexcess_api.security.CustomUserDetails;
import com.z.c.woodexcess_api.service.address.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AddressResponse> create(
            @Valid @RequestBody AddressRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("Creating address for user '{}', payload: {}", userDetails.getUsername(), request);
        AddressResponse response = addressService.create(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/from-cep")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AddressResponse> createFromCep(
            @Valid @RequestBody AddressFromCepRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("POST /api/addresses/from-cep - CEP: {} - User: {}", request.zipCode(), userDetails.getUsername());
        AddressResponse response = addressService.createFromCep(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AddressResponse> findById(@PathVariable UUID id) {
        log.info("GET ID /api/addresses/{}", id);
        AddressResponse response = addressService.findById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AddressResponse>> findAll(@AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("GET ALL /api/addresses - User: {}", userDetails.getUsername());
        List<AddressResponse> addresses = addressService.findAllByUser(userDetails.getId());
        return ResponseEntity.ok(addresses);
    }

    @GetMapping("/primary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AddressResponse> findPrimary(@AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("GET /api/addresses/primary - User: {}", userDetails.getUsername());
        AddressResponse response = addressService.findPrimaryByUser(userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AddressResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody AddressRequest request) {
        log.info("Updating address - ID: {}, Payload: {}", id, request);
        AddressResponse response = addressService.update(id, request);
        return ResponseEntity.ok(response);

    }

    @PatchMapping("/{id}/set-primary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AddressResponse> setPrimary(@PathVariable UUID id) {
        log.info("PATCH /api/addresses/{}/set-primary", id);
        AddressResponse response = addressService.setPrimary(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        log.info("Deleting address - ID: {}", id);
        addressService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
