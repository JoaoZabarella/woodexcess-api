package com.z.c.woodexcess_api.validator;

import com.z.c.woodexcess_api.exception.listing.ListingNotFoundException;
import com.z.c.woodexcess_api.exception.users.UserNotFoundException;
import com.z.c.woodexcess_api.model.MaterialListing;
import com.z.c.woodexcess_api.model.User;
import com.z.c.woodexcess_api.repository.MaterialListingRepository;
import com.z.c.woodexcess_api.repository.MessageRepository;
import com.z.c.woodexcess_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.AccessDeniedException;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class MessageValidator {

    private final UserRepository userRepository;
    private final MaterialListingRepository listingRepository;
    private final MessageRepository messageRepository;


    public ValidatedMessageData validateAndLoadMessageData(UUID senderId, UUID recipientId, UUID listingId) {
        log.debug("Validating message creation: sender={}, recipient={}, listing={}",
                senderId, recipientId, listingId);


        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> {
                    log.warn("Sender not found: {}", senderId);
                    return new UserNotFoundException("Sender not found with ID: " + senderId);
                });

        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> {
                    log.warn("Recipient not found: {}", recipientId);
                    return new UserNotFoundException("Recipient not found with ID: " + recipientId);
                });

        MaterialListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> {
                    log.warn("Listing not found: {}", listingId);
                    return new ListingNotFoundException(listingId);
                });


        validateDifferentUsers(senderId, recipientId);

        log.debug("Message data validated successfully");
        return new ValidatedMessageData(sender, recipient, listing);
    }

    public void validateUserExists(UUID userId, String errorMessage) {
        if (!userRepository.existsById(userId)) {
            log.warn("User validation failed: {}", errorMessage);
            throw new UserNotFoundException(errorMessage);
        }
    }

    public void validateUserExists(UUID userId) {
        validateUserExists(userId, "User not found with ID: " + userId);
    }

    public void validateListingExists(UUID listingId) {
        if (!listingRepository.existsById(listingId)) {
            log.warn("Listing validation failed: {}", listingId);
            throw new ListingNotFoundException(listingId);
        }
    }

    public void validateDifferentUsers(UUID senderId, UUID recipientId) {
        if (senderId.equals(recipientId)) {
            log.warn("User {} attempted to send message to themselves", senderId);
            throw new IllegalArgumentException("Cannot send message to yourself");
        }
    }

    @Deprecated(forRemoval = true)
    public void validateMessageCreation(UUID senderId, UUID recipientId, UUID listingId) {
        validateUserExists(senderId, "Sender not found with ID: " + senderId);
        validateUserExists(recipientId, "Recipient not found with ID: " + recipientId);
        validateListingExists(listingId);
        validateDifferentUsers(senderId, recipientId);
    }

    public void validateConversationAccess(UUID currentUserId, UUID otherUserId, UUID listingId)
            throws AccessDeniedException {
        validateUserExists(currentUserId, "Current user not found with ID: " + currentUserId);
        validateUserExists(otherUserId, "Other user not found with ID: " + otherUserId);
        validateListingExists(listingId);

        if (currentUserId.equals(otherUserId)) {
            log.warn("User {} attempted to access conversation with themselves", currentUserId);
            throw new IllegalArgumentException("Cannot access conversation with yourself");
        }

        boolean conversationExists = messageRepository.existsConversation(
                currentUserId, otherUserId, listingId
        );

        if (!conversationExists) {
            log.warn("User {} attempted to access non-existent conversation with user {} for listing {}",
                    currentUserId, otherUserId, listingId);
            throw new AccessDeniedException(
                    "You don't have permission to access this conversation"
            );
        }

        log.debug("Conversation access validated for user {} with user {} on listing {}",
                currentUserId, otherUserId, listingId);
    }
}
