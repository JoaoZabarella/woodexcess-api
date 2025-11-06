package com.z.c.woodexcess_api.controller;

import com.z.c.woodexcess_api.dto.UserRegisterDTO;
import com.z.c.woodexcess_api.dto.UserResponseDTO;
import com.z.c.woodexcess_api.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService service;

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> toResponse(@Valid @RequestBody UserRegisterDTO user) throws IllegalAccessException {
        UserResponseDTO response = service.registerUser(user);
        return ResponseEntity.ok(response);
    }
}
