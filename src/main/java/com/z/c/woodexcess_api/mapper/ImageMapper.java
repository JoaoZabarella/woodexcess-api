package com.z.c.woodexcess_api.mapper;

import com.z.c.woodexcess_api.dto.listing.ImageResponse;
import com.z.c.woodexcess_api.model.ListingImage;
import org.springframework.stereotype.Component;


@Component
public class ImageMapper {


    public ImageResponse toResponse(ListingImage image) {
        if (image == null) {
            return null;
        }

        return ImageResponse.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .thumbnailUrl(image.getThumbnailUrl())
                .displayOrder(image.getDisplayOrder())
                .fileSize(image.getFileSize())
                .fileExtension(image.getFileExtension())
                .isPrimary(image.getIsPrimary())
                .uploadedAt(image.getUploadedAt())
                .build();
    }
}
