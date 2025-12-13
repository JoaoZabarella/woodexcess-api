package com.z.c.woodexcess_api.service;

import com.z.c.woodexcess_api.dto.message.ConversationResponse;
import com.z.c.woodexcess_api.dto.message.MessageRequest;
import com.z.c.woodexcess_api.dto.message.MessageResponse;
import com.z.c.woodexcess_api.exception.listing.ListingNotFoundException;
import com.z.c.woodexcess_api.exception.users.UserNotFoundException;
import com.z.c.woodexcess_api.mapper.MessageMapper;
import com.z.c.woodexcess_api.model.MaterialListing;
import com.z.c.woodexcess_api.model.Message;
import com.z.c.woodexcess_api.model.User;
import com.z.c.woodexcess_api.repository.MessageRepository;
import com.z.c.woodexcess_api.service.message.MessageService;
import com.z.c.woodexcess_api.validator.MessageValidator;
import com.z.c.woodexcess_api.validator.ValidatedMessageData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageService Unit Tests")
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MessageMapper messageMapper;

    @Mock
    private MessageValidator validator;

    @InjectMocks
    private MessageService messageService;

    private UUID senderId;
    private UUID recipientId;
    private UUID listingId;
    private User sender;
    private User recipient;
    private MaterialListing listing;
    private Message message;
    private MessageRequest messageRequest;
    private MessageResponse messageResponse;

    @BeforeEach
    void setUp() {
        senderId = UUID.randomUUID();
        recipientId = UUID.randomUUID();
        listingId = UUID.randomUUID();

        // Mock User entities
        sender = User.builder()
                .id(senderId)
                .name("John Sender")
                .email("sender@example.com")
                .build();

        recipient = User.builder()
                .id(recipientId)
                .name("Jane Recipient")
                .email("recipient@example.com")
                .build();

        // Mock MaterialListing
        listing = MaterialListing.builder()
                .id(listingId)
                .title("Oak Wood Planks")
                .build();

        // Mock Message entity
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

        // Mock DTOs
        messageRequest = MessageRequest.builder()
                .recipientId(recipientId)
                .listingId(listingId)
                .content("Hello, is this item still available?")
                .build();

        messageResponse = MessageResponse.builder()
                .id(message.getId())
                .senderId(senderId)
                .senderEmail("sender@example.com")
                .senderName("John Sender")
                .recipientId(recipientId)
                .recipientEmail("recipient@example.com")
                .recipientName("Jane Recipient")
                .listingId(listingId)
                .listingTitle("Oak Wood Planks")
                .content("Hello, is this item still available?")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should send message successfully")
    void sendMessage_Success() {
        // Arrange
        ValidatedMessageData validatedData = new ValidatedMessageData(sender, recipient, listing);

        when(validator.validateAndLoadMessageData(senderId, recipientId, listingId))
                .thenReturn(validatedData);
        when(messageRepository.save(any(Message.class))).thenReturn(message);
        when(messageMapper.toResponse(message)).thenReturn(messageResponse);

        // Act
        MessageResponse result = messageService.sendMessage(senderId, messageRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.content()).isEqualTo("Hello, is this item still available?");
        assertThat(result.senderId()).isEqualTo(senderId);
        assertThat(result.recipientId()).isEqualTo(recipientId);
        assertThat(result.isRead()).isFalse();

        // Verify interactions
        verify(validator).validateAndLoadMessageData(senderId, recipientId, listingId);
        verify(messageRepository).save(any(Message.class));
        verify(messageMapper).toResponse(message);

        // Verify saved message properties
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());
        Message savedMessage = messageCaptor.getValue();

        assertThat(savedMessage.getSender()).isEqualTo(sender);
        assertThat(savedMessage.getRecipient()).isEqualTo(recipient);
        assertThat(savedMessage.getListing()).isEqualTo(listing);
        assertThat(savedMessage.getContent()).isEqualTo("Hello, is this item still available?");
        assertThat(savedMessage.getIsRead()).isFalse();
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when sender not found")
    void sendMessage_SenderNotFound() {
        // Arrange
        when(validator.validateAndLoadMessageData(senderId, recipientId, listingId))
                .thenThrow(new UserNotFoundException("Sender not found with ID: " + senderId));

        // Act & Assert
        assertThatThrownBy(() -> messageService.sendMessage(senderId, messageRequest))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("Sender not found");

        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when recipient not found")
    void sendMessage_RecipientNotFound() {
        // Arrange
        when(validator.validateAndLoadMessageData(senderId, recipientId, listingId))
                .thenThrow(new UserNotFoundException("Recipient not found with ID: " + recipientId));

        // Act & Assert
        assertThatThrownBy(() -> messageService.sendMessage(senderId, messageRequest))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("Recipient not found");

        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ListingNotFoundException when listing not found")
    void sendMessage_ListingNotFound() {
        // Arrange
        when(validator.validateAndLoadMessageData(senderId, recipientId, listingId))
                .thenThrow(new ListingNotFoundException(listingId));

        // Act & Assert
        assertThatThrownBy(() -> messageService.sendMessage(senderId, messageRequest))
                .isInstanceOf(ListingNotFoundException.class);

        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should trim message content before saving")
    void sendMessage_TrimsContent() {
        // Arrange
        MessageRequest requestWithSpaces = MessageRequest.builder()
                .recipientId(recipientId)
                .listingId(listingId)
                .content("  Hello with spaces  ")
                .build();

        ValidatedMessageData validatedData = new ValidatedMessageData(sender, recipient, listing);
        when(validator.validateAndLoadMessageData(senderId, recipientId, listingId))
                .thenReturn(validatedData);
        when(messageRepository.save(any(Message.class))).thenReturn(message);
        when(messageMapper.toResponse(any())).thenReturn(messageResponse);

        // Act
        messageService.sendMessage(senderId, requestWithSpaces);

        // Assert
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());
        Message savedMessage = messageCaptor.getValue();

        assertThat(savedMessage.getContent()).isEqualTo("Hello with spaces");
    }

    @Test
    @DisplayName("Should get conversation successfully")
    void getConversation_Success() throws AccessDeniedException {
        // Arrange
        List<Message> messages = Arrays.asList(message);
        Page<Message> messagePage = new PageImpl<>(messages);

        doNothing().when(validator).validateConversationAccess(senderId, recipientId, listingId);
        when(messageRepository.findConversationOptimized(
                eq(senderId), eq(recipientId), eq(listingId), any(Pageable.class)))
                .thenReturn(messagePage);
        when(messageMapper.toResponse(message)).thenReturn(messageResponse);

        // Act
        List<MessageResponse> result = messageService.getConversation(senderId, recipientId, listingId);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(messageResponse);

        verify(validator).validateConversationAccess(senderId, recipientId, listingId);
        verify(messageRepository).findConversationOptimized(
                eq(senderId), eq(recipientId), eq(listingId), any(Pageable.class));
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when user has no access to conversation")
    void getConversation_AccessDenied() throws AccessDeniedException {
        // Arrange
        doThrow(new AccessDeniedException("You don't have permission to access this conversation"))
                .when(validator).validateConversationAccess(senderId, recipientId, listingId);

        // Act & Assert
        assertThatThrownBy(() ->
                messageService.getConversation(senderId, recipientId, listingId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("permission");

        verify(messageRepository, never()).findConversationOptimized(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should get messages by listing with pagination")
    void getMessagesByListing_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        List<Message> messages = Arrays.asList(message);
        Page<Message> messagePage = new PageImpl<>(messages, pageable, 1);

        doNothing().when(validator).validateListingExists(listingId);
        doNothing().when(validator).validateUserExists(senderId);
        when(messageRepository.findMessagesByListingAndUser(listingId, senderId, pageable))
                .thenReturn(messagePage);
        when(messageMapper.toResponse(message)).thenReturn(messageResponse);

        // Act
        Page<MessageResponse> result = messageService.getMessagesByListing(listingId, senderId, pageable);

        // Assert
        assertThat(result).isNotEmpty();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);

        verify(validator).validateListingExists(listingId);
        verify(validator).validateUserExists(senderId);
    }

    @Test
    @DisplayName("Should mark conversation as read")
    void markConversationAsRead_Success() throws AccessDeniedException {
        // Arrange
        doNothing().when(validator).validateConversationAccess(recipientId, senderId, listingId);
        doNothing().when(messageRepository).markMessagesAsRead(recipientId, senderId, listingId);

        // Act
        messageService.markConversationAsRead(recipientId, senderId, listingId);

        // Assert
        verify(validator).validateConversationAccess(recipientId, senderId, listingId);
        verify(messageRepository).markMessagesAsRead(recipientId, senderId, listingId);
    }

    @Test
    @DisplayName("Should get unread count")
    void getUnreadCount_Success() {
        // Arrange
        Long expectedCount = 5L;
        doNothing().when(validator).validateUserExists(recipientId);
        when(messageRepository.countUnreadMessagesByUser(recipientId)).thenReturn(expectedCount);

        // Act
        Long result = messageService.getUnreadCount(recipientId);

        // Assert
        assertThat(result).isEqualTo(expectedCount);
        verify(validator).validateUserExists(recipientId);
        verify(messageRepository).countUnreadMessagesByUser(recipientId);
    }

    @Test
    @DisplayName("Should get recent conversations")
    void getRecentConversations_Success() {
        // Arrange
        List<Message> lastMessages = Arrays.asList(message);
        ConversationResponse conversationResponse = new ConversationResponse(
                listingId,
                "Oak Wood Planks",
                recipientId,
                "Jane Recipient",
                messageResponse,
                2L,
                null
        );

        doNothing().when(validator).validateUserExists(senderId);
        when(messageRepository.findRecentConversations(senderId)).thenReturn(lastMessages);
        when(messageRepository.countUnreadInConversation(senderId, recipientId, listingId))
                .thenReturn(2L);
        when(messageMapper.toConversationResponse(message, senderId, 2L))
                .thenReturn(conversationResponse);

        // Act
        List<ConversationResponse> result = messageService.getRecentConversations(senderId);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).listingId()).isEqualTo(listingId);
        assertThat(result.get(0).unreadCount()).isEqualTo(2L);

        verify(validator).validateUserExists(senderId);
        verify(messageRepository).findRecentConversations(senderId);
    }

    @Test
    @DisplayName("Should return empty list when no recent conversations")
    void getRecentConversations_EmptyList() {
        // Arrange
        doNothing().when(validator).validateUserExists(senderId);
        when(messageRepository.findRecentConversations(senderId)).thenReturn(List.of());

        // Act
        List<ConversationResponse> result = messageService.getRecentConversations(senderId);

        // Assert
        assertThat(result).isEmpty();
        verify(messageRepository).findRecentConversations(senderId);
    }
}
