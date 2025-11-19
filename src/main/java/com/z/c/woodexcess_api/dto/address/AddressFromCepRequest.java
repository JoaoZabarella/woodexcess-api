package com.z.c.woodexcess_api.dto.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressFromCepRequest {

    @NotBlank(message = "ZIP code is required")
    @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "ZIP code must be in format 12345-678 or 12345678")
    private String zipCode;

    @NotBlank(message = "Number is required")
    @Size(max = 20, message = "Number must be at most 20 characters")
    private String number;

    @Size(max = 255, message = "Complement must be at most 255 characters")
    private String complement;

    private Boolean isPrimary;
}
