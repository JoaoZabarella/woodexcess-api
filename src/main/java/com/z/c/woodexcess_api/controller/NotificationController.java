package com.z.c.woodexcess_api.controller;

import com.z.c.woodexcess_api.dto.notification.NotificationResponse;
import com.z.c.woodexcess_api.model.enums.NotificationType;
import com.z.c.woodexcess_api.security.CustomUserDetails;
import com.z.c.woodexcess_api.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notifications", description = "User notification management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get user notifications",
            description = "Get paginated list of notifications for authenticated user (filterable by read status and type)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully")
            }
    )
    public ResponseEntity<Page<NotificationResponse>> getUserNotifications(
            @Parameter(description = "Filter by read status (true/false)")
            @RequestParam(required = false) Boolean isRead,

            @Parameter(description = "Filter by notification type")
            @RequestParam(required = false) NotificationType type,

            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.debug("GET /api/notifications - User: {}, isRead: {}, type: {}",
                userDetails.getUsername(), isRead, type);

        Page<NotificationResponse> response = notificationService.getUserNotifications(
                userDetails.getId(), isRead, type, pageable);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{notificationId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get notification by ID",
            description = "Get specific notification details",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Notification retrieved successfully"),
                    @ApiResponse(responseCode = "404", description = "Notification not found")
            }
    )
    public ResponseEntity<NotificationResponse> getNotification(
            @Parameter(description = "Notification ID")
            @PathVariable UUID notificationId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.debug("GET /api/notifications/{} - User: {}", notificationId, userDetails.getUsername());

        NotificationResponse response = notificationService.getNotificationById(notificationId, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/recent")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get recent notifications",
            description = "Get most recent notifications (default: 10 latest)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Recent notifications retrieved successfully")
            }
    )
    public ResponseEntity<List<NotificationResponse>> getRecentNotifications(
            @Parameter(description = "Number of notifications to retrieve (default: 10)")
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.debug("GET /api/notifications/recent - User: {}, limit: {}", userDetails.getUsername(), limit);

        List<NotificationResponse> response = notificationService.getRecentNotifications(userDetails.getId(), limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get unread notifications count",
            description = "Get total number of unread notifications for user (for UI badges)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Count retrieved successfully")
            }
    )
    public ResponseEntity<UnreadCountResponse> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.debug("GET /api/notifications/unread-count - User: {}", userDetails.getUsername());

        long count = notificationService.countUnreadNotifications(userDetails.getId());
        return ResponseEntity.ok(new UnreadCountResponse(count));
    }

    @PatchMapping("/{notificationId}/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Mark notification as read",
            description = "Mark a specific notification as read",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Notification marked as read"),
                    @ApiResponse(responseCode = "404", description = "Notification not found")
            }
    )
    public ResponseEntity<NotificationResponse> markAsRead(
            @Parameter(description = "Notification ID")
            @PathVariable UUID notificationId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("PATCH /api/notifications/{}/read - User: {}", notificationId, userDetails.getUsername());

        NotificationResponse response = notificationService.markAsRead(notificationId, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{notificationId}/unread")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Mark notification as unread",
            description = "Mark a specific notification as unread",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Notification marked as unread"),
                    @ApiResponse(responseCode = "404", description = "Notification not found")
            }
    )
    public ResponseEntity<NotificationResponse> markAsUnread(
            @Parameter(description = "Notification ID")
            @PathVariable UUID notificationId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("PATCH /api/notifications/{}/unread - User: {}", notificationId, userDetails.getUsername());

        NotificationResponse response = notificationService.markAsUnread(notificationId, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/mark-all-read")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Mark all notifications as read",
            description = "Mark all unread notifications as read for authenticated user",
            responses = {
                    @ApiResponse(responseCode = "204", description = "All notifications marked as read")
            }
    )
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("PATCH /api/notifications/mark-all-read - User: {}", userDetails.getUsername());

        notificationService.markAllAsRead(userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{notificationId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Delete notification",
            description = "Delete a specific notification",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Notification deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "Notification not found")
            }
    )
    public ResponseEntity<Void> deleteNotification(
            @Parameter(description = "Notification ID")
            @PathVariable UUID notificationId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("DELETE /api/notifications/{} - User: {}", notificationId, userDetails.getUsername());

        notificationService.deleteNotification(notificationId, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Delete all read notifications",
            description = "Delete all read notifications for authenticated user",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Read notifications deleted successfully")
            }
    )
    public ResponseEntity<Void> deleteAllRead(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("DELETE /api/notifications/read - User: {}", userDetails.getUsername());

        notificationService.deleteAllRead(userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Delete all notifications",
            description = "Delete ALL notifications for authenticated user (read and unread)",
            responses = {
                    @ApiResponse(responseCode = "204", description = "All notifications deleted successfully")
            }
    )
    public ResponseEntity<Void> deleteAllNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("DELETE /api/notifications - User: {}", userDetails.getUsername());

        notificationService.deleteAll(userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    public record UnreadCountResponse(
            @Parameter(description = "Number of unread notifications")
            long count
    ) {}
}
