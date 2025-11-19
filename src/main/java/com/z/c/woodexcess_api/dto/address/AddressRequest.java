package com.z.c.woodexcess_api.dto.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class AddressRequest{

    @NotBlank(message = "Street is required")
    @Size(min = 3, max = 255, message = "Street must be between 3 and 255 characters")
    private String street;

    @NotBlank(message = "Number is required")
    @Size(max = 20, message = "Number must be at most 20 characters")
    private String number;

    @Size(max = 255, message = "Complement must be at most 255 characters")
    private String complement;

    @NotBlank(message = "District is required")
    @Size(min = 2, max = 100, message = "District must be between 2 and 100 characters")
    private String district;

    @NotBlank(message = "City is required")
    @Size(min = 2, max = 100, message = "City must be between 2 and 100 characters")
    private String city;

    @NotBlank(message = "State is required")
    @Size(min = 2, max = 2, message = "State must be exactly 2 characters (UF)")
    @Pattern(regexp = "^[A-Z]{2}$", message = "State must be a valid Brazilian UF (e.g., SP, RJ)")
    private String state;

    @NotBlank(message = "ZIP code is required")
    @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "ZIP code must be in format 12345-678 or 12345678")
    private String zipCode;

    @Size(min = 2, max = 50, message = "Country must be between 2 and 50 characters")
    private String country;

    private Boolean isPrimary;
}