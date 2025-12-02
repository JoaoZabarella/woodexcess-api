package com.z.c.woodexcess_api.controller;

import com.z.c.woodexcess_api.dto.listing.ImageResponse;
import com.z.c.woodexcess_api.exception.listing.ListingImageException;
import com.z.c.woodexcess_api.exception.listing.ListingNotFoundException;
import com.z.c.woodexcess_api.service.listing.ListingImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/listings/{listingId}")
@RequiredArgsConstructor
@Slf4j
public class ListingImageController {

    private final ListingImageService imageService;

    @PostMapping("/images")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upload image to listing", description = "Upload a new image to the specified listing")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Image uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or image validation failed"),
            @ApiResponse(responseCode = "404", description = "Listing not found"),
            @ApiResponse(responseCode = "409", description = "Maximum images limit reached")
    })
    public ResponseEntity<ImageResponse> uploadImage(
            @PathVariable UUID listingId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Boolean isPrimary) {

        log.info("Upload image request for listing {}", listingId);

        ImageResponse response = imageService.addImage(listingId, file, isPrimary);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/images")
    @Operation(summary = "Get all images from listing")
    public ResponseEntity<List<ImageResponse>> getImages(@PathVariable UUID listingId) {
        log.info("Get images request for listing {}", listingId);
        List<ImageResponse> images = imageService.getListingImages(listingId);
        return ResponseEntity.ok(images);
    }


    @PutMapping("/images/{imageId}/primary")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Set image as primary")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Primary image set successfully"),
            @ApiResponse(responseCode = "404", description = "Image or listing not found")
    })
    public ResponseEntity<Void> setPrimaryImage(
            @PathVariable UUID listingId,
            @PathVariable UUID imageId) {

        log.info("Set primary image {} for listing {}", imageId, listingId);
        imageService.setPrimaryImage(listingId, imageId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/images/reorder")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Reorder images")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Images reordered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid image IDs or count mismatch"),
            @ApiResponse(responseCode = "404", description = "Listing not found")
    })
    public ResponseEntity<Void> reorderImages(
            @PathVariable UUID listingId,
            @RequestBody List<UUID> imageIds) {

        log.info("Reorder {} images for listing {}", imageIds.size(), listingId);
        imageService.reorderImages(listingId, imageIds);
        return ResponseEntity.ok().build();
    }


    @DeleteMapping("/images/{imageId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete image from listing")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Image deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Image or listing not found")
    })
    public ResponseEntity<Void> deleteImage(
            @PathVariable UUID listingId,
            @PathVariable UUID imageId) {

        log.info("Delete image {} from listing {}", imageId, listingId);
        imageService.deleteImage(listingId, imageId);
        return ResponseEntity.noContent().build();
    }
}
