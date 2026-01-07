// java
    package com.z.c.woodexcess_api.service;

    import com.z.c.woodexcess_api.dto.notification.CreateNotificationCommand;
    import com.z.c.woodexcess_api.dto.notification.NotificationResponse;
    import com.z.c.woodexcess_api.exception.ResourceNotFoundException;
    import com.z.c.woodexcess_api.mapper.NotificationMapper;
    import com.z.c.woodexcess_api.model.Notification;
    import com.z.c.woodexcess_api.model.User;
    import com.z.c.woodexcess_api.model.enums.NotificationType;
    import com.z.c.woodexcess_api.repository.NotificationRepository;
    import com.z.c.woodexcess_api.repository.UserRepository;
    import jakarta.validation.Valid;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.PageRequest;
    import org.springframework.data.domain.Pageable;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;
    import org.springframework.validation.annotation.Validated;

    import java.time.LocalDateTime;
    import java.util.List;
    import java.util.UUID;

    @Service
    @Validated
    @RequiredArgsConstructor
    @Slf4j
    public class NotificationService {

        private final NotificationRepository notificationRepository;
        private final UserRepository userRepository;

        @Transactional
        public NotificationResponse createNotification(@Valid CreateNotificationCommand command) {
            log.info("Creating notification for user: {}, type: {}", command.userId(), command.type());

            User user = userRepository.findById(command.userId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + command.userId()));

            Notification notification = Notification.builder()
                    .user(user)
                    .type(command.type())
                    .title(command.title())
                    .message(command.message())
                    .linkUrl(command.linkUrl())
                    .metadata(command.metadata())
                    .isRead(false)
                    .build();

            Notification saved = notificationRepository.save(notification);

            log.info("Notification created successfully: {}", saved.getId());
            return NotificationMapper.toResponse(saved);
        }

        @Transactional
        public NotificationResponse markAsRead(UUID notificationId, UUID userId) {
            log.info("Marking notification as read: {}, user: {}", notificationId, userId);

            Notification notification = notificationRepository.findById(notificationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));

            if (!notification.getUser().getId().equals(userId)) {
                throw new ResourceNotFoundException("Notification not found: " + notificationId);
            }

            if (!notification.isRead()) {
                notification.markAsRead();
                notificationRepository.save(notification);
            }

            log.info("Notification marked as read: {}", notificationId);
            return NotificationMapper.toResponse(notification);
        }

        @Transactional
        public void markAllAsRead(UUID userId) {
            log.info("Marking all notifications as read for user: {}", userId);

            List<Notification> unreadNotifications = notificationRepository.findByUserIdAndIsReadFalse(userId);

            unreadNotifications.forEach(Notification::markAsRead);

            notificationRepository.saveAll(unreadNotifications);

            log.info("Marked {} notifications as read for user: {}", unreadNotifications.size(), userId);
        }

        @Transactional
        public NotificationResponse markAsUnread(UUID notificationId, UUID userId) {
            log.info("Marking notification as unread: {}, user: {}", notificationId, userId);

            Notification notification = notificationRepository.findById(notificationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));

            if (!notification.getUser().getId().equals(userId)) {
                throw new ResourceNotFoundException("Notification not found: " + notificationId);
            }

            if (notification.isRead()) {
                notification.markAsUnread();
                notificationRepository.save(notification);
            }

            log.info("Notification marked as unread: {}", notificationId);
            return NotificationMapper.toResponse(notification);
        }

        @Transactional
        public void deleteNotification(UUID notificationId, UUID userId) {
            log.info("Deleting notification: {}, user: {}", notificationId, userId);

            Notification notification = notificationRepository.findById(notificationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));

            if (!notification.getUser().getId().equals(userId)) {
                throw new ResourceNotFoundException("Notification not found: " + notificationId);
            }

            notificationRepository.delete(notification);

            log.info("Notification deleted: {}", notificationId);
        }

        @Transactional
        public void deleteAllRead(UUID userId) {
            log.info("Deleting all read notifications for user: {}", userId);

            List<Notification> readNotifications = notificationRepository.findByUserIdAndIsReadTrue(userId);
            notificationRepository.deleteAll(readNotifications);

            log.info("Deleted {} read notifications for user: {}", readNotifications.size(), userId);
        }

        @Transactional
        public void deleteAll(UUID userId) {
            log.info("Deleting all notifications for user: {}", userId);

            notificationRepository.deleteByUserId(userId);

            log.info("All notifications deleted for user: {}", userId);
        }

        @Transactional(readOnly = true)
        public NotificationResponse getNotificationById(UUID notificationId, UUID userId) {
            log.debug("Fetching notification: {}, user: {}", notificationId, userId);

            Notification notification = notificationRepository.findById(notificationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));

            if (!notification.getUser().getId().equals(userId)) {
                throw new ResourceNotFoundException("Notification not found: " + notificationId);
            }

            return NotificationMapper.toResponse(notification);
        }

        @Transactional(readOnly = true)
        public Page<NotificationResponse> getUserNotifications(
                UUID userId,
                Boolean isRead,
                NotificationType type,
                Pageable pageable
        ) {
            log.debug("Fetching notifications for user: {}, isRead: {}, type: {}", userId, isRead, type);

            Page<Notification> notifications;

            if (isRead != null && type != null) {
                notifications = notificationRepository.findByUserIdAndIsReadAndTypeOrderByCreatedAtDesc(
                        userId, isRead, type, pageable
                );
            } else if (isRead != null) {
                notifications = notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(
                        userId, isRead, pageable
                );
            } else if (type != null) {
                notifications = notificationRepository.findByUserIdAndTypeOrderByCreatedAtDesc(
                        userId, type, pageable
                );
            } else {
                notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
            }

            return notifications.map(NotificationMapper::toResponse);
        }

        @Transactional(readOnly = true)
        public long countUnreadNotifications(UUID userId) {
            return notificationRepository.countByUserIdAndIsReadFalse(userId);
        }

        @Transactional(readOnly = true)
        public List<NotificationResponse> getRecentNotifications(UUID userId, int limit) {
            log.debug("Fetching recent {} notifications for user: {}", limit, userId);


            PageRequest pageRequest = PageRequest.of(0, Math.max(1, limit));
            List<Notification> notifications = notificationRepository
                    .findByUserIdOrderByCreatedAtDesc(userId, pageRequest)
                    .getContent();

            return notifications.stream()
                    .map(NotificationMapper::toResponse)
                    .toList();
        }

        @Transactional
        public void deleteOldReadNotifications(int daysOld) {
            log.info("Deleting read notifications older than {} days", daysOld);

            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
            int deleted = notificationRepository.deleteByIsReadTrueAndCreatedAtBefore(cutoffDate);

            log.info("Deleted {} old read notifications", deleted);
        }
    }