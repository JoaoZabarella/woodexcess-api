package com.z.c.woodexcess_api.dto.listing;

import com.z.c.woodexcess_api.enums.Condition;
import com.z.c.woodexcess_api.enums.MaterialType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateListingRequest(
                @NotBlank(message = "Title is required") @Size(max = 100, message = "Title must not exceed 100 characters") String title,

                @NotBlank(message = "Description is required") @Size(max = 2000, message = "Description must not exceed 2000 characters") String description,

                @NotNull(message = "Material type is required") MaterialType materialType,

                @NotNull(message = "Price is required") @DecimalMin(value = "0.01", message = "Price must be greater than 0") @Digits(integer = 8, fraction = 2, message = "Price must have at most 8 integer digits and 2 decimal places") BigDecimal price,

                @NotNull(message = "Quantity is required") @Min(value = 1, message = "Quantity must be at least 1") Integer quantity,

                @NotNull(message = "Condition is required") Condition condition,

                UUID addressId) {
}
