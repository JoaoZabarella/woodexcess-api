package com.z.c.woodexcess_api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.z.c.woodexcess_api.dto.address.AddressFromCepRequest;
import com.z.c.woodexcess_api.dto.address.AddressRequest;
import com.z.c.woodexcess_api.dto.auth.LoginRequest;
import com.z.c.woodexcess_api.dto.auth.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AddressControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {

        String uniqueEmail = "address-test-" + UUID.randomUUID() + "@example.com";

        RegisterRequest registerRequest = RegisterRequest.builder()
                .email(uniqueEmail)
                .password("Test123!@#")
                .name("Address Test User")
                .phone("11971407689")

                .build();

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());


        LoginRequest loginRequest = LoginRequest.builder()
                .email(uniqueEmail)
                .password("Test123!@#")
                .build();

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        accessToken = objectMapper.readTree(response).get("accessToken").asText();
    }

    @Test
    @DisplayName("Deve criar endereço manualmente com sucesso")
    void shouldCreateAddressManually() throws Exception {
        AddressRequest request = AddressRequest.builder()
                .street("Rua das Flores")
                .number("123")
                .complement("Apto 45")
                .district("Centro")
                .city("São Paulo")
                .state("SP")
                .zipCode("01310-100")
                .country("Brasil")
                .isPrimary(true)
                .build();

        mockMvc.perform(post("/api/addresses")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.street").value("Rua das Flores"));
    }

    @Test
    @DisplayName("Deve listar endereços do usuário")
    void shouldListAllUserAddresses() throws Exception {

        AddressRequest request = AddressRequest.builder()
                .street("Rua Teste")
                .number("789")
                .district("Bairro")
                .city("São Paulo")
                .state("SP")
                .zipCode("01310-100")
                .build();

        mockMvc.perform(post("/api/addresses")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());


        mockMvc.perform(get("/api/addresses")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].street").value("Rua Teste"));
    }

    @Test
    @DisplayName("Deve atualizar endereço com sucesso")
    void shouldUpdateAddress() throws Exception {

        AddressRequest createRequest = AddressRequest.builder()
                .street("Rua Original")
                .number("100")
                .district("Centro")
                .city("São Paulo")
                .state("SP")
                .zipCode("01310-100")
                .build();

        String createResponse = mockMvc.perform(post("/api/addresses")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String addressId = objectMapper.readTree(createResponse).get("id").asText();


        AddressRequest updateRequest = AddressRequest.builder()
                .street("Rua Atualizada")
                .number("200")
                .district("Jardins")
                .city("São Paulo")
                .state("SP")
                .zipCode("01310-100")
                .build();

        mockMvc.perform(put("/api/addresses/" + addressId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.street").value("Rua Atualizada"));
    }

    @Test
    @DisplayName("Deve criar endereço via CEP com sucesso")
    void shouldCreateAddressFromCep() throws Exception {
        AddressFromCepRequest request = AddressFromCepRequest.builder()
                .zipCode("01310-100")
                .number("456")
                .complement("Bloco B")
                .isPrimary(true)
                .build();

        mockMvc.perform(post("/api/addresses/from-cep")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("Deve deletar endereço com sucesso")
    void shouldDeleteAddress() throws Exception {

        AddressRequest createRequest = AddressRequest.builder()
                .street("Rua Temporária")
                .number("999")
                .district("Centro")
                .city("São Paulo")
                .state("SP")
                .zipCode("01310-100")
                .build();

        String createResponse = mockMvc.perform(post("/api/addresses")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String addressId = objectMapper.readTree(createResponse).get("id").asText();


        mockMvc.perform(delete("/api/addresses/" + addressId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());


        mockMvc.perform(get("/api/addresses/" + addressId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }
}
