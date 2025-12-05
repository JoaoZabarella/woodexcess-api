package com.z.c.woodexcess_api.repository;

import com.z.c.woodexcess_api.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {



    @Query("""
        SELECT CASE WHEN EXISTS (
            SELECT 1
            FROM Message m Where m.listing.id = :listingId
            AND (
                (m.sender.id = :userId1 AND m.recipient.id = :userId2)
                OR (m.sender.id = :userId2 AND m.recipient.id = :userId1)
            )
        ) THEN true ELSE false END 
""")
    boolean existsConversation(
            @Param("userId1") UUID userId1,
            @Param("userId2") UUID userId2,
            @Param("listingId") UUID listingId
    );


    @EntityGraph(attributePaths = {"sender", "recipient", "listing"})
    @Query("""
           SELECT m FROM Message m 
           WHERE m.listing.id = :listingId
           AND ((m.sender.id = :user1Id AND m.recipient.id = :user2Id)
               OR (m.sender.id = :user2Id AND m.recipient.id = :user1Id))
           ORDER BY m.createdAt ASC
    """)
    List<Message> findConversationBetweenUsers(
            @Param("user1Id") UUID user1Id,
            @Param("user2Id") UUID user2Id,
            @Param("listingId") UUID listingId
    );


    @EntityGraph(attributePaths = {"sender", "recipient", "listing"})
    @Query("""
        SELECT m FROM Message m
        WHERE m.listing.id = :listingId
        AND (m.sender.id = :userId OR m.recipient.id = :userId)
        ORDER BY m.createdAt DESC
    """)
    Page<Message> findMessagesByListingAndUser(
            @Param("listingId") UUID listingId,
            @Param("userId") UUID userId,
            Pageable pageable
    );

    @Query("""
        SELECT COUNT(m) FROM Message m
        WHERE m.recipient.id = :userId
        AND m.isRead = false
    """)
    Long countUnreadMessagesByUser(@Param("userId") UUID userId);


    @Query("""
        SELECT COUNT(m) FROM Message m
        WHERE m.recipient.id = :recipientId
        AND m.sender.id = :senderId
        AND m.listing.id = :listingId
        AND m.isRead = false
    """)
    Long countUnreadInConversation(
            @Param("recipientId") UUID recipientId,
            @Param("senderId") UUID senderId,
            @Param("listingId") UUID listingId
    );

    @Modifying
    @Query("""
        UPDATE Message m SET m.isRead = true
        WHERE m.recipient.id = :recipientId
        AND m.sender.id = :senderId
        AND m.listing.id = :listingId
        AND m.isRead = false
    """)
    void markMessagesAsRead(
            @Param("recipientId") UUID recipientId,
            @Param("senderId") UUID senderId,
            @Param("listingId") UUID listingId
    );


    @EntityGraph(attributePaths = {"sender", "recipient", "listing"})
    @Query("""
        SELECT m FROM Message m
        WHERE m.id IN (
            SELECT MAX(m2.id) FROM Message m2
            WHERE (m2.sender.id = :userId OR m2.recipient.id = :userId)
            GROUP BY m2.listing.id,
                     CASE WHEN m2.sender.id = :userId
                          THEN m2.recipient.id
                          ELSE m2.sender.id
                     END
        )
        ORDER BY m.createdAt DESC
    """)
    List<Message> findRecentConversations(@Param("userId") UUID userId);
}
