package com.z.c.woodexcess_api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.z.c.woodexcess_api.dto.address.AddressRequest;
import com.z.c.woodexcess_api.dto.auth.LoginRequest;
import com.z.c.woodexcess_api.dto.auth.LoginResponse;
import com.z.c.woodexcess_api.dto.auth.RegisterRequest;
import com.z.c.woodexcess_api.dto.listing.CreateListingRequest;
import com.z.c.woodexcess_api.dto.listing.UpdateListingRequest;
import com.z.c.woodexcess_api.model.enums.Condition;
import com.z.c.woodexcess_api.model.enums.MaterialType;
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
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class MaterialListingControllerIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private com.z.c.woodexcess_api.repository.MaterialListingRepository listingRepository;

        private String userToken;
        private String adminToken;
        private String addressId;

        @BeforeEach
        void setUp() throws Exception {
                // Clean up database
                listingRepository.deleteAll();

                // Register and login user
                String uniqueEmail = "user-" + UUID.randomUUID() + "@example.com";

                AddressRequest addressRequest = AddressRequest.builder()
                                .street("Rua Teste")
                                .number("123")
                                .district("Centro")
                                .city("São Paulo")
                                .state("SP")
                                .zipCode("01310-100")
                                .country("Brasil")
                                .isPrimary(true)
                                .build();

                RegisterRequest registerRequest = new RegisterRequest(
                                "Test User",
                                uniqueEmail,
                                "11987654321",
                                "StrongPass123!@#",
                                List.of(addressRequest));

                mockMvc.perform(post("/api/users/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerRequest)))
                                .andExpect(status().isCreated());

                LoginRequest loginRequest = new LoginRequest(uniqueEmail, "StrongPass123!@#");
                MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isOk())
                                .andReturn();

                LoginResponse loginResponse = objectMapper.readValue(
                                loginResult.getResponse().getContentAsString(),
                                LoginResponse.class);
                userToken = loginResponse.accessToken();

                // Get user's address ID
                MvcResult addressResult = mockMvc.perform(get("/api/addresses")
                                .header("Authorization", "Bearer " + userToken))
                                .andExpect(status().isOk())
                                .andReturn();

                String addressJson = addressResult.getResponse().getContentAsString();
                addressId = objectMapper.readTree(addressJson).get(0).get("id").asText();
        }

        @Test
        @DisplayName("Should create listing successfully")
        void shouldCreateListingSuccessfully() throws Exception {
                CreateListingRequest request = new CreateListingRequest(
                                "Sobra de Madeira de Lei - Ipê",
                                "Tábuas de ipê em excelente estado",
                                MaterialType.WOOD,
                                new BigDecimal("150.50"),
                                10,
                                Condition.USED,
                                UUID.fromString(addressId));

                mockMvc.perform(post("/api/listings")
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andDo(print())
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.title").value("Sobra de Madeira de Lei - Ipê"))
                                .andExpect(jsonPath("$.materialType").value("WOOD"))
                                .andExpect(jsonPath("$.price").value(150.50))
                                .andExpect(jsonPath("$.quantity").value(10))
                                .andExpect(jsonPath("$.condition").value("USED"))
                                .andExpect(jsonPath("$.status").value("ACTIVE"))
                                .andExpect(jsonPath("$.city").value("São Paulo"))
                                .andExpect(jsonPath("$.state").value("SP"))
                                .andExpect(jsonPath("$.owner").exists())
                                .andExpect(jsonPath("$.address").exists());
        }

        @Test
        @DisplayName("Should fail to create listing without authentication")
        void shouldFailToCreateListingWithoutAuth() throws Exception {
                CreateListingRequest request = new CreateListingRequest(
                                "Sobra de Madeira",
                                "Descrição",
                                MaterialType.WOOD,
                                new BigDecimal("100.00"),
                                5,
                                Condition.NEW,
                                UUID.fromString(addressId));

                mockMvc.perform(post("/api/listings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should list all active listings publicly")
        void shouldListAllActiveListingsPublicly() throws Exception {
                // Create a listing first
                CreateListingRequest request = new CreateListingRequest(
                                "Sobra de MDF",
                                "MDF em bom estado",
                                MaterialType.MDF,
                                new BigDecimal("80.00"),
                                15,
                                Condition.USED,
                                UUID.fromString(addressId));

                mockMvc.perform(post("/api/listings")
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)));

                // List without authentication (public access)
                mockMvc.perform(get("/api/listings")
                                .param("page", "0")
                                .param("size", "10"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.content[0].title").value("Sobra de MDF"))
                                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));
        }

        @Test
        @DisplayName("Should filter listings by material type")
        void shouldFilterListingsByMaterialType() throws Exception {
                // Create WOOD listing
                CreateListingRequest woodRequest = new CreateListingRequest(
                                "Madeira de Lei",
                                "Ipê",
                                MaterialType.WOOD,
                                new BigDecimal("200.00"),
                                5,
                                Condition.NEW,
                                UUID.fromString(addressId));

                mockMvc.perform(post("/api/listings")
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(woodRequest)));

                // Create MDF listing
                CreateListingRequest mdfRequest = new CreateListingRequest(
                                "Sobra de MDF",
                                "MDF",
                                MaterialType.MDF,
                                new BigDecimal("50.00"),
                                10,
                                Condition.USED,
                                UUID.fromString(addressId));

                mockMvc.perform(post("/api/listings")
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(mdfRequest)));

                // Filter by WOOD
                mockMvc.perform(get("/api/listings")
                                .param("materialType", "WOOD"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.content[0].materialType").value("WOOD"));
        }

        @Test
        @DisplayName("Should filter listings by price range")
        void shouldFilterListingsByPriceRange() throws Exception {
                // Create listing
                CreateListingRequest request = new CreateListingRequest(
                                "Madeira Cara",
                                "Madeira premium",
                                MaterialType.WOOD,
                                new BigDecimal("500.00"),
                                3,
                                Condition.NEW,
                                UUID.fromString(addressId));

                mockMvc.perform(post("/api/listings")
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)));

                // Filter by price range
                mockMvc.perform(get("/api/listings")
                                .param("minPrice", "400")
                                .param("maxPrice", "600"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.content[0].price").value(500.00));
        }

        @Test
        @DisplayName("Should get listing by ID publicly")
        void shouldGetListingByIdPublicly() throws Exception {
                // Create listing
                CreateListingRequest request = new CreateListingRequest(
                                "Sobra de Compensado",
                                "Compensado naval",
                                MaterialType.PLYWOOD,
                                new BigDecimal("120.00"),
                                8,
                                Condition.USED,
                                UUID.fromString(addressId));

                MvcResult createResult = mockMvc.perform(post("/api/listings")
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andReturn();

                String listingId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                                .get("id").asText();

                // Get by ID without authentication
                mockMvc.perform(get("/api/listings/" + listingId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(listingId))
                                .andExpect(jsonPath("$.title").value("Sobra de Compensado"));
        }

        @Test
        @DisplayName("Should update listing successfully by owner")
        void shouldUpdateListingSuccessfullyByOwner() throws Exception {
                // Create listing
                CreateListingRequest createRequest = new CreateListingRequest(
                                "Título Original",
                                "Descrição original",
                                MaterialType.WOOD,
                                new BigDecimal("100.00"),
                                10,
                                Condition.USED,
                                UUID.fromString(addressId));

                MvcResult createResult = mockMvc.perform(post("/api/listings")
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createRequest)))
                                .andExpect(status().isCreated())
                                .andReturn();

                String listingId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                                .get("id").asText();

                // Update listing
                UpdateListingRequest updateRequest = new UpdateListingRequest(
                                "Título Atualizado",
                                "Descrição atualizada",
                                MaterialType.MDF,
                                new BigDecimal("150.00"),
                                5,
                                Condition.NEW,
                                null);

                mockMvc.perform(put("/api/listings/" + listingId)
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.title").value("Título Atualizado"))
                                .andExpect(jsonPath("$.price").value(150.00))
                                .andExpect(jsonPath("$.quantity").value(5));
        }

        @Test
        @DisplayName("Should fail to update listing without authentication")
        void shouldFailToUpdateListingWithoutAuth() throws Exception {
                UpdateListingRequest updateRequest = new UpdateListingRequest(
                                "Título",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null);

                mockMvc.perform(put("/api/listings/" + UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should deactivate listing successfully by owner")
        void shouldDeactivateListingSuccessfullyByOwner() throws Exception {
                // Create listing
                CreateListingRequest request = new CreateListingRequest(
                                "Listing para desativar",
                                "Será desativado",
                                MaterialType.WOOD,
                                new BigDecimal("100.00"),
                                5,
                                Condition.USED,
                                UUID.fromString(addressId));

                MvcResult createResult = mockMvc.perform(post("/api/listings")
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andReturn();

                String listingId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                                .get("id").asText();

                // Deactivate listing
                mockMvc.perform(patch("/api/listings/" + listingId + "/deactivate")
                                .header("Authorization", "Bearer " + userToken))
                                .andExpect(status().isNoContent());

                // Verify it's not in active listings
                mockMvc.perform(get("/api/listings")
                                .param("status", "ACTIVE"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[?(@.id == '" + listingId + "')]").doesNotExist());
        }

        @Test
        @DisplayName("Should fail to deactivate listing without authentication")
        void shouldFailToDeactivateListingWithoutAuth() throws Exception {
                mockMvc.perform(patch("/api/listings/" + UUID.randomUUID() + "/deactivate"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should filter listings by city and state")
        void shouldFilterListingsByCityAndState() throws Exception {
                // Create listing
                CreateListingRequest request = new CreateListingRequest(
                                "Madeira em SP",
                                "Localizada em São Paulo",
                                MaterialType.WOOD,
                                new BigDecimal("100.00"),
                                5,
                                Condition.USED,
                                UUID.fromString(addressId));

                mockMvc.perform(post("/api/listings")
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)));

                // Filter by city and state
                mockMvc.perform(get("/api/listings")
                                .param("city", "São Paulo")
                                .param("state", "SP"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.content[0].city").value("São Paulo"))
                                .andExpect(jsonPath("$.content[0].state").value("SP"));
        }

        @Test
        @DisplayName("Should support pagination and sorting")
        void shouldSupportPaginationAndSorting() throws Exception {
                // Create multiple listings
                for (int i = 1; i <= 3; i++) {
                        CreateListingRequest request = new CreateListingRequest(
                                        "Listing " + i,
                                        "Descrição " + i,
                                        MaterialType.WOOD,
                                        new BigDecimal(100 * i),
                                        i,
                                        Condition.USED,
                                        UUID.fromString(addressId));

                        mockMvc.perform(post("/api/listings")
                                        .header("Authorization", "Bearer " + userToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)));
                }

                // Test pagination
                mockMvc.perform(get("/api/listings")
                                .param("page", "0")
                                .param("size", "2")
                                .param("sort", "price,desc"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.content.length()").value(2))
                                .andExpect(jsonPath("$.totalElements").value(3))
                                .andExpect(jsonPath("$.content[0].price").value(300.00));
        }
}
