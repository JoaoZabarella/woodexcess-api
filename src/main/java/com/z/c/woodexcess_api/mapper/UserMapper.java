package com.z.c.woodexcess_api.mapper;

import com.z.c.woodexcess_api.dto.auth.RegisterRequest;
import com.z.c.woodexcess_api.dto.auth.RegisterResponse;
import com.z.c.woodexcess_api.model.User;
import com.z.c.woodexcess_api.role.UserRole;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public User toEntity(RegisterRequest dto) {
        return new User(
                null,
                dto.name(),
                dto.email(),
                null,
                UserRole.USER
        );
    }

    public RegisterResponse toResponse(User user) {
        return new RegisterResponse(
                user.getId().toString(),
                user.getName(),
                user.getEmail(),
                user.getRole().toString()
        );
    }
}

