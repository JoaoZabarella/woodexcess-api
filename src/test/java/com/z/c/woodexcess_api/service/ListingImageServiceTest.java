package com.z.c.woodexcess_api.service;

import com.z.c.woodexcess_api.dto.listing.ImageResponse;
import com.z.c.woodexcess_api.exception.listing.ListingImageException;
import com.z.c.woodexcess_api.exception.listing.ListingNotFoundException;
import com.z.c.woodexcess_api.mapper.ImageMapper;
import com.z.c.woodexcess_api.model.ListingImage;
import com.z.c.woodexcess_api.model.MaterialListing;
import com.z.c.woodexcess_api.model.User;
import com.z.c.woodexcess_api.processor.ImageProcessor;
import com.z.c.woodexcess_api.repository.ListingImageRepository;
import com.z.c.woodexcess_api.repository.MaterialListingRepository;
import com.z.c.woodexcess_api.service.listing.ListingImageService;
import com.z.c.woodexcess_api.service.storage.StorageService;
import com.z.c.woodexcess_api.validator.ImageValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListingImageService Unit Tests")
class ListingImageServiceTest {

    @Mock
    private ListingImageRepository imageRepository;

    @Mock
    private MaterialListingRepository listingRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private ImageValidator imageValidator;

    @Mock
    private ImageProcessor imageProcessor;

    @Mock
    private ImageMapper imageMapper;

    @InjectMocks
    private ListingImageService listingImageService;

    private UUID listingId;
    private MaterialListing listing;
    private User owner;
    private MultipartFile mockFile;
    private static final int MAX_IMAGES = 10;

    @BeforeEach
    void setUp() {
        listingId = UUID.randomUUID();
        owner  = User.builder()
                .id(UUID.randomUUID())
                .name("Test Seller")
                .email("seller@test.com")
                .build();

        listing = MaterialListing.builder()
                .id(listingId)
                .title("Test Listing")
                .owner(owner)
                .build();

        mockFile = new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                "test image content".getBytes());

        // Set maxImages field value
        ReflectionTestUtils.setField(listingImageService, "maxImages", MAX_IMAGES);
    }


    private ImageResponse createImageResponse(UUID id, String imageUrl, String thumbnailUrl,
                                              Integer displayOrder, Boolean isPrimary,
                                              Long fileSize, String fileExtension) {
        return new ImageResponse(id, imageUrl, thumbnailUrl, displayOrder,
                fileSize, fileExtension, isPrimary, LocalDateTime.now());
    }



    @Test
    @DisplayName("Should add image successfully as primary when isPrimary is true")
    void addImage_WithPrimaryTrue_Success() throws Exception {
        // Arrange
        byte[] thumbnailBytes = "thumbnail data".getBytes();
        MockMultipartFile thumbnailFile = new MockMultipartFile(
                "thumbnail", "thumb.jpg", "image/jpeg", thumbnailBytes);

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        doNothing().when(imageValidator).validate(mockFile);
        when(imageRepository.countByListing(listing)).thenReturn(0L);
        when(storageService.upload(eq(mockFile), anyString())).thenReturn("images/original-key");
        when(storageService.getPubliUrl("images/original-key")).thenReturn("https://s3.com/original.jpg");
        when(imageProcessor.generateThumbnail(mockFile.getBytes())).thenReturn(thumbnailBytes);
        when(imageProcessor.createMultipartFile(thumbnailBytes, "thumbnail-test-image.jpg"))
                .thenReturn(thumbnailFile);
        when(storageService.upload(eq(thumbnailFile), anyString())).thenReturn("thumbnails/thumb-key");
        when(storageService.getPubliUrl("thumbnails/thumb-key")).thenReturn("https://s3.com/thumb.jpg");
        when(imageRepository.findMaxDisplayOrderByListing(listing)).thenReturn(Optional.empty());
        when(imageRepository.removeAllPrimaryFlags(listing)).thenReturn(0);
        when(imageRepository.save(any(ListingImage.class))).thenAnswer(inv -> inv.getArgument(0));

        ImageResponse expectedResponse = createImageResponse(
                UUID.randomUUID(),
                "https://s3.com/original.jpg",
                "https://s3.com/thumb.jpg",
                1,
                true,
                mockFile.getSize(),
                "jpg"
        );
        when(imageMapper.toResponse(any(ListingImage.class))).thenReturn(expectedResponse);

        // Act
        ImageResponse result = listingImageService.addImage(listingId, mockFile, true);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.imageUrl()).isEqualTo("https://s3.com/original.jpg");
        assertThat(result.isPrimary()).isTrue();

        verify(listingRepository).findById(listingId);
        verify(imageValidator).validate(mockFile);
        verify(imageRepository).countByListing(listing);
        verify(storageService, times(2)).upload(any(), anyString());
        verify(imageRepository).removeAllPrimaryFlags(listing);
        verify(imageRepository).save(any(ListingImage.class));
    }

    @Test
    @DisplayName("Should add image successfully as non-primary when isPrimary is false")
    void addImage_WithPrimaryFalse_Success() throws Exception {
        // Arrange
        byte[] thumbnailBytes = "thumbnail".getBytes();
        MockMultipartFile thumbnailFile = new MockMultipartFile("thumb", thumbnailBytes);

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        doNothing().when(imageValidator).validate(mockFile);
        when(imageRepository.countByListing(listing)).thenReturn(2L);
        when(storageService.upload(any(), anyString())).thenReturn("key");
        when(storageService.getPubliUrl(anyString())).thenReturn("https://s3.com/image.jpg");
        when(imageProcessor.generateThumbnail(any())).thenReturn(thumbnailBytes);
        when(imageProcessor.createMultipartFile(any(), anyString())).thenReturn(thumbnailFile);
        when(imageRepository.findMaxDisplayOrderByListing(listing)).thenReturn(Optional.of(2));
        when(imageRepository.save(any(ListingImage.class))).thenAnswer(inv -> inv.getArgument(0));

        ImageResponse expectedResponse = createImageResponse(
                UUID.randomUUID(),
                "https://s3.com/image.jpg",
                "https://s3.com/thumb.jpg",
                3,
                false,
                mockFile.getSize(),
                "jpg"
        );
        when(imageMapper.toResponse(any(ListingImage.class))).thenReturn(expectedResponse);

        // Act
        ImageResponse result = listingImageService.addImage(listingId, mockFile, false);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isPrimary()).isFalse();
        assertThat(result.displayOrder()).isEqualTo(3);

        verify(imageRepository, never()).removeAllPrimaryFlags(any());
    }

    @Test
    @DisplayName("Should add image with null isPrimary defaulting to false")
    void addImage_WithNullIsPrimary_DefaultsToFalse() throws Exception {
        // Arrange
        byte[] thumbnailBytes = "thumb".getBytes();
        MockMultipartFile thumbnailFile = new MockMultipartFile("thumb", thumbnailBytes);

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        doNothing().when(imageValidator).validate(mockFile);
        when(imageRepository.countByListing(listing)).thenReturn(0L);
        when(storageService.upload(any(), anyString())).thenReturn("key");
        when(storageService.getPubliUrl(anyString())).thenReturn("https://s3.com/image.jpg");
        when(imageProcessor.generateThumbnail(any())).thenReturn(thumbnailBytes);
        when(imageProcessor.createMultipartFile(any(), anyString())).thenReturn(thumbnailFile);
        when(imageRepository.findMaxDisplayOrderByListing(listing)).thenReturn(Optional.empty());
        when(imageRepository.save(any(ListingImage.class))).thenAnswer(inv -> inv.getArgument(0));

        ImageResponse expectedResponse = createImageResponse(
                UUID.randomUUID(),
                "https://s3.com/image.jpg",
                "https://s3.com/thumb.jpg",
                1,
                false,
                mockFile.getSize(),
                "jpg"
        );
        when(imageMapper.toResponse(any(ListingImage.class))).thenReturn(expectedResponse);

        // Act
        ImageResponse result = listingImageService.addImage(listingId, mockFile, null);

        // Assert
        assertThat(result.isPrimary()).isFalse();
        verify(imageRepository, never()).removeAllPrimaryFlags(any());
    }

    @Test
    @DisplayName("Should throw ListingNotFoundException when listing not found")
    void addImage_ListingNotFound_ThrowsException() throws Exception {
        // Arrange
        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> listingImageService.addImage(listingId, mockFile, true))
                .isInstanceOf(ListingNotFoundException.class)
                .hasMessageContaining("Listing not found");

        verify(imageValidator, never()).validate(any());
        verify(storageService, never()).upload(any(), anyString());
    }

    @Test
    @DisplayName("Should throw ListingImageException when validation fails")
    void addImage_ValidationFails_ThrowsException() throws Exception {
        // Arrange
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        doThrow(new ListingImageException("Invalid image format"))
                .when(imageValidator).validate(mockFile);

        // Act & Assert
        assertThatThrownBy(() -> listingImageService.addImage(listingId, mockFile, true))
                .isInstanceOf(ListingImageException.class)
                .hasMessageContaining("Invalid image format");

        verify(storageService, never()).upload(any(), anyString());
    }

    @Test
    @DisplayName("Should throw ListingImageException when max images limit reached")
    void addImage_MaxImagesReached_ThrowsException() throws Exception {
        // Arrange
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        doNothing().when(imageValidator).validate(mockFile);
        when(imageRepository.countByListing(listing)).thenReturn((long) MAX_IMAGES);

        // Act & Assert
        assertThatThrownBy(() -> listingImageService.addImage(listingId, mockFile, true))
                .isInstanceOf(ListingImageException.class)
                .hasMessageContaining("Maximum number of images reached");

        verify(storageService, never()).upload(any(), anyString());
    }

    @Test
    @DisplayName("Should throw ListingImageException when IOException occurs during thumbnail generation")
    void addImage_IOExceptionOnThumbnail_ThrowsException() throws Exception {
        // Arrange
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        doNothing().when(imageValidator).validate(mockFile);
        when(imageRepository.countByListing(listing)).thenReturn(0L);
        when(storageService.upload(eq(mockFile), anyString())).thenReturn("original-key");
        when(storageService.getPubliUrl("original-key")).thenReturn("https://s3.com/original.jpg");
        when(imageProcessor.generateThumbnail(mockFile.getBytes()))
                .thenThrow(new IOException("Failed to generate thumbnail"));

        // Act & Assert
        assertThatThrownBy(() -> listingImageService.addImage(listingId, mockFile, true))
                .isInstanceOf(ListingImageException.class)
                .hasMessageContaining("Failed to upload thumbnail to storage")
                .hasCauseInstanceOf(IOException.class);


        verify(storageService).delete("original-key");
    }

    @Test
    @DisplayName("Should throw ListingImageException when storage upload fails")
    void addImage_StorageUploadFails_ThrowsException() throws Exception {
        // Arrange
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        doNothing().when(imageValidator).validate(mockFile);
        when(imageRepository.countByListing(listing)).thenReturn(0L);
        when(storageService.upload(any(), anyString()))
                .thenThrow(new RuntimeException("S3 upload failed"));

        // Act & Assert
        assertThatThrownBy(() -> listingImageService.addImage(listingId, mockFile, true))
                .isInstanceOf(ListingImageException.class)
                .hasMessageContaining("Failed to upload image to storage");
    }


    @Test
    @DisplayName("Should delete image successfully")
    void deleteImage_Success() {
        // Arrange
        UUID imageId = UUID.randomUUID();
        ListingImage image = ListingImage.builder()
                .id(imageId)
                .listing(listing)
                .storageKey("image-key")
                .build();

        when(imageRepository.findById(imageId)).thenReturn(Optional.of(image));
        doNothing().when(storageService).delete("image-key");
        doNothing().when(imageRepository).delete(image);

        // Act
        listingImageService.deleteImage(listingId, imageId);

        // Assert
        verify(imageRepository).findById(imageId);
        verify(storageService).delete("image-key");
        verify(imageRepository).delete(image);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent image")
    void deleteImage_ImageNotFound_ThrowsException() {
        // Arrange
        UUID imageId = UUID.randomUUID();
        when(imageRepository.findById(imageId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> listingImageService.deleteImage(listingId, imageId))
                .isInstanceOf(ListingImageException.class)
                .hasMessageContaining("Image not found");

        verify(storageService, never()).delete(anyString());
        verify(imageRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should throw exception when image belongs to different listing")
    void deleteImage_ImageBelongsToDifferentListing_ThrowsException() {
        // Arrange
        UUID imageId = UUID.randomUUID();
        UUID differentListingId = UUID.randomUUID();
        MaterialListing differentListing = MaterialListing.builder()
                .id(differentListingId)
                .title("Different Listing")
                .build();

        ListingImage image = ListingImage.builder()
                .id(imageId)
                .listing(differentListing)
                .storageKey("key")
                .build();

        when(imageRepository.findById(imageId)).thenReturn(Optional.of(image));

        // Act & Assert
        assertThatThrownBy(() -> listingImageService.deleteImage(listingId, imageId))
                .isInstanceOf(ListingImageException.class)
                .hasMessageContaining("Image does not belong to this listing");

        verify(storageService, never()).delete(anyString());
    }

    // ==================== SET PRIMARY IMAGE TESTS ====================

    @Test
    @DisplayName("Should set primary image successfully")
    void setPrimaryImage_Success() {
        // Arrange
        UUID imageId = UUID.randomUUID();
        ListingImage image = ListingImage.builder()
                .id(imageId)
                .listing(listing)
                .isPrimary(false)
                .build();

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(imageRepository.findById(imageId)).thenReturn(Optional.of(image));
        when(imageRepository.removeAllPrimaryFlags(listing)).thenReturn(1);
        when(imageRepository.save(image)).thenReturn(image);

        // Act
        listingImageService.setPrimaryImage(listingId, imageId);

        // Assert
        assertThat(image.getIsPrimary()).isTrue();
        verify(imageRepository).removeAllPrimaryFlags(listing);
        verify(imageRepository).save(image);
    }

    @Test
    @DisplayName("Should throw exception when setting primary for non-existent image")
    void setPrimaryImage_ImageNotFound_ThrowsException() {
        // Arrange
        UUID imageId = UUID.randomUUID();
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(imageRepository.findById(imageId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> listingImageService.setPrimaryImage(listingId, imageId))
                .isInstanceOf(ListingImageException.class)
                .hasMessageContaining("Image not found");

        verify(imageRepository, never()).save(any());
    }

    // ==================== REORDER IMAGES TESTS ====================

    @Test
    @DisplayName("Should reorder images successfully")
    void reorderImages_Success() {
        // Arrange
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        ListingImage img1 = ListingImage.builder().id(id1).listing(listing).displayOrder(1).build();
        ListingImage img2 = ListingImage.builder().id(id2).listing(listing).displayOrder(2).build();
        ListingImage img3 = ListingImage.builder().id(id3).listing(listing).displayOrder(3).build();

        List<UUID> newOrder = Arrays.asList(id3, id1, id2);
        List<ListingImage> currentImages = Arrays.asList(img1, img2, img3);

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(imageRepository.findByListingOrderByDisplayOrderAsc(listing)).thenReturn(currentImages);
        when(imageRepository.save(any(ListingImage.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        listingImageService.reorderImages(listingId, newOrder);

        // Assert
        assertThat(img3.getDisplayOrder()).isEqualTo(1);
        assertThat(img1.getDisplayOrder()).isEqualTo(2);
        assertThat(img2.getDisplayOrder()).isEqualTo(3);
        verify(imageRepository, times(3)).save(any(ListingImage.class));
    }

    @Test
    @DisplayName("Should throw exception when reordering with mismatched image count")
    void reorderImages_MismatchedCount_ThrowsException() {
        // Arrange
        List<ListingImage> currentImages = Arrays.asList(
                ListingImage.builder().id(UUID.randomUUID()).listing(listing).build(),
                ListingImage.builder().id(UUID.randomUUID()).listing(listing).build());

        List<UUID> newOrder = Collections.singletonList(UUID.randomUUID());

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(imageRepository.findByListingOrderByDisplayOrderAsc(listing)).thenReturn(currentImages);

        // Act & Assert
        assertThatThrownBy(() -> listingImageService.reorderImages(listingId, newOrder))
                .isInstanceOf(ListingImageException.class)
                .hasMessageContaining("Image count mismatch");

        verify(imageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when reordering with non-existent image ID")
    void reorderImages_NonExistentImageId_ThrowsException() {
        // Arrange
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID nonExistentId = UUID.randomUUID();

        List<ListingImage> currentImages = Arrays.asList(
                ListingImage.builder().id(id1).listing(listing).build(),
                ListingImage.builder().id(id2).listing(listing).build());

        List<UUID> newOrder = Arrays.asList(id1, nonExistentId);

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(imageRepository.findByListingOrderByDisplayOrderAsc(listing)).thenReturn(currentImages);

        // Act & Assert
        assertThatThrownBy(() -> listingImageService.reorderImages(listingId, newOrder))
                .isInstanceOf(ListingImageException.class)
                .hasMessageContaining("Image not found");
    }

    // ==================== GET LISTING IMAGES TESTS ====================

    @Test
    @DisplayName("Should get all images for a listing")
    void getListingImages_Success() {
        // Arrange
        ListingImage img1 = ListingImage.builder()
                .id(UUID.randomUUID())
                .listing(listing)
                .displayOrder(1)
                .build();

        ListingImage img2 = ListingImage.builder()
                .id(UUID.randomUUID())
                .listing(listing)
                .displayOrder(2)
                .build();

        List<ListingImage> images = Arrays.asList(img1, img2);

        ImageResponse response1 = createImageResponse(
                img1.getId(), "url1", "thumb1", 1, true, 1000L, "jpg");
        ImageResponse response2 = createImageResponse(
                img2.getId(), "url2", "thumb2", 2, false, 2000L, "png");

        when(imageRepository.findByListingIdOrderByDisplayOrderAsc(listingId)).thenReturn(images);
        when(imageMapper.toResponse(img1)).thenReturn(response1);
        when(imageMapper.toResponse(img2)).thenReturn(response2);

        // Act
        List<ImageResponse> result = listingImageService.getListingImages(listingId);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).imageUrl()).isEqualTo("url1");
        assertThat(result.get(1).imageUrl()).isEqualTo("url2");
        verify(imageRepository).findByListingIdOrderByDisplayOrderAsc(listingId);
    }

    @Test
    @DisplayName("Should return empty list when no images exist")
    void getListingImages_EmptyList_Success() {
        // Arrange
        when(imageRepository.findByListingIdOrderByDisplayOrderAsc(listingId))
                .thenReturn(Collections.emptyList());

        // Act
        List<ImageResponse> result = listingImageService.getListingImages(listingId);

        // Assert
        assertThat(result).isEmpty();
        verify(imageRepository).findByListingIdOrderByDisplayOrderAsc(listingId);
    }
}
