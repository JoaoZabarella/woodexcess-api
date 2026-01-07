package com.z.c.woodexcess_api.dto.user;

import com.z.c.woodexcess_api.dto.address.AddressResponse;
import com.z.c.woodexcess_api.model.enums.UserRole;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record UserResponse(
        UUID id,
        String name,
        String email,
        String phone,
        String avatarUrl,
        Boolean active,
        UserRole role,
        List<AddressResponse> addresses) {
}
