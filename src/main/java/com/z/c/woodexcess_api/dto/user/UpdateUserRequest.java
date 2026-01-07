package com.z.c.woodexcess_api.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record UpdateUserRequest(
        @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
        String name,

        @Email
        String email,

        @Size(max = 20, message = "Phone cannot exceed 20 characters")
        String phone,

        @Size(max = 500, message = "Avatar URL cannot exceed 500 characters")
        String avatarUrl
) {}