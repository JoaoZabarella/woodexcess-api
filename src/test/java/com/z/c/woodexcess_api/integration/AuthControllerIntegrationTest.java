package com.z.c.woodexcess_api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.z.c.woodexcess_api.dto.address.AddressRequest;
import com.z.c.woodexcess_api.dto.auth.LoginRequest;
import com.z.c.woodexcess_api.dto.auth.RegisterRequest;
import com.z.c.woodexcess_api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldLoginUserWithAddressSuccessfully() throws Exception {
        // ✅ CORRIGIDO: "Estado" → "SP"
        AddressRequest address = new AddressRequest(
                "Rua Y",
                "101",
                "",
                "Bairro",
                "Cidade",
                "SP",  // ✅ CORRIGIDO
                "54321-987",
                "Brasil"
        );

        RegisterRequest registerRequest = new RegisterRequest(
                "Jane",
                "jane@mail.com",
                "98765432",
                "securePass123",
                List.of(address)
        );

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest("jane@mail.com", "securePass123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldFailToLoginInactiveUser() throws Exception {
        // ✅ CORRIGIDO: "UF" → "RJ"
        AddressRequest address = new AddressRequest(
                "Rua Z",
                "200",
                "",
                "Novo",
                "AnotherCity",
                "RJ",  // ✅ CORRIGIDO
                "99999-000",
                "Brasil"
        );

        RegisterRequest registerRequest = new RegisterRequest(
                "Jack",
                "jack@mail.com",
                "13579113",
                "passInactive",
                List.of(address)
        );

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Desativa o usuário após o registro
        userRepository.findByEmail("jack@mail.com").ifPresent(user -> {
            user.setActive(false);
            userRepository.save(user);
        });

        LoginRequest loginRequest = new LoginRequest("jack@mail.com", "passInactive");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }
}
