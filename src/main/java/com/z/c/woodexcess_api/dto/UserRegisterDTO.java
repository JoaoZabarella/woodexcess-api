package com.z.c.woodexcess_api.dto;

import com.z.c.woodexcess_api.role.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRegisterDTO (

        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @Size(min = 6, message = "Password must be at least 6 chearacters")
        @NotBlank(message = "Password is required")
        String password
){
}
