package com.z.c.woodexcess_api.mapper;

import com.z.c.woodexcess_api.dto.message.ConversationResponse;
import com.z.c.woodexcess_api.dto.message.MessageResponse;
import com.z.c.woodexcess_api.model.MaterialListing;
import com.z.c.woodexcess_api.model.Message;
import com.z.c.woodexcess_api.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MessageMapper Unit Tests")
class MessageMapperTest {

    private MessageMapper messageMapper;
    private Message message;
    private UUID senderId;
    private UUID recipientId;
    private UUID listingId;
    private UUID currentUserId;

    @BeforeEach
    void setUp() {
        messageMapper = new MessageMapper();

        senderId = UUID.randomUUID();
        recipientId = UUID.randomUUID();
        listingId = UUID.randomUUID();
        currentUserId = senderId;

        User sender = User.builder()
                .id(senderId)
                .name("John Sender")
                .email("sender@example.com")
                .build();

        User recipient = User.builder()
                .id(recipientId)
                .name("Jane Recipient")
                .email("recipient@example.com")
                .build();

        MaterialListing listing = MaterialListing.builder()
                .id(listingId)
                .title("Oak Wood Planks")
                .build();

        message = Message.builder()
                .id(UUID.randomUUID())
                .sender(sender)
                .recipient(recipient)
                .listing(listing)
                .content("Hello, is this item still available?")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should map Message to MessageResponse correctly")
    void toResponse_Success() {
        // Act
        MessageResponse result = messageMapper.toResponse(message);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(message.getId());
        assertThat(result.senderId()).isEqualTo(senderId);
        assertThat(result.senderEmail()).isEqualTo("sender@example.com");
        assertThat(result.senderName()).isEqualTo("John Sender");
        assertThat(result.recipientId()).isEqualTo(recipientId);
        assertThat(result.recipientEmail()).isEqualTo("recipient@example.com");
        assertThat(result.recipientName()).isEqualTo("Jane Recipient");
        assertThat(result.listingId()).isEqualTo(listingId);
        assertThat(result.listingTitle()).isEqualTo("Oak Wood Planks");
        assertThat(result.content()).isEqualTo("Hello, is this item still available?");
        assertThat(result.isRead()).isFalse();
        assertThat(result.createdAt()).isEqualTo(message.getCreatedAt());
        assertThat(result.updatedAt()).isEqualTo(message.getUpdatedAt());
    }

    @Test
    @DisplayName("Should map to ConversationResponse when current user is sender")
    void toConversationResponse_CurrentUserIsSender() {
        // Arrange
        Long unreadCount = 3L;

        // Act
        ConversationResponse result = messageMapper.toConversationResponse(
                message, currentUserId, unreadCount);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.listingId()).isEqualTo(listingId);
        assertThat(result.listingTitle()).isEqualTo("Oak Wood Planks");
        assertThat(result.otherUserId()).isEqualTo(recipientId);
        assertThat(result.otherUsername()).isEqualTo("Jane Recipient");
        assertThat(result.unreadCount()).isEqualTo(3L);
        assertThat(result.message()).isNotNull();
        assertThat(result.message().content()).isEqualTo("Hello, is this item still available?");
    }

    @Test
    @DisplayName("Should map to ConversationResponse when current user is recipient")
    void toConversationResponse_CurrentUserIsRecipient() {
        // Arrange
        UUID currentUserIdAsRecipient = recipientId;
        Long unreadCount = 1L;

        // Act
        ConversationResponse result = messageMapper.toConversationResponse(
                message, currentUserIdAsRecipient, unreadCount);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.listingId()).isEqualTo(listingId);
        assertThat(result.listingTitle()).isEqualTo("Oak Wood Planks");
        assertThat(result.otherUserId()).isEqualTo(senderId);
        assertThat(result.otherUsername()).isEqualTo("John Sender");
        assertThat(result.unreadCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should handle zero unread count in ConversationResponse")
    void toConversationResponse_ZeroUnreadCount() {
        // Arrange
        Long unreadCount = 0L;

        // Act
        ConversationResponse result = messageMapper.toConversationResponse(
                message, currentUserId, unreadCount);

        // Assert
        assertThat(result.unreadCount()).isZero();
    }
}
