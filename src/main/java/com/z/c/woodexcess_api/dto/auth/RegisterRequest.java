package com.z.c.woodexcess_api.dto.auth;

import com.z.c.woodexcess_api.dto.address.AddressRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

import java.util.List;

@Builder
public record RegisterRequest(
        @NotBlank(message = "Name is required") String name,

        @NotBlank(message = "Email is required") @Email(message = "Email must be valid") String email,

        @NotBlank(message = "Phone is required") @Pattern(regexp = "^\\d{10,15}$", message = "Phone must contain 10-15 digits") String phone,

        @NotBlank(message = "Password is required") @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,}$", message = "Password must contain at least 8 characters, one uppercase, one lowercase, one number and one special character") String password,

        @Valid List<AddressRequest> addresses) {
}
