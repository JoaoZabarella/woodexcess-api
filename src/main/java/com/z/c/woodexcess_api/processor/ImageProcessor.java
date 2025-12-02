package com.z.c.woodexcess_api.processor;

import com.z.c.woodexcess_api.exception.listing.ListingImageException;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
public class ImageProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ImageProcessor.class);

    @Value("${app.listing.thumbnail.width:300}")
    private int thumbnailWidth;

    @Value("${app.listing.thumbnail.height:300}")
    private int thumbnailHeight;

    public byte[] generateThumbnail(byte[] imageBytes) throws IOException {
        logger.debug("Generating thumbnail: inputSize={} bytes, targetSize={}x{} pixels",
                imageBytes.length, thumbnailWidth, thumbnailHeight);

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Thumbnails.of(inputStream)
                    .size(thumbnailWidth, thumbnailHeight)
                    .outputFormat("jpg")
                    .outputQuality(0.85)
                    .toOutputStream(outputStream);

            byte[] result = outputStream.toByteArray();
            logger.debug("Thumbnail generated successfully: outputSize={} bytes", result.length);
            return result;

        } catch (IOException e) {
            logger.error("Failed to generate thumbnail: {}", e.getMessage(), e);
            throw new ListingImageException("Failed to generate thumbnail", e);
        }
    }

    public MultipartFile createMultipartFile(byte[] bytes, String filename) {
        logger.debug("Creating MultipartFile: filename={}, size={} bytes", filename, bytes.length);

        return new MultipartFile() {
            @Override
            public String getName() {
                return "file";
            }

            @Override
            public String getOriginalFilename() {
                return filename;
            }

            @Override
            public String getContentType() {
                return "image/jpeg";
            }

            @Override
            public boolean isEmpty() {
                return bytes.length == 0;
            }

            @Override
            public long getSize() {
                return bytes.length;
            }

            @Override
            public byte[] getBytes() {
                return bytes;
            }

            @Override
            public java.io.InputStream getInputStream() {
                return new ByteArrayInputStream(bytes);
            }

            @Override
            public void transferTo(java.io.File dest) throws IOException {
                java.nio.file.Files.write(dest.toPath(), bytes);
            }
        };
    }
}
