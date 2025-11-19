package com.z.c.woodexcess_api.dto.user;

import com.z.c.woodexcess_api.dto.address.AddressResponse;
import com.z.c.woodexcess_api.mapper.AddressMapper;
import com.z.c.woodexcess_api.model.User;
import com.z.c.woodexcess_api.role.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String name;
    private String email;
    private String phone;
    private Boolean active;
    private UserRole role;
    private List<AddressResponse> addresses;

    public UserResponse(User user, AddressMapper addressMapper) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.active = user.getActive();
        this.role = user.getRole();
        this.addresses = user.getAddresses() != null
                ? user.getAddresses().stream()
                .map(addressMapper::toResponseDTO)
                .collect(Collectors.toList())
                : List.of();
    }
}


