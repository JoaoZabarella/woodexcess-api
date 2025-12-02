package com.z.c.woodexcess_api.service.listing;

import com.z.c.woodexcess_api.dto.listing.ImageResponse;
import com.z.c.woodexcess_api.exception.listing.ListingImageException;
import com.z.c.woodexcess_api.exception.listing.ListingNotFoundException;
import com.z.c.woodexcess_api.mapper.ImageMapper;
import com.z.c.woodexcess_api.model.ListingImage;
import com.z.c.woodexcess_api.model.MaterialListing;
import com.z.c.woodexcess_api.processor.ImageProcessor;
import com.z.c.woodexcess_api.repository.ListingImageRepository;
import com.z.c.woodexcess_api.repository.MaterialListingRepository;
import com.z.c.woodexcess_api.service.storage.StorageService;
import com.z.c.woodexcess_api.util.FileUtils;
import com.z.c.woodexcess_api.validator.ImageValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class ListingImageService {

    private static final Logger logger = LoggerFactory.getLogger(ListingImageService.class);

    private final ListingImageRepository imageRepository;
    private final MaterialListingRepository listingRepository;
    private final StorageService storageService;
    private final ImageValidator imageValidator;
    private final ImageProcessor imageProcessor;
    private final ImageMapper imageMapper;

    @Value("${app.listing.max-images:10}")
    private int maxImages;

    public ListingImageService(
            ListingImageRepository imageRepository,
            MaterialListingRepository listingRepository,
            StorageService storageService,
            ImageValidator imageValidator,
            ImageProcessor imageProcessor,
            ImageMapper imageMapper) {
        this.imageRepository = imageRepository;
        this.listingRepository = listingRepository;
        this.storageService = storageService;
        this.imageValidator = imageValidator;
        this.imageProcessor = imageProcessor;
        this.imageMapper = imageMapper;
    }

    @Transactional
    public ImageResponse addImage(UUID listingId, MultipartFile file, Boolean isPrimary) {
        logger.info("Starting image upload for listing: {}", listingId);
        logger.debug("File details: name={}, size={} bytes, type={}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        try {

            logger.debug("Finding listing by ID: {}", listingId);
            MaterialListing listing = findListingOrThrow(listingId);
            logger.debug("Listing found: {}", listing.getTitle());

            // 2. Validate image
            logger.debug("Validating image file");
            imageValidator.validate(file);
            logger.debug("Image validation passed");

            logger.debug("Checking image count limit (max: {})", maxImages);
            validateImageLimit(listing);
            logger.debug("Image limit check passed");

            logger.info("Uploading original image to S3");
            String imageKey = storageService.upload(file, "listings/images");
            logger.info("Original image uploaded successfully: {}", imageKey);

            String imageUrl = storageService.getPubliUrl(imageKey);
            logger.debug("Generated image URL: {}", imageUrl);

            logger.info("Generating thumbnail from uploaded image");
            byte[] thumbnailBytes = imageProcessor.generateThumbnail(file.getBytes());
            logger.info("Thumbnail generated successfully: {} bytes", thumbnailBytes.length);

            MultipartFile thumbnailFile = imageProcessor.createMultipartFile(
                    thumbnailBytes,
                    "thumbnail-" + file.getOriginalFilename());

            logger.info("Uploading thumbnail to S3");
            String thumbnailKey = storageService.upload(thumbnailFile, "listings/thumbnails");
            logger.info("Thumbnail uploaded successfully: {}", thumbnailKey);

            String thumbnailUrl = storageService.getPubliUrl(thumbnailKey);
            logger.debug("Generated thumbnail URL: {}", thumbnailUrl);

            Integer displayOrder = calculateNextDisplayOrder(listing);
            logger.debug("Calculated display order: {}", displayOrder);

            if (Boolean.TRUE.equals(isPrimary)) {
                logger.debug("Setting as primary image, removing existing primary flags");
                removePrimaryFlag(listing);
            }

            logger.debug("Saving image entity to database");
            ListingImage image = buildListingImage(
                    listing, imageUrl, thumbnailUrl, imageKey,
                    displayOrder, file, isPrimary);
            imageRepository.save(image);

            logger.info("Image added successfully to listing {}: {}", listingId, imageKey);

            return imageMapper.toResponse(image);

        } catch (IOException e) {
            logger.error("IOException during image processing for listing {}: {}", listingId, e.getMessage(), e);
            throw new ListingNotFoundException(e.getMessage());
        } catch (ListingImageException e) {
            logger.error("ListingImageException for listing {}: {}", listingId, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during image processing for listing {}: {}", listingId, e.getMessage(), e);
            throw new ListingNotFoundException(e.getMessage());
        }
    }

    @Transactional
    public void deleteImage(UUID listingId, UUID imageId) {
        logger.info("Deleting image {} from listing {}", imageId, listingId);

        ListingImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ListingImageException("Image not found"));

        validateImageBelongsToListing(image, listingId);

        storageService.delete(image.getStorageKey());
        imageRepository.delete(image);

        logger.info("Image deleted successfully from listing {}: {}", listingId, imageId);
    }

    @Transactional
    public void setPrimaryImage(UUID listingId, UUID imageId) {
        logger.info("Setting primary image {} for listing {}", imageId, listingId);

        MaterialListing listing = findListingOrThrow(listingId);
        ListingImage image = findImageOrThrow(imageId);

        validateImageBelongsToListing(image, listingId);

        removePrimaryFlag(listing);
        image.setIsPrimary(true);
        imageRepository.save(image);

        logger.info("Primary image set successfully for listing {}: {}", listingId, imageId);
    }

    @Transactional
    public void reorderImages(UUID listingId, List<UUID> imageIds) {
        logger.info("Reordering {} images for listing {}", imageIds.size(), listingId);

        MaterialListing listing = findListingOrThrow(listingId);
        List<ListingImage> images = imageRepository.findByListingOrderByDisplayOrderAsc(listing);

        validateImageCount(images, imageIds);

        for (int i = 0; i < imageIds.size(); i++) {
            ListingImage image = findImageInList(images, imageIds.get(i));
            image.setDisplayOrder(i + 1);
            imageRepository.save(image);
        }

        logger.info("Images reordered successfully for listing {}", listingId);
    }

    @Transactional(readOnly = true)
    public List<ImageResponse> getListingImages(UUID listingId) {
        logger.debug("Retrieving images for listing {}", listingId);
        return imageRepository.findByListingIdOrderByDisplayOrderAsc(listingId)
                .stream()
                .map(imageMapper::toResponse)
                .toList();
    }

    private MaterialListing findListingOrThrow(UUID listingId) {
        return listingRepository.findById(listingId)
                .orElseThrow(() -> new ListingNotFoundException("Listing not found: " + listingId));
    }

    private ListingImage findImageOrThrow(UUID imageId) {
        return imageRepository.findById(imageId)
                .orElseThrow(() -> new ListingImageException("Image not found: " + imageId));
    }

    private void validateImageLimit(MaterialListing listing) {
        long currentCount = imageRepository.countByListing(listing);
        if (currentCount >= maxImages) {
            throw new ListingImageException("Maximum number of images reached: " + maxImages);
        }
    }

    private void validateImageBelongsToListing(ListingImage image, UUID listingId) {
        if (!image.getListing().getId().equals(listingId)) {
            throw new ListingImageException("Image does not belong to this listing");
        }
    }

    private void validateImageCount(List<ListingImage> images, List<UUID> imageIds) {
        if (images.size() != imageIds.size()) {
            throw new ListingImageException(
                    String.format("Image count mismatch: expected %d, got %d",
                            images.size(), imageIds.size()));
        }
    }

    private ListingImage findImageInList(List<ListingImage> images, UUID imageId) {
        return images.stream()
                .filter(img -> img.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new ListingImageException("Image not found: " + imageId));
    }

    private Integer calculateNextDisplayOrder(MaterialListing listing) {
        return imageRepository.findMaxDisplayOrderByListing(listing)
                .orElse(0) + 1;
    }

    @Transactional
    protected void removePrimaryFlag(MaterialListing listing) {
        logger.debug("Removing primary flag from existing images for listing");

        int updated = imageRepository.removeAllPrimaryFlags(listing);

        if (updated > 0) {
            logger.debug("Primary flag removed from {} images", updated);
        } else {
            logger.debug("No primary images found to update");
        }

    }

    private ListingImage buildListingImage(
            MaterialListing listing,
            String imageUrl,
            String thumbnailUrl,
            String imageKey,
            Integer displayOrder,
            MultipartFile file,
            Boolean isPrimary) {

        return ListingImage.builder()
                .listing(listing)
                .imageUrl(imageUrl)
                .thumbnailUrl(thumbnailUrl)
                .storageKey(imageKey)
                .displayOrder(displayOrder)
                .fileSize(file.getSize())
                .fileExtension(FileUtils.getExtension(file.getOriginalFilename()))
                .isPrimary(isPrimary != null ? isPrimary : false)
                .build();
    }
}
