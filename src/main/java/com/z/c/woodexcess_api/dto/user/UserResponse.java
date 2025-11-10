package com.z.c.woodexcess_api.dto.user;

import com.z.c.woodexcess_api.model.User;
import com.z.c.woodexcess_api.role.UserRole;

import java.util.UUID;

public record UserResponse (
        UUID id,
        String name,
        String email,
        UserRole role
){
    public UserResponse(User user){
        this(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }
}
