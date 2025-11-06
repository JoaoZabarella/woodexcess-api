package com.z.c.woodexcess_api.service;

import com.z.c.woodexcess_api.dto.auth.RegisterRequest;
import com.z.c.woodexcess_api.exception.users.EmailAlredyExistException;
import com.z.c.woodexcess_api.model.User;
import com.z.c.woodexcess_api.repository.UserRepository;
import com.z.c.woodexcess_api.role.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class UserServiceTest {

    @Mock
    private UserRepository repository;
    @Mock
    private PasswordEncoder encoder;
    @InjectMocks
    private UserService service;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    void shouldRegisterNewUser() throws IllegalAccessException {

        RegisterRequest dto = new RegisterRequest("John", "john@mail.com", "123456");
        when(repository.findByEmail("john@mail.com")).thenReturn(Optional.empty());
        when(encoder.encode("123456")).thenReturn("hashed");

        User savedUser = new User(UUID.randomUUID(), "John", "john@mail.com", "hashed", UserRole.USER);
        when(repository.save(any(User.class))).thenReturn(savedUser);

        var response = service.registerUser(dto);

        assertNotNull(response);
        assertEquals("John", response.name());
        assertEquals("john@mail.com", response.email());
        assertEquals(UserRole.USER, response.role());
    }

    @Test
    void shouldThrowExceptionWhenEmailAlreadyExist() throws IllegalAccessException {
        RegisterRequest dto = new RegisterRequest("John", "john@mail.com", "123456");
        when(repository.findByEmail("john@mail.com")).thenReturn(Optional.of(new User()));

        Exception ex = assertThrows(EmailAlredyExistException.class, () -> service.registerUser(dto));
        assertEquals("Email already exists", ex.getMessage());
    }
}