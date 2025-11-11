    package com.z.c.woodexcess_api.mapper;

    import com.z.c.woodexcess_api.dto.auth.RegisterRequest;
    import com.z.c.woodexcess_api.dto.auth.RegisterResponse;
    import com.z.c.woodexcess_api.dto.user.UserResponse;
    import com.z.c.woodexcess_api.model.Address;
    import com.z.c.woodexcess_api.model.User;
    import com.z.c.woodexcess_api.role.UserRole;
    import org.springframework.security.crypto.password.PasswordEncoder;
    import org.springframework.stereotype.Component;

    import java.util.List;

    @Component
    public class UserMapper {
        private final PasswordEncoder passwordEncoder;

        public UserMapper(PasswordEncoder passwordEncoder) {
            this.passwordEncoder = passwordEncoder;
        }

        public User toEntity(RegisterRequest dto) {
            User user = new User();
            user.setName(dto.name());
            user.setEmail(dto.email());
            user.setPhone(dto.phone());
            user.setPassword(passwordEncoder.encode(dto.password()));
            user.setRole(UserRole.USER);
            user.setActive(true);


            if (dto.addresses() != null) {
                List<Address> addresses = dto.addresses().stream().map(addressDto -> {
                    Address address = new Address();
                    address.setStreet(addressDto.street());
                    address.setNumber(addressDto.number());
                    address.setComplement(addressDto.complement());
                    address.setDistrict(addressDto.district());
                    address.setCity(addressDto.city());
                    address.setState(addressDto.state());
                    address.setZipCode(addressDto.zipCode());
                    address.setCountry(addressDto.country());
                    address.setActive(true);
                    address.setUser(user); // relacional JPA
                    return address;
                }).toList();
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
            return new UserResponse(user);
        }
    }

