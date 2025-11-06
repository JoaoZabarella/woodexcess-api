package com.z.c.woodexcess_api.controller;

import com.z.c.woodexcess_api.dto.auth.LoginRequest;
import com.z.c.woodexcess_api.dto.auth.LoginResponse;
import com.z.c.woodexcess_api.dto.auth.RegisterRequest;
import com.z.c.woodexcess_api.dto.auth.RegisterResponse;
import com.z.c.woodexcess_api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService service;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = service.authenticate(request.email(), request.password());
        return ResponseEntity.ok(new LoginResponse(token));
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return null;
    }

}
