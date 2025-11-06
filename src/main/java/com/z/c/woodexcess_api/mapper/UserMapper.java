package com.z.c.woodexcess_api.mapper;

import com.z.c.woodexcess_api.dto.UserResponseDTO;
import com.z.c.woodexcess_api.model.User;

public class UserMapper {
    public static UserResponseDTO toResponse(User user){
        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }
}
