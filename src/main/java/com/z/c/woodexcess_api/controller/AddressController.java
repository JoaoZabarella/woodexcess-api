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

@RestController
@Slf4j
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    //Manual
    @PostMapping
    public ResponseEntity<AddressResponse> create(
            @Valid @RequestBody AddressRequest request,
            @AuthenticationPrincipal CustomUserDetails details
    ) {
        log.info("POST /api/addresses - User: {} and  ID: {}", details.getUsername(), details.getId());
        AddressResponse response = addressService.create(details.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    //Endereço a partir do VIACEP
    @PostMapping("from/cep")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AddressResponse> createFromCep(
            @Valid @RequestBody AddressFromCepRequest request,
            @AuthenticationPrincipal CustomUserDetails details
    ) {
        log.info("POST /api/addresses/from-cep - CEP: {}  User: {} and  ID: {}", request.getZipCode(), details.getUsername(), details.getId());
        AddressResponse response = addressService.createFromCep(details.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AddressResponse> findById(@PathVariable UUID id) {
        log.info("GET /api/addresses - ID: {}", id);
        AddressResponse response = addressService.findById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AddressResponse>> findAll(@AuthenticationPrincipal CustomUserDetails details) {
        log.info("GET /api/addresses - User: {} and  ID: {}", details.getUsername(), details.getId());
        List<AddressResponse> addresses = addressService.findAllByUser(details.getId());
        return ResponseEntity.ok(addresses);
    }

    @GetMapping("/primary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AddressResponse> findByPrimary(@AuthenticationPrincipal CustomUserDetails details) {
        log.info("GET /api/addresses/primary - User: {} and  ID: {}", details.getUsername(), details.getId());
        AddressResponse response = addressService.findPrimaryByUser(details.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AddressResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody AddressRequest request
    ){
        log.info("PUT /api/addresses/{} - User: {} and  ID: {}", request.getZipCode(), id, id);

        AddressResponse response = addressService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/set-primary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AddressResponse> setPrimary(@PathVariable UUID id, CustomUserDetails details){
        log.info("PATCH /api/addresses/{} - User: {}", id, details.getUsername());

        AddressResponse response = addressService.setPrimary(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(@PathVariable UUID id, CustomUserDetails details){
        log.info("DELETE /api/addresses/{} - User: {}", id, details.getUsername());

        addressService.delete(id);
        return ResponseEntity.noContent().build();

    }


}
