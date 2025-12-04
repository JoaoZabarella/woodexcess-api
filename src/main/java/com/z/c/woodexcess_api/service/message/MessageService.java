package com.z.c.woodexcess_api.service.message;

import com.z.c.woodexcess_api.dto.message.ConversationResponse;
import com.z.c.woodexcess_api.dto.message.MessageRequest;
import com.z.c.woodexcess_api.dto.message.MessageResponse;
import com.z.c.woodexcess_api.mapper.MessageMapper;
import com.z.c.woodexcess_api.model.MaterialListing;
import com.z.c.woodexcess_api.model.Message;
import com.z.c.woodexcess_api.model.User;
import com.z.c.woodexcess_api.repository.MaterialListingRepository;
import com.z.c.woodexcess_api.repository.MessageRepository;
import com.z.c.woodexcess_api.repository.UserRepository;
import com.z.c.woodexcess_api.validator.MessageValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final MaterialListingRepository listingRepository;
    private final MessageMapper messageMapper;
    private final MessageValidator validator;


    @Transactional
    public MessageResponse sendMessage(UUID senderId, MessageRequest request) {
        log.info("Sending message from user {} to user {} about listing {}",
                senderId, request.recipientId(), request.listingId());

        validator.validateMessageCreation(senderId, request.recipientId(), request.listingId());

        User sender = userRepository.findById(senderId).orElseThrow();
        User recipient = userRepository.findById(request.recipientId()).orElseThrow();
        MaterialListing listing = listingRepository.findById(request.listingId()).orElseThrow();

        Message message = Message.builder()
                .sender(sender)
                .recipient(recipient)
                .listing(listing)
                .content(request.content().trim())
                .isRead(false)
                .build();

        Message savedMessage = messageRepository.save(message);
        log.info("Message {} saved successfully", savedMessage.getId());

        return messageMapper.toResponse(savedMessage);
    }


    @Transactional(readOnly = true)
    public List<MessageResponse> getConversation(UUID currentUserId, UUID otherUserId, UUID listingId) {
        log.info("Fetching conversation between {} and {} for listing {}",
                currentUserId, otherUserId, listingId);

        validator.validateConversationAccess(currentUserId, otherUserId, listingId);

        List<Message> messages = messageRepository.findConversationBetweenUsers(
                currentUserId, otherUserId, listingId
        );

        log.debug("Found {} messages in conversation", messages.size());
        return messages.stream()
                .map(messageMapper::toResponse)
                .toList();
    }


    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessagesByListing(UUID listingId, UUID userId, Pageable pageable) {
        log.info("Fetching messages for listing {} and user {} (page: {}, size: {})",
                listingId, userId, pageable.getPageNumber(), pageable.getPageSize());

        validator.validateListingExists(listingId);
        validator.validateUserExists(userId);

        Page<Message> messagesPage = messageRepository.findMessagesByListingAndUser(
                listingId, userId, pageable
        );

        log.debug("Found {} messages (total: {})",
                messagesPage.getNumberOfElements(), messagesPage.getTotalElements());
        return messagesPage.map(messageMapper::toResponse);
    }


    @Transactional
    public void markConversationAsRead(UUID recipientId, UUID senderId, UUID listingId) {
        log.info("Marking messages as read: recipient={}, sender={}, listing={}",
                recipientId, senderId, listingId);

        validator.validateConversationAccess(recipientId, senderId, listingId);

        messageRepository.markMessagesAsRead(recipientId, senderId, listingId);
        log.debug("Messages marked as read successfully");
    }


    @Transactional(readOnly = true)
    public Long getUnreadCount(UUID userId) {
        log.debug("Fetching unread count for user {}", userId);

        validator.validateUserExists(userId);

        Long count = messageRepository.countUnreadMessagesByUser(userId);
        log.debug("User {} has {} unread messages", userId, count);
        return count;
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> getRecentConversations(UUID userId) {
        log.info("Fetching recent conversations for user {}", userId);

        validator.validateUserExists(userId);

        List<Message> lastMessages = messageRepository.findRecentConversations(userId);

        return lastMessages.stream()
                .map(message -> {
                    boolean isSender = message.getSender().getId().equals(userId);
                    UUID otherUserId = isSender ?
                            message.getRecipient().getId() :
                            message.getSender().getId();

                    Long unreadCount = messageRepository.countUnreadInConversation(
                            userId, otherUserId, message.getListing().getId()
                    );

                    return messageMapper.toConversationResponse(message, userId, unreadCount);
                })
                .toList();
    }
}
