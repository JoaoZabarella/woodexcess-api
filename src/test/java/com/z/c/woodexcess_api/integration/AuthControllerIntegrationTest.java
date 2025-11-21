package com.z.c.woodexcess_api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.z.c.woodexcess_api.dto.address.AddressRequest;
import com.z.c.woodexcess_api.dto.auth.LoginRequest;
import com.z.c.woodexcess_api.dto.auth.RegisterRequest;
import com.z.c.woodexcess_api.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Deve fazer login com usuário que possui endereço")
    void shouldLoginUserWithAddressSuccessfully() throws Exception {

        String uniqueEmail = "jane-" + UUID.randomUUID() + "@mail.com";

        AddressRequest address = AddressRequest.builder()
                .street("Rua Y")
                .number("101")
                .complement(null)
                .district("Sul")
                .city("São Paulo")
                .state("SP")
                .zipCode("12345-674")
                .country("Italia")
                .isPrimary(false)
                .build();


        RegisterRequest registerRequest = new RegisterRequest(
                "Jane",
                uniqueEmail,
                "11987654321",
                "SecurePass123!@#",
                List.of(address)
        );

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest(uniqueEmail, "SecurePass123!@#");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    @DisplayName("Deve falhar ao fazer login com usuário inativo")
    void shouldFailToLoginInactiveUser() throws Exception {
        String uniqueEmail = "jack-" + UUID.randomUUID() + "@mail.com";

        AddressRequest address = AddressRequest.builder()
                .street("Rua Z")
                .number("200")
                .complement("")
                .district("Novo")
                .city("AnotherCity")
                .state("RJ")
                .zipCode("99999-000")
                .country("Brasil")
                .isPrimary(true)
                .build();

        RegisterRequest registerRequest = new RegisterRequest(
                "Jack",
                uniqueEmail,
                "21987654321",
                "PassInactive123!@#",
                List.of(address)
        );

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        userRepository.findByEmail(uniqueEmail).ifPresent(user -> {
            user.setActive(false);
            userRepository.save(user);
        });

        LoginRequest loginRequest = new LoginRequest(uniqueEmail, "PassInactive123!@#");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }
}
