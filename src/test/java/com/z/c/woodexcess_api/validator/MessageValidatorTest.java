package com.z.c.woodexcess_api.validator;

import com.z.c.woodexcess_api.exception.listing.ListingNotFoundException;
import com.z.c.woodexcess_api.exception.users.UserNotFoundException;
import com.z.c.woodexcess_api.model.MaterialListing;
import com.z.c.woodexcess_api.model.User;
import com.z.c.woodexcess_api.repository.MaterialListingRepository;
import com.z.c.woodexcess_api.repository.MessageRepository;
import com.z.c.woodexcess_api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.file.AccessDeniedException;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageValidator Unit Tests")
class MessageValidatorTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MaterialListingRepository listingRepository;

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private MessageValidator messageValidator;

    private UUID userId1;
    private UUID userId2;
    private UUID listingId;
    private User user1;
    private User user2;
    private MaterialListing listing;

    @BeforeEach
    void setUp() {
        userId1 = UUID.randomUUID();
        userId2 = UUID.randomUUID();
        listingId = UUID.randomUUID();

        user1 = User.builder()
                .id(userId1)
                .name("User One")
                .email("user1@example.com")
                .build();

        user2 = User.builder()
                .id(userId2)
                .name("User Two")
                .email("user2@example.com")
                .build();

        listing = MaterialListing.builder()
                .id(listingId)
                .title("Test Listing")
                .build();
    }

    @Test
    @DisplayName("Should validate and load message data successfully")
    void validateAndLoadMessageData_Success() {
        // Arrange
        when(userRepository.findById(userId1)).thenReturn(Optional.of(user1));
        when(userRepository.findById(userId2)).thenReturn(Optional.of(user2));
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        // Act
        ValidatedMessageData result = messageValidator.validateAndLoadMessageData(
                userId1, userId2, listingId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.sender()).isEqualTo(user1);
        assertThat(result.recipient()).isEqualTo(user2);
        assertThat(result.listing()).isEqualTo(listing);

        verify(userRepository).findById(userId1);
        verify(userRepository).findById(userId2);
        verify(listingRepository).findById(listingId);
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when sender not found")
    void validateAndLoadMessageData_SenderNotFound() {
        // Arrange
        when(userRepository.findById(userId1)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
                messageValidator.validateAndLoadMessageData(userId1, userId2, listingId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("Sender not found");

        verify(userRepository).findById(userId1);
        verify(userRepository, never()).findById(userId2);
        verify(listingRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when recipient not found")
    void validateAndLoadMessageData_RecipientNotFound() {
        // Arrange
        when(userRepository.findById(userId1)).thenReturn(Optional.of(user1));
        when(userRepository.findById(userId2)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
                messageValidator.validateAndLoadMessageData(userId1, userId2, listingId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("Recipient not found");

        verify(userRepository).findById(userId1);
        verify(userRepository).findById(userId2);
        verify(listingRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should throw ListingNotFoundException when listing not found")
    void validateAndLoadMessageData_ListingNotFound() {
        // Arrange
        when(userRepository.findById(userId1)).thenReturn(Optional.of(user1));
        when(userRepository.findById(userId2)).thenReturn(Optional.of(user2));
        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
                messageValidator.validateAndLoadMessageData(userId1, userId2, listingId))
                .isInstanceOf(ListingNotFoundException.class);

        verify(listingRepository).findById(listingId);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when trying to message yourself")
    void validateAndLoadMessageData_SameUser() {
        // Arrange
        when(userRepository.findById(userId1)).thenReturn(Optional.of(user1));
        when(userRepository.findById(userId1)).thenReturn(Optional.of(user1));
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        // Act & Assert
        assertThatThrownBy(() ->
                messageValidator.validateAndLoadMessageData(userId1, userId1, listingId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot send message to yourself");
    }

    @Test
    @DisplayName("Should validate user exists successfully")
    void validateUserExists_Success() {
        // Arrange
        when(userRepository.existsById(userId1)).thenReturn(true);

        // Act & Assert
        assertThatCode(() -> messageValidator.validateUserExists(userId1))
                .doesNotThrowAnyException();

        verify(userRepository).existsById(userId1);
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when user does not exist")
    void validateUserExists_UserNotFound() {
        // Arrange
        when(userRepository.existsById(userId1)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> messageValidator.validateUserExists(userId1))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).existsById(userId1);
    }

    @Test
    @DisplayName("Should validate listing exists successfully")
    void validateListingExists_Success() {
        // Arrange
        when(listingRepository.existsById(listingId)).thenReturn(true);

        // Act & Assert
        assertThatCode(() -> messageValidator.validateListingExists(listingId))
                .doesNotThrowAnyException();

        verify(listingRepository).existsById(listingId);
    }

    @Test
    @DisplayName("Should throw ListingNotFoundException when listing does not exist")
    void validateListingExists_ListingNotFound() {
        // Arrange
        when(listingRepository.existsById(listingId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> messageValidator.validateListingExists(listingId))
                .isInstanceOf(ListingNotFoundException.class);

        verify(listingRepository).existsById(listingId);
    }

    @Test
    @DisplayName("Should validate different users successfully")
    void validateDifferentUsers_Success() {
        // Act & Assert
        assertThatCode(() -> messageValidator.validateDifferentUsers(userId1, userId2))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when users are the same")
    void validateDifferentUsers_SameUser() {
        // Act & Assert
        assertThatThrownBy(() -> messageValidator.validateDifferentUsers(userId1, userId1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot send message to yourself");
    }

    @Test
    @DisplayName("Should validate conversation access successfully")
    void validateConversationAccess_Success() throws AccessDeniedException {
        // Arrange
        when(userRepository.existsById(userId1)).thenReturn(true);
        when(userRepository.existsById(userId2)).thenReturn(true);
        when(listingRepository.existsById(listingId)).thenReturn(true);
        when(messageRepository.existsConversation(userId1, userId2, listingId)).thenReturn(true);

        // Act & Assert
        assertThatCode(() ->
                messageValidator.validateConversationAccess(userId1, userId2, listingId))
                .doesNotThrowAnyException();

        verify(messageRepository).existsConversation(userId1, userId2, listingId);
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when conversation does not exist")
    void validateConversationAccess_ConversationNotFound() {
        // Arrange
        when(userRepository.existsById(userId1)).thenReturn(true);
        when(userRepository.existsById(userId2)).thenReturn(true);
        when(listingRepository.existsById(listingId)).thenReturn(true);
        when(messageRepository.existsConversation(userId1, userId2, listingId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() ->
                messageValidator.validateConversationAccess(userId1, userId2, listingId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("permission");

        verify(messageRepository).existsConversation(userId1, userId2, listingId);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when trying to access conversation with yourself")
    void validateConversationAccess_SameUser() {
        // Arrange
        when(userRepository.existsById(userId1)).thenReturn(true);
        when(listingRepository.existsById(listingId)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() ->
                messageValidator.validateConversationAccess(userId1, userId1, listingId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot access conversation with yourself");
    }
}
