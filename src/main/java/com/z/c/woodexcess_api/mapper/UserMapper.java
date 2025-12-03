package com.z.c.woodexcess_api.mapper;

import com.z.c.woodexcess_api.dto.address.AddressResponse;
import com.z.c.woodexcess_api.dto.auth.RegisterRequest;
import com.z.c.woodexcess_api.dto.auth.RegisterResponse;
import com.z.c.woodexcess_api.dto.user.UserResponse;
import com.z.c.woodexcess_api.model.Address;
import com.z.c.woodexcess_api.model.User;
import com.z.c.woodexcess_api.enums.UserRole;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class UserMapper {
    private final PasswordEncoder passwordEncoder;
    private final AddressMapper addressMapper;

    public UserMapper(PasswordEncoder passwordEncoder, AddressMapper addressMapper) {
        this.passwordEncoder = passwordEncoder;
        this.addressMapper = addressMapper;
    }

    public User toEntity(RegisterRequest dto) {
        User user = User.builder()
                .name(dto.name())
                .email(dto.email())
                .phone(dto.phone())
                .password(passwordEncoder.encode(dto.password()))
                .role(UserRole.USER)
                .active(true)
                .build();

        if (dto.addresses() != null && !dto.addresses().isEmpty()) {
            List<Address> addresses = dto.addresses().stream()
                    .map(addressDto -> addressMapper.toEntity(addressDto, user))
                    .toList();
            user.setAddresses(addresses);
        } else {
            user.setAddresses(Collections.emptyList());
        }

        return user;
    }

    public RegisterResponse toRegisterResponse(User user) {
        return new RegisterResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getActive(),
                user.getRole()
        );
    }

    public UserResponse toUserResponse(User user) {
        List<AddressResponse> addressResponses = user.getAddresses() != null
                ? user.getAddresses().stream()
                .map(addressMapper::toResponseDTO)
                .toList()
                : Collections.emptyList();

        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getActive(),
                user.getRole(),
                addressResponses
        );
    }
}
