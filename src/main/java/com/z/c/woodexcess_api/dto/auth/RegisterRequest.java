package com.z.c.woodexcess_api.dto.auth;

import com.z.c.woodexcess_api.dto.address.AddressRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import java.util.List;

public record RegisterRequest(
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "Email is required") @Email(message = "Email must be valid") String email,
        @NotBlank(message = "Phone is required") @Size(max = 20, message = "A phone can only have 20 numbers.") String phone,
        @NotBlank(message = "Password is required") @Size(min = 6, message = "Password must be at least 6 characters") String password,
        @NotNull(message = "Required at least one address") @Valid List<AddressRequest> addresses
) {}
