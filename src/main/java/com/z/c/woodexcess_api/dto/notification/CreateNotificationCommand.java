package com.z.c.woodexcess_api.dto.notification;

import com.z.c.woodexcess_api.model.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.Map;
import java.util.UUID;

@Builder
public record CreateNotificationCommand(
        @NotNull(message = "User ID cannot be null")
        UUID userId,

        @NotNull(message = "Notification type cannot be null")
        NotificationType type,

        @NotBlank(message = "Title cannot be blank")
        @Size(max = 100, message = "Title cannot exceed 100 characters")
        String title,

        @NotBlank(message = "Message cannot be blank")
        @Size(max = 500, message = "Message cannot exceed 500 characters")
        String message,

        @Size(max = 255, message = "Link URL cannot exceed 255 characters")
        String linkUrl,

        Map<String, Object> metadata
) {
}
