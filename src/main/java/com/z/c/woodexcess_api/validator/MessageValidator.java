package com.z.c.woodexcess_api.validator;

import com.z.c.woodexcess_api.exception.listing.ListingNotFoundException;
import com.z.c.woodexcess_api.exception.users.UserNotFoundException;
import com.z.c.woodexcess_api.repository.ListingImageRepository;
import com.z.c.woodexcess_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class MessageValidator {

    private final UserRepository userRepository;
    private final ListingImageRepository listingRepository;

    public void validateUserExists(UUID userId, String errorMessage){
        if (!userRepository.existsById(userId)) {
            log.warn("User validation failed {}", errorMessage);
            throw new UserNotFoundException(errorMessage);
        }
    }

    public void validateUserExists(UUID userId) {
        validateUserExists(userId, "User not found with ID: " + userId);
    }

    public void validateListingExists(UUID listingId){
        if(!listingRepository.existsById(listingId)){
            log.warn("Listing validation failed {}", listingId);
            throw new ListingNotFoundException(listingId);
        }
    }

    public void validateDifferentUsers(UUID senderId, UUID recipientId) {
        if (senderId.equals(recipientId)) {
            log.warn("User {} attempted to send message to themselves", senderId);
            throw new IllegalArgumentException("Cannot send message to yourself");
        }
    }

    public void validateMessageCreation(UUID senderId, UUID recipientId, UUID listingId) {
        validateUserExists(senderId, "Sender not found with ID: " + senderId);
        validateUserExists(recipientId, "Recipient not found with ID: " + recipientId);
        validateListingExists(listingId);
        validateDifferentUsers(senderId, recipientId);
    }

    public void validateConversationAccess(UUID currentUserId, UUID otherUserId, UUID listingId) {
        validateUserExists(currentUserId,  "Current user not found with ID: " + currentUserId);
        validateUserExists(otherUserId,   "Current user not found with ID: " + otherUserId);
        validateListingExists(listingId);
    }



}
