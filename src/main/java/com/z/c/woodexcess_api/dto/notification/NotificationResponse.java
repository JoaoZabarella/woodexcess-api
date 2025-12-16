package com.z.c.woodexcess_api.dto.notification;

import com.z.c.woodexcess_api.model.enums.NotificationType;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Builder
public record NotificationResponse(
        UUID id,
        NotificationType type,
        String title,
        String message,
        String linkUrl,
        Map<String, Object> metadata,
        Boolean isRead,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {}