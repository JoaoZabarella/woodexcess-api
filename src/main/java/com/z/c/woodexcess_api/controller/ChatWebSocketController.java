package com.z.c.woodexcess_api.controller;

import com.z.c.woodexcess_api.dto.message.ChatMessageDTO;
import com.z.c.woodexcess_api.dto.message.MessageRequest;
import com.z.c.woodexcess_api.dto.message.MessageResponse;
import com.z.c.woodexcess_api.security.CustomUserDetails;
import com.z.c.woodexcess_api.service.message.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(
            @Valid @Payload ChatMessageDTO chatMessage,
            Principal principal) {

        if (principal == null) {
            log.error("WebSocket message received without authentication (principal is null)");
            return;
        }

        log.debug("Principal type: {}, name: {}",
                principal.getClass().getSimpleName(), principal.getName());

        if (!(principal instanceof UsernamePasswordAuthenticationToken)) {
            log.error("Unexpected principal type: {}", principal.getClass().getName());
            return;
        }

        UsernamePasswordAuthenticationToken authToken =
                (UsernamePasswordAuthenticationToken) principal;

        Object principalObj = authToken.getPrincipal();
        if (principalObj == null) {
            log.error("Authentication token has null principal");
            return;
        }

        if (!(principalObj instanceof CustomUserDetails)) {
            log.error("Principal is not CustomUserDetails: {}", principalObj.getClass().getName());
            return;
        }

        CustomUserDetails userDetails = (CustomUserDetails) principalObj;
        UUID senderId = userDetails.getId();

        log.info("WebSocket message from {} (ID: {}) to {} about listing {}",
                userDetails.getUsername(), senderId,
                chatMessage.recipientId(), chatMessage.listingId());

        try {
            MessageRequest request = new MessageRequest(
                    chatMessage.recipientId(),
                    chatMessage.listingId(),
                    chatMessage.content()
            );

            MessageResponse saved = messageService.sendMessage(senderId, request);
            ChatMessageDTO payload = ChatMessageDTO.fromMessageResponse(saved);

            messagingTemplate.convertAndSendToUser(
                    saved.recipientEmail(),
                    "/queue/messages",
                    payload
            );

            messagingTemplate.convertAndSendToUser(
                    userDetails.getUsername(),
                    "/queue/messages",
                    payload
            );

            log.info("Message delivered successfully");

        } catch (Exception e) {
            log.error("Failed to send message via WebSocket: {}", e.getMessage(), e);
        }
    }

    @MessageMapping("/chat.typing")
    public void typing(
            @Payload ChatMessageDTO typing,
            Principal principal) {

        if (principal == null) {
            log.warn("Typing notification without authentication");
            return;
        }

        try {
            CustomUserDetails userDetails = (CustomUserDetails)
                    ((UsernamePasswordAuthenticationToken) principal).getPrincipal();
            UUID senderId = userDetails.getId();

            log.debug("Typing notification from {} to {} (listing={})",
                    senderId, typing.recipientId(), typing.listingId());

            messagingTemplate.convertAndSendToUser(
                    typing.recipientId().toString(),
                    "/queue/typing",
                    new TypingNotification(senderId, typing.listingId())
            );
        } catch (Exception e) {
            log.error("Failed to process typing notification: {}", e.getMessage());
        }
    }

    private record TypingNotification(UUID userId, UUID listingId) {}
}
