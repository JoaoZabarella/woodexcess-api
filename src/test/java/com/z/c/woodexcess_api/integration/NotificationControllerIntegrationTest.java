package com.z.c.woodexcess_api.integration;

import com.z.c.woodexcess_api.model.Notification;
import com.z.c.woodexcess_api.model.User;
import com.z.c.woodexcess_api.model.enums.NotificationType;
import com.z.c.woodexcess_api.model.enums.UserRole;
import com.z.c.woodexcess_api.repository.NotificationRepository;
import com.z.c.woodexcess_api.repository.UserRepository;
import com.z.c.woodexcess_api.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
class NotificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user;
    private User anotherUser;
    private String userToken;
    private String anotherUserToken;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        userRepository.deleteAll();

        // Create main user
        user = User.builder()
                .name("John Doe")
                .email("john@test.com")
                .password(passwordEncoder.encode("password123"))
                .phone("11999999999")
                .role(UserRole.USER)
                .build();
        user = userRepository.save(user);


        anotherUser = User.builder()
                .name("Jane Smith")
                .email("jane@test.com")
                .password(passwordEncoder.encode("password123"))
                .phone("11988888888")
                .role(UserRole.USER)
                .build();
        anotherUser = userRepository.save(anotherUser);


        userToken = jwtProvider.generateJwtToken(user);
        anotherUserToken = jwtProvider.generateJwtToken(anotherUser);
    }

    @Test
    void getUserNotifications_Success() throws Exception {
        // Create notifications
        createNotification(user, NotificationType.NEW_OFFER, "New offer received", false);
        createNotification(user, NotificationType.OFFER_ACCEPTED, "Offer accepted", true);
        createNotification(user, NotificationType.OFFER_REJECTED, "Offer rejected", false);

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[0].type").exists())
                .andExpect(jsonPath("$.content[0].title").exists())
                .andExpect(jsonPath("$.content[0].isRead").exists());
    }

    @Test
    void getUserNotifications_FilteredByReadStatus() throws Exception {
        // Create notifications
        createNotification(user, NotificationType.NEW_OFFER, "Unread notification", false);
        createNotification(user, NotificationType.OFFER_ACCEPTED, "Read notification", true);

        mockMvc.perform(get("/api/notifications")
                        .param("isRead", "false")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].isRead").value(false));
    }

    @Test
    void getUserNotifications_FilteredByType() throws Exception {
        // Create notifications
        createNotification(user, NotificationType.NEW_OFFER, "New offer", false);
        createNotification(user, NotificationType.OFFER_ACCEPTED, "Offer accepted", false);
        createNotification(user, NotificationType.NEW_OFFER, "Another offer", false);

        mockMvc.perform(get("/api/notifications")
                        .param("type", "NEW_OFFER")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].type").value("NEW_OFFER"))
                .andExpect(jsonPath("$.content[1].type").value("NEW_OFFER"));
    }

    @Test
    void getUserNotifications_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getNotificationById_Success() throws Exception {
        Notification notification = createNotification(user, NotificationType.NEW_OFFER, "Test notification", false);

        mockMvc.perform(get("/api/notifications/{id}", notification.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notification.getId().toString()))
                .andExpect(jsonPath("$.title").value("Test notification"))
                .andExpect(jsonPath("$.type").value("NEW_OFFER"));
    }

    @Test
    void getNotificationById_NotFound() throws Exception {
        UUID randomId = UUID.randomUUID();

        mockMvc.perform(get("/api/notifications/{id}", randomId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getNotificationById_AccessDenied() throws Exception {

        Notification notification = createNotification(anotherUser, NotificationType.NEW_OFFER, "Private notification", false);


        mockMvc.perform(get("/api/notifications/{id}", notification.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRecentNotifications_Success() throws Exception {

        for (int i = 0; i < 15; i++) {
            createNotification(user, NotificationType.NEW_OFFER, "Notification " + i, false);
        }

        mockMvc.perform(get("/api/notifications/recent")
                        .param("limit", "10")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(10)));
    }

    @Test
    void getRecentNotifications_DefaultLimit() throws Exception {

        for (int i = 0; i < 15; i++) {
            createNotification(user, NotificationType.NEW_OFFER, "Notification " + i, false);
        }

        mockMvc.perform(get("/api/notifications/recent")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(10))); // Default limit
    }

    @Test
    void getUnreadCount_Success() throws Exception {
        // Create notifications
        createNotification(user, NotificationType.NEW_OFFER, "Unread 1", false);
        createNotification(user, NotificationType.NEW_OFFER, "Unread 2", false);
        createNotification(user, NotificationType.OFFER_ACCEPTED, "Read 1", true);

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    void getUnreadCount_Zero() throws Exception {
        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void markAsRead_Success() throws Exception {
        Notification notification = createNotification(user, NotificationType.NEW_OFFER, "Unread notification", false);

        mockMvc.perform(patch("/api/notifications/{id}/read", notification.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notification.getId().toString()))
                .andExpect(jsonPath("$.isRead").value(true))
                .andExpect(jsonPath("$.readAt").exists());
    }

    @Test
    void markAsRead_AlreadyRead() throws Exception {
        Notification notification = createNotification(user, NotificationType.NEW_OFFER, "Already read", true);

        mockMvc.perform(patch("/api/notifications/{id}/read", notification.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead").value(true));
    }

    @Test
    void markAsRead_NotFound() throws Exception {
        UUID randomId = UUID.randomUUID();

        mockMvc.perform(patch("/api/notifications/{id}/read", randomId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void markAsUnread_Success() throws Exception {
        Notification notification = createNotification(user, NotificationType.NEW_OFFER, "Read notification", true);

        mockMvc.perform(patch("/api/notifications/{id}/unread", notification.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notification.getId().toString()))
                .andExpect(jsonPath("$.isRead").value(false))
                .andExpect(jsonPath("$.readAt").doesNotExist());
    }

    @Test
    void markAllAsRead_Success() throws Exception {

        createNotification(user, NotificationType.NEW_OFFER, "Unread 1", false);
        createNotification(user, NotificationType.NEW_OFFER, "Unread 2", false);
        createNotification(user, NotificationType.OFFER_ACCEPTED, "Unread 3", false);

        mockMvc.perform(patch("/api/notifications/mark-all-read")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());


        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void markAllAsRead_NoUnreadNotifications() throws Exception {

        createNotification(user, NotificationType.NEW_OFFER, "Read 1", true);

        mockMvc.perform(patch("/api/notifications/mark-all-read")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteNotification_Success() throws Exception {
        Notification notification = createNotification(user, NotificationType.NEW_OFFER, "To be deleted", false);

        mockMvc.perform(delete("/api/notifications/{id}", notification.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());


        mockMvc.perform(get("/api/notifications/{id}", notification.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteNotification_NotFound() throws Exception {
        UUID randomId = UUID.randomUUID();

        mockMvc.perform(delete("/api/notifications/{id}", randomId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteNotification_AccessDenied() throws Exception {

        Notification notification = createNotification(anotherUser, NotificationType.NEW_OFFER, "Private", false);


        mockMvc.perform(delete("/api/notifications/{id}", notification.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteAllRead_Success() throws Exception {

        createNotification(user, NotificationType.NEW_OFFER, "Unread", false);
        createNotification(user, NotificationType.OFFER_ACCEPTED, "Read 1", true);
        createNotification(user, NotificationType.OFFER_REJECTED, "Read 2", true);

        mockMvc.perform(delete("/api/notifications/read")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());


        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].isRead").value(false));
    }

    @Test
    void deleteAllRead_NoReadNotifications() throws Exception {

        createNotification(user, NotificationType.NEW_OFFER, "Unread", false);

        mockMvc.perform(delete("/api/notifications/read")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());


        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void deleteAllNotifications_Success() throws Exception {

        createNotification(user, NotificationType.NEW_OFFER, "Notification 1", false);
        createNotification(user, NotificationType.OFFER_ACCEPTED, "Notification 2", true);
        createNotification(user, NotificationType.OFFER_REJECTED, "Notification 3", false);

        mockMvc.perform(delete("/api/notifications")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());


        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void deleteAllNotifications_DoesNotAffectOtherUsers() throws Exception {
        // Create notifications for both users
        createNotification(user, NotificationType.NEW_OFFER, "User notification", false);
        createNotification(anotherUser, NotificationType.OFFER_ACCEPTED, "Another user notification", false);


        mockMvc.perform(delete("/api/notifications")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());


        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));


        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + anotherUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void getUserNotifications_Pagination() throws Exception {

        for (int i = 0; i < 25; i++) {
            createNotification(user, NotificationType.NEW_OFFER, "Notification " + i, false);
        }


        mockMvc.perform(get("/api/notifications")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(10)))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.number").value(0));

        // Second page
        mockMvc.perform(get("/api/notifications")
                        .param("page", "1")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(10)))
                .andExpect(jsonPath("$.number").value(1));
    }


    private Notification createNotification(User user, NotificationType type, String title, boolean isRead) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("offer_id", UUID.randomUUID().toString());
        metadata.put("listing_id", UUID.randomUUID().toString());

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message("Test message for " + title)
                .linkUrl("/offers/" + UUID.randomUUID())
                .metadata(metadata)
                .isRead(isRead)
                .build();

        if (isRead) {
            notification.setReadAt(LocalDateTime.now().minusHours(1));
        }

        return notificationRepository.save(notification);
    }
}
