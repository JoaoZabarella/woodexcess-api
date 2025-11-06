package com.z.c.woodexcess_api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.z.c.woodexcess_api.dto.auth.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class UserRegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void shouldRegisterUserSuccessfully() throws Exception {
        RegisterRequest dto = new RegisterRequest("Joao", "joao@mail.com", "123456");
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Joao"))
                .andExpect(jsonPath("$.email").value("joao@mail.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }


    @Test
    void shouldFailToRegisterWithDuplicateEmail() throws Exception {
        RegisterRequest dto = new RegisterRequest("Joao", "duplicate@mail.com", "123456");

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto))
        ).andExpect(status().isOk());


        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto))
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Email")));
    }

    @Test
    void shouldFailWithInvalidPayload() throws Exception {
        RegisterRequest dto = new RegisterRequest("", "invalidemail", "123");
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto))
                )
                .andExpect(status().isBadRequest());
    }
}

