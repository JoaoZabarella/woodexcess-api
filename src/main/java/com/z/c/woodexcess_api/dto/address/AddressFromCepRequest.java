package com.z.c.woodexcess_api.dto.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record AddressFromCepRequest(
        @NotBlank(message = "ZIP code is required") @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "ZIP code must be in format 12345-678 or 12345678") String zipCode,

        @NotBlank(message = "Number is required") @Size(max = 20, message = "Number must be at most 20 characters") String number,

        @Size(max = 255, message = "Complement must be at most 255 characters") String complement,

        Boolean isPrimary) {
}
