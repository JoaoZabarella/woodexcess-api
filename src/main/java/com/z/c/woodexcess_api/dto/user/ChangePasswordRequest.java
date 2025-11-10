package com.z.c.woodexcess_api.dto.user;

public record ChangePasswordRequest(
        String currentPassword,
        String newPassword
) {
}
