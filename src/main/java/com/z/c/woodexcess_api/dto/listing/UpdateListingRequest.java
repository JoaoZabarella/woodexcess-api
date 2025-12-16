package com.z.c.woodexcess_api.dto.listing;

import com.z.c.woodexcess_api.model.enums.Condition;
import com.z.c.woodexcess_api.model.enums.MaterialType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateListingRequest(
                @Size(max = 100, message = "Title must not exceed 100 characters") String title,

                @Size(max = 2000, message = "Description must not exceed 2000 characters") String description,

                MaterialType materialType,

                @DecimalMin(value = "0.01", message = "Price must be greater than 0") @Digits(integer = 8, fraction = 2, message = "Price must have at most 8 integer digits and 2 decimal places") BigDecimal price,

                @Min(value = 1, message = "Quantity must be at least 1") Integer quantity,

                Condition condition,

                UUID addressId) {
}
