package com.z.c.woodexcess_api.controller;

import com.z.c.woodexcess_api.dto.auth.LoginRequest;
import com.z.c.woodexcess_api.dto.auth.LoginResponse;
import com.z.c.woodexcess_api.dto.auth.RefreshTokenRequest;
import com.z.c.woodexcess_api.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService service;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        var response = service.authenticate(request.email(), request.password(), httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        var response = service.refreshAccessToken(request.refreshToken(), httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        service.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

}
