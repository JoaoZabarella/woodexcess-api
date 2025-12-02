package com.z.c.woodexcess_api.validator;


import com.z.c.woodexcess_api.exception.listing.ListingImageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class ImageValidator {

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp");

    @Value("${app.listing.max-file-size:5242880}")
    private long maxFileSize;


    public void validate(MultipartFile file) {
        validateNotEmpty(file);
        validateFileSize(file);
        validateExtension(file);
        validateImageIntegrity(file);
    }

    public void validateNotEmpty(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ListingImageException("File is empty or null");
        }
    }


    public void validateFileSize(MultipartFile file) {
        if (file.getSize() > maxFileSize) {
            throw new ListingImageException(
                    String.format("File size (%d bytes) exceeds maximum allowed (%d bytes)",
                            file.getSize(), maxFileSize)
            );
        }

        if (file.getSize() == 0) {
            throw new ListingImageException("File size is 0 bytes");
        }
    }


    public void validateExtension(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new ListingImageException("Filename is required");
        }

        String extension = extractExtension(filename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ListingImageException(
                    String.format("Invalid file type '%s'. Allowed: %s", extension, ALLOWED_EXTENSIONS)
            );
        }
    }


    public void validateImageIntegrity(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));

            if (image == null) {
                throw new ListingImageException("File is not a valid image (cannot decode)");
            }


            if (image.getWidth() < 100 || image.getHeight() < 100) {
                throw new ListingImageException("Image dimensions too small (min 100x100)");
            }

        } catch (IOException e) {
            throw new ListingImageException("Failed to read image file", e);
        }
    }


    private String extractExtension(String filename) {
        if (!filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}

