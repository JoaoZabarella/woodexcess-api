package com.z.c.woodexcess_api.dto.address;

import jakarta.validation.constraints.NotBlank;

public record AddressRequest(
        @NotBlank(message = "Street is required") String street,
        @NotBlank(message = "Number is required") String number,
        String complement,
        @NotBlank(message = "District is required") String district,
        @NotBlank(message = "City is required") String city,
        @NotBlank(message = "State is required") String state,
        @NotBlank(message = "ZipCode is required") String zipCode,
        @NotBlank(message = "Country is required") String country
) {}
