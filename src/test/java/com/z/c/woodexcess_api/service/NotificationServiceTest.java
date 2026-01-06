package com.z.c.woodexcess_api.service;

import com.z.c.woodexcess_api.dto.notification.CreateNotificationCommand;
import com.z.c.woodexcess_api.dto.notification.NotificationResponse;
import com.z.c.woodexcess_api.exception.ResourceNotFoundException;
import com.z.c.woodexcess_api.model.Notification;
import com.z.c.woodexcess_api.model.User;
import com.z.c.woodexcess_api.model.enums.NotificationType;
import com.z.c.woodexcess_api.repository.NotificationRepository;
import com.z.c.woodexcess_api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Unit Tests")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    private User user;
    private Notification notification;
    private CreateNotificationCommand createCommand;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .name("John User")
                .email("user@example.com")
                .build();

        notification = Notification.builder()
                .id(UUID.randomUUID())
                .user(user)
                .type(NotificationType.NEW_OFFER)
                .title("Title")
                .message("Message")
                .linkUrl("/test")
                .metadata(Map.of("key", "value"))
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        createCommand = CreateNotificationCommand.builder()
                .userId(user.getId())
                .type(NotificationType.NEW_OFFER)
                .title("Title")
                .message("Message")
                .linkUrl("/test")
                .metadata(Map.of("key", "value"))
                .build();
    }


    @Test
    @DisplayName("Should create notification successfully")
    void shouldCreateNotificationSuccessfully() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        NotificationResponse response = notificationService.createNotification(createCommand);

        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo(user.getId());
        assertThat(response.type()).isEqualTo(NotificationType.NEW_OFFER);
        assertThat(response.isRead()).isFalse();

        verify(userRepository).findById(user.getId());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("Should throw when creating notification for non-existing user")
    void shouldThrowWhenUserNotFoundOnCreate() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.createNotification(createCommand))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(notificationRepository, never()).save(any());
    }

    // =============== MARK AS READ / UNREAD ===============

    @Test
    @DisplayName("Should mark notification as read")
    void shouldMarkNotificationAsRead() {
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        NotificationResponse response = notificationService.markAsRead(notification.getId(), user.getId());

        assertThat(response).isNotNull();
        assertThat(notification.isRead()).isTrue();
        assertThat(response.isRead()).isTrue();

        verify(notificationRepository).findById(notification.getId());
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("Should not save if notification already read")
    void shouldNotSaveIfAlreadyRead() {
        notification.markAsRead();

        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));

        NotificationResponse response = notificationService.markAsRead(notification.getId(), user.getId());

        assertThat(response).isNotNull();
        assertThat(notification.isRead()).isTrue();

        verify(notificationRepository).findById(notification.getId());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should mark all notifications as read")
    void shouldMarkAllNotificationsAsRead() {
        Notification n1 = Notification.builder()
                .id(UUID.randomUUID())
                .user(user)
                .type(NotificationType.NEW_OFFER)
                .title("Title 1")
                .message("Message 1")
                .linkUrl("/test1")
                .metadata(Map.of("key", "value"))
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        Notification n2 = Notification.builder()
                .id(UUID.randomUUID())
                .user(user)
                .type(NotificationType.NEW_OFFER)
                .title("Title 2")
                .message("Message 2")
                .linkUrl("/test2")
                .metadata(Map.of("key", "value2"))
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.findByUserIdAndIsReadFalse(user.getId()))
                .thenReturn(List.of(n1, n2));

        notificationService.markAllAsRead(user.getId());

        assertThat(n1.isRead()).isTrue();
        assertThat(n2.isRead()).isTrue();

        verify(notificationRepository).findByUserIdAndIsReadFalse(user.getId());
        verify(notificationRepository).saveAll(anyList());
    }


    @Test
    @DisplayName("Should mark notification as unread")
    void shouldMarkNotificationAsUnread() {
        notification.markAsRead(); // começa como lida
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        NotificationResponse response = notificationService.markAsUnread(notification.getId(), user.getId());

        assertThat(response).isNotNull();
        assertThat(notification.isRead()).isFalse();
        assertThat(response.isRead()).isFalse();

        verify(notificationRepository).findById(notification.getId());
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("Should not save if notification already unread")
    void shouldNotSaveIfAlreadyUnread() {
        notification.markAsUnread();

        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));

        NotificationResponse response = notificationService.markAsUnread(notification.getId(), user.getId());

        assertThat(response).isNotNull();
        assertThat(notification.isRead()).isFalse();

        verify(notificationRepository).findById(notification.getId());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw when marking read/unread for another user's notification")
    void shouldThrowWhenUserDoesNotOwnNotification() {
        UUID otherUserId = UUID.randomUUID();
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markAsRead(notification.getId(), otherUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Notification not found");

        assertThatThrownBy(() -> notificationService.markAsUnread(notification.getId(), otherUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Notification not found");

        verify(notificationRepository, times(2)).findById(notification.getId());
        verify(notificationRepository, never()).save(any());
    }

    // =============== DELETE ===============

    @Test
    @DisplayName("Should delete notification successfully")
    void shouldDeleteNotificationSuccessfully() {
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));

        notificationService.deleteNotification(notification.getId(), user.getId());

        verify(notificationRepository).findById(notification.getId());
        verify(notificationRepository).delete(notification);
    }

    @Test
    @DisplayName("Should throw when deleting notification from another user")
    void shouldThrowWhenDeletingNotificationFromAnotherUser() {
        UUID otherUserId = UUID.randomUUID();
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.deleteNotification(notification.getId(), otherUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Notification not found");

        verify(notificationRepository).findById(notification.getId());
        verify(notificationRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should delete all read notifications")
    void shouldDeleteAllReadNotifications() {
        Notification n1 = Notification.builder()
                .id(UUID.randomUUID())
                .user(user)
                .type(NotificationType.NEW_OFFER)
                .title("Title 1")
                .message("Message 1")
                .isRead(true)
                .build();

        Notification n2 = Notification.builder()
                .id(UUID.randomUUID())
                .user(user)
                .type(NotificationType.NEW_OFFER)
                .title("Title 2")
                .message("Message 2")
                .isRead(true)
                .build();

        when(notificationRepository.findByUserIdAndIsReadTrue(user.getId()))
                .thenReturn(List.of(n1, n2));

        notificationService.deleteAllRead(user.getId());

        verify(notificationRepository).findByUserIdAndIsReadTrue(user.getId());
        verify(notificationRepository).deleteAll(anyList());
    }

    @Test
    @DisplayName("Should delete all notifications for user")
    void shouldDeleteAllNotifications() {
        notificationService.deleteAll(user.getId());

        verify(notificationRepository).deleteByUserId(user.getId());
    }

    @Test
    @DisplayName("Should get notification by id for owner")
    void shouldGetNotificationByIdForOwner() {
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));

        NotificationResponse response = notificationService.getNotificationById(notification.getId(), user.getId());

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(notification.getId());

        verify(notificationRepository).findById(notification.getId());
    }

    @Test
    @DisplayName("Should throw when getting notification by id for another user")
    void shouldThrowWhenGettingNotificationByIdForAnotherUser() {
        UUID otherUserId = UUID.randomUUID();
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.getNotificationById(notification.getId(), otherUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Notification not found");

        verify(notificationRepository).findById(notification.getId());
    }

    @Test
    @DisplayName("Should get user notifications without filters")
    void shouldGetUserNotificationsWithoutFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> page = new PageImpl<>(List.of(notification), pageable, 1);

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable))
                .thenReturn(page);

        Page<NotificationResponse> result = notificationService.getUserNotifications(
                user.getId(), null, null, pageable
        );

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(notification.getId());
    }

    @Test
    @DisplayName("Should get user notifications filtered by isRead")
    void shouldGetUserNotificationsFilteredByIsRead() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> page = new PageImpl<>(List.of(notification), pageable, 1);

        when(notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(user.getId(), false, pageable))
                .thenReturn(page);

        Page<NotificationResponse> result = notificationService.getUserNotifications(
                user.getId(), false, null, pageable
        );

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(notificationRepository).findByUserIdAndIsReadOrderByCreatedAtDesc(user.getId(), false, pageable);
    }

    @Test
    @DisplayName("Should get user notifications filtered by type")
    void shouldGetUserNotificationsFilteredByType() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> page = new PageImpl<>(List.of(notification), pageable, 1);

        when(notificationRepository.findByUserIdAndTypeOrderByCreatedAtDesc(user.getId(), NotificationType.NEW_OFFER, pageable))
                .thenReturn(page);

        Page<NotificationResponse> result = notificationService.getUserNotifications(
                user.getId(), null, NotificationType.NEW_OFFER, pageable
        );

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(notificationRepository).findByUserIdAndTypeOrderByCreatedAtDesc(
                user.getId(), NotificationType.NEW_OFFER, pageable
        );
    }

    @Test
    @DisplayName("Should get user notifications filtered by isRead and type")
    void shouldGetUserNotificationsFilteredByIsReadAndType() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> page = new PageImpl<>(List.of(notification), pageable, 1);

        when(notificationRepository.findByUserIdAndIsReadAndTypeOrderByCreatedAtDesc(
                user.getId(), false, NotificationType.NEW_OFFER, pageable))
                .thenReturn(page);

        Page<NotificationResponse> result = notificationService.getUserNotifications(
                user.getId(), false, NotificationType.NEW_OFFER, pageable
        );

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(notificationRepository).findByUserIdAndIsReadAndTypeOrderByCreatedAtDesc(
                user.getId(), false, NotificationType.NEW_OFFER, pageable
        );
    }


    @Test
    @DisplayName("Should count unread notifications")
    void shouldCountUnreadNotifications() {
        when(notificationRepository.countByUserIdAndIsReadFalse(user.getId())).thenReturn(3L);

        long result = notificationService.countUnreadNotifications(user.getId());

        assertThat(result).isEqualTo(3L);
        verify(notificationRepository).countByUserIdAndIsReadFalse(user.getId());
    }

    @Test
    @DisplayName("Should get recent notifications with limit")
    void shouldGetRecentNotifications() {
        int limit = 5;
        Pageable pageable = PageRequest.of(0, limit);
        Page<Notification> page = new PageImpl<>(List.of(notification), pageable, 1);

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(user.getId()), any(Pageable.class)))
                .thenReturn(page);

        List<NotificationResponse> result = notificationService.getRecentNotifications(user.getId(), limit);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(eq(user.getId()), any(Pageable.class));
    }

    @Test
    @DisplayName("Should delete old read notifications")
    void shouldDeleteOldReadNotifications() {
        int daysOld = 30;
        when(notificationRepository.deleteByIsReadTrueAndCreatedAtBefore(any(LocalDateTime.class)))
                .thenReturn(5);

        notificationService.deleteOldReadNotifications(daysOld);

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(notificationRepository).deleteByIsReadTrueAndCreatedAtBefore(captor.capture());

        LocalDateTime cutoff = captor.getValue();
        assertThat(cutoff).isNotNull();
    }
}
