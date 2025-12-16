package com.z.c.woodexcess_api.mapper;

import com.z.c.woodexcess_api.dto.notification.NotificationResponse;
import com.z.c.woodexcess_api.model.Notification;
import lombok.experimental.UtilityClass;

@UtilityClass
public class NotificationMapper {

    public NotificationResponse toResponse(Notification notification) {
        if (notification == null) {
            return null;
        }

        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .linkUrl(notification.getLinkUrl())
                .metadata(notification.getMetadata())
                .isRead(notification.isRead())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
