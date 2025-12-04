package com.z.c.woodexcess_api.controller;

import com.z.c.woodexcess_api.dto.message.ConversationResponse;
import com.z.c.woodexcess_api.dto.message.MessageRequest;
import com.z.c.woodexcess_api.dto.message.MessageResponse;
import com.z.c.woodexcess_api.security.CustomUserDetails; // ✅ ADICIONAR IMPORT
import com.z.c.woodexcess_api.service.message.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Messages", description = "Endpoints for user messaging system")
@SecurityRequirement(name = "bearerAuth")
public class MessageController {

    private final MessageService messageService;


    @PostMapping
    @Operation(
            summary = "Send a message",
            description = "Sends a message from authenticated user to another user about a specific listing"
    )
    @ApiResponse(responseCode = "201", description = "Message sent successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or trying to message yourself")
    @ApiResponse(responseCode = "404", description = "Recipient or listing not found")
    public ResponseEntity<MessageResponse> sendMessage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody MessageRequest request) {

        UUID senderId = userDetails.getId();
        MessageResponse response = messageService.sendMessage(senderId, request);

        log.info("Message sent from {} to {}", senderId, request.recipientId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/conversation")
    @Operation(
            summary = "Get conversation history",
            description = "Retrieves all messages between two users about a specific listing, ordered chronologically"
    )
    @ApiResponse(responseCode = "200", description = "Conversation retrieved successfully")
    @ApiResponse(responseCode = "404", description = "User or listing not found")
    public ResponseEntity<List<MessageResponse>> getConversation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "Other user ID in the conversation")
            @RequestParam UUID otherUserId,
            @Parameter(description = "Listing ID context")
            @RequestParam UUID listingId) {

        UUID currentUserId = userDetails.getId();
        List<MessageResponse> messages = messageService.getConversation(
                currentUserId, otherUserId, listingId
        );

        log.debug("Retrieved {} messages for conversation", messages.size());
        return ResponseEntity.ok(messages);
    }


    @GetMapping("/listing/{listingId}")
    @Operation(
            summary = "Get messages by listing",
            description = "Retrieves all messages for a specific listing where user is sender or recipient (paginated)"
    )
    @ApiResponse(responseCode = "200", description = "Messages retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Listing not found")
    public ResponseEntity<Page<MessageResponse>> getMessagesByListing(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "Listing ID")
            @PathVariable UUID listingId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        UUID userId = userDetails.getId();
        Page<MessageResponse> messages = messageService.getMessagesByListing(
                listingId, userId, pageable
        );

        log.debug("Retrieved page {} of messages for listing {}", pageable.getPageNumber(), listingId);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/conversations")
    @Operation(
            summary = "Get recent conversations",
            description = "Retrieves list of recent conversations with last message and unread count"
    )
    @ApiResponse(responseCode = "200", description = "Conversations retrieved successfully")
    public ResponseEntity<List<ConversationResponse>> getRecentConversations(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UUID userId = userDetails.getId();
        List<ConversationResponse> conversations = messageService.getRecentConversations(userId);

        log.debug("Retrieved {} recent conversations for user {}", conversations.size(), userId);
        return ResponseEntity.ok(conversations);
    }


    @GetMapping("/unread-count")
    @Operation(
            summary = "Get unread message count",
            description = "Returns total number of unread messages for authenticated user across all conversations"
    )
    @ApiResponse(responseCode = "200", description = "Unread count retrieved successfully")
    public ResponseEntity<Long> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UUID userId = userDetails.getId();
        Long count = messageService.getUnreadCount(userId);

        log.debug("User {} has {} unread messages", userId, count);
        return ResponseEntity.ok(count);
    }


    @PatchMapping("/mark-read")
    @Operation(
            summary = "Mark conversation as read",
            description = "Marks all messages from a specific sender about a listing as read"
    )
    @ApiResponse(responseCode = "204", description = "Messages marked as read successfully")
    @ApiResponse(responseCode = "404", description = "User or listing not found")
    public ResponseEntity<Void> markConversationAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "Sender user ID")
            @RequestParam UUID senderId,
            @Parameter(description = "Listing ID context")
            @RequestParam UUID listingId) {

        UUID recipientId = userDetails.getId();
        messageService.markConversationAsRead(recipientId, senderId, listingId);

        log.info("Marked messages as read: recipient={}, sender={}, listing={}",
                recipientId, senderId, listingId);
        return ResponseEntity.noContent().build();
    }
}
