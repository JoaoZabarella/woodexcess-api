package com.z.c.woodexcess_api.mapper;

import com.z.c.woodexcess_api.dto.address.AddressRequest;
import com.z.c.woodexcess_api.dto.auth.RegisterRequest;
import com.z.c.woodexcess_api.dto.auth.RegisterResponse;
import com.z.c.woodexcess_api.dto.user.UserResponse;
import com.z.c.woodexcess_api.model.Address;
import com.z.c.woodexcess_api.model.User;
import com.z.c.woodexcess_api.role.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final PasswordEncoder passwordEncoder;
    private final AddressMapper addressMapper;

    public User toEntity(RegisterRequest dto) {
        User user = new User();
        user.setName(dto.name());
        user.setEmail(dto.email());
        user.setPhone(dto.phone());
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setRole(UserRole.USER);
        user.setActive(true);

        if (dto.addresses() != null) {
            List<Address> addresses = dto.addresses().stream()
                    .map(addressDto -> {
                        Address address = new Address();
                        address.setStreet(addressDto.getStreet());
                        address.setNumber(addressDto.getNumber());
                        address.setComplement(addressDto.getComplement());
                        address.setDistrict(addressDto.getDistrict());
                        address.setCity(addressDto.getCity());
                        address.setState(addressDto.getState());
                        address.setZipCode(addressDto.getZipCode());
                        address.setCountry(addressDto.getCountry());
                        address.setActive(true);
                        address.setUser(user); // relacional JPA
                        return address;
                    })
                    .toList();

            user.setAddresses(addresses);
        }

        return user;
    }

    public RegisterResponse toRegisterResponse(User user) {
        return new RegisterResponse(
                user.getId().toString(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getActive(),
                user.getRole()
        );
    }

    public UserResponse toUserResponse(User user) {
        return new UserResponse(user, addressMapper);
    }
}
