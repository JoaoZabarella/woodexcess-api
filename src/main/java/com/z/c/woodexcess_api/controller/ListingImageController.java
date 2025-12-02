package com.z.c.woodexcess_api.controller;

import com.z.c.woodexcess_api.dto.listing.ImageResponse;
import com.z.c.woodexcess_api.service.listing.ListingImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/listings/{listingId}/images")
@Tag(name = "Listing Images", description = "Gerenciamento de imagens de listings de materiais")
public class ListingImageController {

    private static final Logger logger = LoggerFactory.getLogger(ListingImageController.class);
    private final ListingImageService imageService;

    public ListingImageController(ListingImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upload de imagem para listing")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Imagem enviada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Arquivo inválido"),
            @ApiResponse(responseCode = "404", description = "Listing não encontrado")
    })
    public ResponseEntity<ImageResponse> uploadImage(
            @PathVariable UUID listingId,
            @RequestParam("file") @NotNull MultipartFile file,
            @RequestParam(value = "isPrimary", required = false) Boolean isPrimary) {

        logger.info("Uploading image to listing {}", listingId);
        ImageResponse response = imageService.addImage(listingId, file, isPrimary);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar imagens do listing")
    public ResponseEntity<List<ImageResponse>> getImages(@PathVariable UUID listingId) {
        List<ImageResponse> images = imageService.getListingImages(listingId);
        return ResponseEntity.ok(images);
    }

    @DeleteMapping("/{imageId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Deletar imagem")
    public ResponseEntity<Void> deleteImage(
            @PathVariable UUID listingId,
            @PathVariable UUID imageId) {

        logger.info("Deleting image {} from listing {}", imageId, listingId);
        imageService.deleteImage(listingId, imageId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{imageId}/primary")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Definir imagem como principal")
    public ResponseEntity<Void> setPrimaryImage(
            @PathVariable UUID listingId,
            @PathVariable UUID imageId) {

        logger.info("Setting primary image {} for listing {}", imageId, listingId);
        imageService.setPrimaryImage(listingId, imageId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/reorder")
    @PreAuthorize("isAuthenticated()")

    @Operation(summary = "Reordenar imagens")
    public ResponseEntity<Void> reorderImages(
            @PathVariable UUID listingId,
            @RequestBody List<UUID> imageIds) {

        logger.info("Reordering {} images for listing {}", imageIds.size(), listingId);
        imageService.reorderImages(listingId, imageIds);
        return ResponseEntity.ok().build();
    }
}
