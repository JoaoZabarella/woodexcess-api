package com.z.c.woodexcess_api.controller;

import com.z.c.woodexcess_api.dto.message.ChatMessageDTO;
import com.z.c.woodexcess_api.dto.message.MessageRequest;
import com.z.c.woodexcess_api.dto.message.MessageResponse;
import com.z.c.woodexcess_api.service.message.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;

import java.util.UUID;

import static java.util.UUID.fromString;


@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(
            @Valid @Payload ChatMessageDTO chatMessage,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID senderId = fromString(userDetails.getUsername());

        log.info("WebSocket message from {} to {} about listing {}",
                senderId, chatMessage.recipientId(), chatMessage.listingId());

        MessageRequest request = new MessageRequest(
                chatMessage.recipientId(),
                chatMessage.listingId(),
                chatMessage.content()
        );

        MessageResponse saved = messageService.sendMessage(senderId, request);
        ChatMessageDTO payload = ChatMessageDTO.fromMessageResponse(saved);

        // Recipient
        messagingTemplate.convertAndSendToUser(
                saved.recipientId().toString(),
                "/queue/messages",
                payload
        );

        // Sender
        messagingTemplate.convertAndSendToUser(
                saved.senderId().toString(),
                "/queue/messages",
                payload
        );
    }


    @MessageMapping("/chat.typing")
    public void typing(
            @Payload ChatMessageDTO typing,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID senderId = fromString(userDetails.getUsername());

        log.debug("Typing notification from {} to {} (listing={})",
                senderId, typing.recipientId(), typing.listingId());

        messagingTemplate.convertAndSendToUser(
                typing.recipientId().toString(),
                "/queue/typing",
                new TypingNotification(senderId, typing.listingId())
        );
    }

    private record  TypingNotification(UUID userId, UUID listingId) {}
}
