package com.z.c.woodexcess_api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.z.c.woodexcess_api.dto.address.AddressRequest;
import com.z.c.woodexcess_api.dto.auth.LoginRequest;
import com.z.c.woodexcess_api.dto.auth.LoginResponse;
import com.z.c.woodexcess_api.dto.auth.RegisterRequest;
import com.z.c.woodexcess_api.dto.listing.CreateListingRequest;
import com.z.c.woodexcess_api.enums.Condition;
import com.z.c.woodexcess_api.enums.MaterialType;
import com.z.c.woodexcess_api.service.storage.S3StorageService;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ListingImageControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private S3StorageService s3StorageService;

    @MockBean
    private com.z.c.woodexcess_api.processor.ImageProcessor imageProcessor;

    private String userToken;
    private String listingId;
    private byte[] validImageBytes;
    private AtomicInteger s3KeyCounter;

    @BeforeEach
    void setUp() throws Exception {
        s3KeyCounter = new AtomicInteger(0);
        validImageBytes = createValidImageBytes();

        when(s3StorageService.upload(any(), anyString())).thenAnswer(invocation ->
                "s3-key-" + s3KeyCounter.incrementAndGet());


        when(s3StorageService.getPubliUrl(anyString())).thenReturn("http://s3.com/image.jpg");

        when(imageProcessor.generateThumbnail(any())).thenReturn(validImageBytes);
        when(imageProcessor.createMultipartFile(any(), anyString()))
                .thenReturn(new MockMultipartFile("thumb", "thumb.jpg", "image/jpeg", validImageBytes));

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

        // Get address ID
        MvcResult addressResult = mockMvc.perform(get("/api/addresses")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn();
        String addressJson = addressResult.getResponse().getContentAsString();
        String addressId = objectMapper.readTree(addressJson).get(0).get("id").asText();

        // Create Listing
        CreateListingRequest listingRequest = new CreateListingRequest(
                "Listing for Images",
                "Description",
                MaterialType.WOOD,
                new BigDecimal("100.00"),
                10,
                Condition.USED,
                UUID.fromString(addressId));

        MvcResult createResult = mockMvc.perform(post("/api/listings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(listingRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        listingId = objectMapper.readTree(responseBody).get("id").asText();
    }

    private byte[] createValidImageBytes() throws Exception {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 100, 100);
        graphics.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    @Test
    @DisplayName("Should upload image successfully")
    void shouldUploadImageSuccessfully() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                validImageBytes);

        mockMvc.perform(multipart("/api/listings/" + listingId + "/images")
                        .file(file)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageUrl").exists())
                .andExpect(jsonPath("$.isPrimary").value(false)); // Primeira imagem é primary

        verify(s3StorageService, times(2)).upload(any(), anyString());
    }

    @Test
    @DisplayName("Should get images by listing")
    void shouldGetImagesByListing() throws Exception {
        // Upload an image first
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                validImageBytes);

        mockMvc.perform(multipart("/api/listings/" + listingId + "/images")
                        .file(file)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isCreated());

        // Get images
        mockMvc.perform(get("/api/listings/" + listingId + "/images"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].imageUrl").exists());
    }

    @Test
    @DisplayName("Should set primary image")
    void shouldSetPrimaryImage() throws Exception {
        // Upload two images
        MockMultipartFile file1 = new MockMultipartFile("file", "img1.jpg", MediaType.IMAGE_JPEG_VALUE, validImageBytes);
        MockMultipartFile file2 = new MockMultipartFile("file", "img2.jpg", MediaType.IMAGE_JPEG_VALUE, validImageBytes);

        mockMvc.perform(multipart("/api/listings/" + listingId + "/images")
                        .file(file1)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isCreated());

        MvcResult result2 = mockMvc.perform(multipart("/api/listings/" + listingId + "/images")
                        .file(file2)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isCreated())
                .andReturn();

        String image2Id = objectMapper.readTree(result2.getResponse().getContentAsString()).get("id").asText();

        // Set second image as primary
        mockMvc.perform(put("/api/listings/" + listingId + "/images/" + image2Id + "/primary")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        // Verify the image is now primary
        mockMvc.perform(get("/api/listings/" + listingId + "/images"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + image2Id + "')].isPrimary").value(true));
    }

    @Test
    @DisplayName("Should delete image")
    void shouldDeleteImage() throws Exception {
        // Upload image
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                validImageBytes);

        MvcResult result = mockMvc.perform(multipart("/api/listings/" + listingId + "/images")
                        .file(file)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isCreated())
                .andReturn();

        String imageId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        // Delete image
        mockMvc.perform(delete("/api/listings/" + listingId + "/images/" + imageId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());

        // Verify deletion from S3
        verify(s3StorageService, atLeast(1)).delete(anyString());

        // Verify empty list
        mockMvc.perform(get("/api/listings/" + listingId + "/images"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("Should fail when uploading without authentication")
    void shouldFailWithoutAuthentication() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                validImageBytes);

        mockMvc.perform(multipart("/api/listings/" + listingId + "/images")
                        .file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should fail when listing not found")
    void shouldFailWhenListingNotFound() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                validImageBytes);

        UUID nonExistentListingId = UUID.randomUUID();

        mockMvc.perform(multipart("/api/listings/" + nonExistentListingId + "/images")
                        .file(file)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should reorder images successfully")
    void shouldReorderImagesSuccessfully() throws Exception {

        MockMultipartFile file1 = new MockMultipartFile("file", "img1.jpg", MediaType.IMAGE_JPEG_VALUE, validImageBytes);
        MockMultipartFile file2 = new MockMultipartFile("file", "img2.jpg", MediaType.IMAGE_JPEG_VALUE, validImageBytes);
        MockMultipartFile file3 = new MockMultipartFile("file", "img3.jpg", MediaType.IMAGE_JPEG_VALUE, validImageBytes);

        MvcResult r1 = mockMvc.perform(multipart("/api/listings/" + listingId + "/images").file(file1)
                .header("Authorization", "Bearer " + userToken)).andReturn();
        MvcResult r2 = mockMvc.perform(multipart("/api/listings/" + listingId + "/images").file(file2)
                .header("Authorization", "Bearer " + userToken)).andReturn();
        MvcResult r3 = mockMvc.perform(multipart("/api/listings/" + listingId + "/images").file(file3)
                .header("Authorization", "Bearer " + userToken)).andReturn();

        String id1 = objectMapper.readTree(r1.getResponse().getContentAsString()).get("id").asText();
        String id2 = objectMapper.readTree(r2.getResponse().getContentAsString()).get("id").asText();
        String id3 = objectMapper.readTree(r3.getResponse().getContentAsString()).get("id").asText();

        // Reorder: 3, 1, 2
        String reorderJson = objectMapper.writeValueAsString(List.of(id3, id1, id2));

        mockMvc.perform(put("/api/listings/" + listingId + "/images/reorder")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reorderJson))
                .andExpect(status().isOk());

        MvcResult listResult = mockMvc.perform(get("/api/listings/" + listingId + "/images"))
                .andExpect(status().isOk())
                .andReturn();

        String responseJson = listResult.getResponse().getContentAsString();
        assertThat(responseJson).contains(id3);
    }
}
