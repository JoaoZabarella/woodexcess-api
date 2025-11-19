package com.z.c.woodexcess_api.dto.address;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.z.c.woodexcess_api.model.Address;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
import java.time.LocalDateTime;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddressResponse{

    private UUID id;
    private UUID userId;
    private String street;
    private String number;
    private String complement;
    private String district;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    private Boolean active;
    private Boolean isPrimary;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;


    private String fullAddress;

}
