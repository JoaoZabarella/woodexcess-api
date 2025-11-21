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
        private final AddressMapper addressMapper;

        public UserMapper(PasswordEncoder passwordEncoder, AddressMapper addressMapper) {
            this.passwordEncoder = passwordEncoder;
            this.addressMapper = addressMapper;
        }

        public User toEntity(RegisterRequest dto) {
            User user = new User();
            user.setName(dto.getName());
            user.setEmail(dto.getEmail());
            user.setPhone(dto.getPhone());
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
            user.setRole(UserRole.USER);
            user.setActive(true);


            if (dto.getAddresses() != null) {
                List<Address> addresses = dto.getAddresses().stream().map(addressDto -> {
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
                    address.setUser(user);
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
            return new UserResponse(user, addressMapper);
        }
    }

