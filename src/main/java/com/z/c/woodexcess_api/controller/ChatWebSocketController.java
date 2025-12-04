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

        CustomUserDetails userDetails = (CustomUserDetails)
                ((UsernamePasswordAuthenticationToken) principal).getPrincipal();
        UUID senderId = userDetails.getId();

        log.info("WebSocket message from {} to {} about listing {}",
                senderId, chatMessage.recipientId(), chatMessage.listingId());

        MessageRequest request = new MessageRequest(
                chatMessage.recipientId(),
                chatMessage.listingId(),
                chatMessage.content()
        );

        MessageResponse saved = messageService.sendMessage(senderId, request);
        ChatMessageDTO payload = ChatMessageDTO.fromMessageResponse(saved);

        String senderEmail = userDetails.getUsername();

        messagingTemplate.convertAndSendToUser(
                saved.recipientEmail(),
                "/queue/messages",
                payload
        );

        messagingTemplate.convertAndSendToUser(
                senderEmail,
                "/queue/messages",
                payload
        );
    }

    @MessageMapping("/chat.typing")
    public void typing(
            @Payload ChatMessageDTO typing,
            Principal principal) {

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
    }

    private record TypingNotification(UUID userId, UUID listingId) {}
}
