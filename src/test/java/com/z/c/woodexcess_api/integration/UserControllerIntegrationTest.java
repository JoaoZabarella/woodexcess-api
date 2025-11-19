package com.z.c.woodexcess_api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.z.c.woodexcess_api.dto.address.AddressRequest;
import com.z.c.woodexcess_api.dto.auth.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@ActiveProfiles("test")
public class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRegisterUserWithAddressSuccessfully() throws Exception {
        // ✅ CORRIGIDO: "Estado" → "SP"
        AddressRequest address = new AddressRequest(
                "Rua X",
                "100",
                "",
                "Centro",
                "Cidade",
                "SP",  // ✅ CORRIGIDO - UF com 2 caracteres
                "12345-678",
                "Brasil"
        );

        RegisterRequest request = new RegisterRequest(
                "John",
                "john@mail.com",
                "123456",
                "12345678",
                List.of(address)
        );

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("John"))
                .andExpect(jsonPath("$.email").value("john@mail.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void shouldFailToRegisterWithInvalidAddressPayload() throws Exception {
        AddressRequest address = new AddressRequest(
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                ""
        );

        RegisterRequest request = new RegisterRequest(
                "John",
                "john@mail.com",
                "123456",
                "12345678",
                List.of(address)
        );

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
