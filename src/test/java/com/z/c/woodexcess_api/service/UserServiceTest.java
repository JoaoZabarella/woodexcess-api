package com.z.c.woodexcess_api.service;

import com.z.c.woodexcess_api.dto.address.AddressRequest;
import com.z.c.woodexcess_api.dto.auth.RegisterRequest;
import com.z.c.woodexcess_api.dto.auth.RegisterResponse;
import com.z.c.woodexcess_api.mapper.UserMapper;
import com.z.c.woodexcess_api.model.Address;
import com.z.c.woodexcess_api.model.User;
import com.z.c.woodexcess_api.repository.UserRepository;
import com.z.c.woodexcess_api.role.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository repository;
    @Mock
    PasswordEncoder encoder;
    @Mock
    UserMapper mapper;

    UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(repository, encoder, mapper);
    }

    @Test
    void registerUser_withAddress_success() {
        AddressRequest address = new AddressRequest("Rua 1", "100", "", "Centro", "Cidade", "Estado", "12345-678", "Brasil");
        RegisterRequest dto = new RegisterRequest("John", "john@email.com", "12345678", "12345678", List.of(address));
        User user = new User();
        when(repository.findByEmail(dto.email())).thenReturn(Optional.empty());
        when(mapper.toEntity(dto)).thenReturn(user);
        when(encoder.encode(dto.password())).thenReturn("hashedPwd");
        when(repository.save(any())).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(UUID.randomUUID());
            u.setAddresses(List.of(new Address(/*...*/)));
            return u;
        });
        when(mapper.toRegisterResponse(any(User.class)))
                .thenReturn(new RegisterResponse("mockId", "John", "john@email.com", "12345678", true, UserRole.USER));

        RegisterResponse result = service.registerUser(dto);
        assertEquals("John", result.name());
        assertEquals("john@email.com", result.email());
        assertEquals(UserRole.USER, result.role());
    }

    @Test
    void registerUser_withInvalidAddress() {
        AddressRequest address = new AddressRequest("", "", "", "", "", "", "", "");
        RegisterRequest dto = new RegisterRequest("John", "john@email.com", "12345678", "12345678", List.of(address));
        when(repository.findByEmail(dto.email())).thenReturn(Optional.empty());
        when(mapper.toEntity(dto)).thenThrow(new IllegalArgumentException("Invalid address"));
        assertThrows(IllegalArgumentException.class, () -> service.registerUser(dto));
    }
}
