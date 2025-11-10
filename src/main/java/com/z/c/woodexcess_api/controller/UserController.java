package com.z.c.woodexcess_api.controller;

import com.z.c.woodexcess_api.dto.auth.RegisterRequest;
import com.z.c.woodexcess_api.dto.auth.RegisterResponse;
import com.z.c.woodexcess_api.dto.user.ChangePasswordRequest;
import com.z.c.woodexcess_api.dto.user.UpdateUserRequest;
import com.z.c.woodexcess_api.dto.user.UserResponse;
import com.z.c.woodexcess_api.security.CustomUserDetails;
import com.z.c.woodexcess_api.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users")
public class UserController extends BaseController {

    private final UserService service;

    @Autowired
    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest user) throws IllegalAccessException {
        var response = service.registerUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal CustomUserDetails details) throws IllegalAccessException {
        var response = service.getUserByID(details.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMe(@AuthenticationPrincipal CustomUserDetails details, @Valid UpdateUserRequest request){
        var response = service.updateUser(details.getId(), request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal CustomUserDetails details, @Valid ChangePasswordRequest request){
        service.changePassword(details.getId(), request);
        return ResponseEntity.noContent().build();
    }
}
