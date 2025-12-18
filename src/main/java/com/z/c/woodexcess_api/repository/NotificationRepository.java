package com.z.c.woodexcess_api.repository;

import com.z.c.woodexcess_api.model.Notification;
import com.z.c.woodexcess_api.model.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @EntityGraph(attributePaths = {"user"})
    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Page<Notification> findByUserIdAndIsReadOrderByCreatedAtDesc(UUID userId, Boolean isRead, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Page<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(UUID userId, NotificationType type, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Page<Notification> findByUserIdAndIsReadAndTypeOrderByCreatedAtDesc(
            UUID userId,
            Boolean isRead,
            NotificationType type,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"user"})
    List<Notification> findByUserIdAndIsReadFalse(UUID userId);

    @EntityGraph(attributePaths = {"user"})
    List<Notification> findByUserIdAndIsReadTrue(UUID userId);

    @Query("SELECT n FROM Notification n " +
            "LEFT JOIN FETCH n.user u " +
            "WHERE n.user.id = :userId " +
            "ORDER BY n.createdAt DESC " +
            "LIMIT :limit")
    List<Notification> findTopNByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId, @Param("limit") int limit);

    long countByUserIdAndIsReadFalse(UUID userId);

    @Modifying
    void deleteByUserId(UUID userId);

    @Modifying
    int deleteByIsReadTrueAndCreatedAtBefore(LocalDateTime cutoffDate);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.user.id = :userId AND n.isRead = false")
    int markAllAsRead(@Param("userId") UUID userId, @Param("readAt") LocalDateTime readAt);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.id IN :notificationIds")
    int markAsRead(@Param("notificationIds") List<UUID> notificationIds, @Param("readAt") LocalDateTime readAt);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :before")
    int deleteOldNotifications(@Param("before") LocalDateTime before);

    @Query(value = "SELECT * FROM notifications n WHERE n.user_id = :userId AND n.metadata->>'offer_id' = :offerId",
            nativeQuery = true)
    List<Notification> findByUserIdAndOfferIdInMetadata(@Param("userId") UUID userId, @Param("offerId") String offerId);
}
