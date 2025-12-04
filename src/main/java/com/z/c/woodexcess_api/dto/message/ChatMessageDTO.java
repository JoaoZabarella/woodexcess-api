package com.z.c.woodexcess_api.dto.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record ChatMessageDTO(

        UUID id,

        @NotNull(message = "Recipient ID is required")
        UUID recipientId,

        @NotNull(message = "Listing ID is required")
        UUID listingId,

        @NotBlank(message = "Message cotent cannot be empty")
        @Size(max = 2000, message = "Message cannot exced 2000 characters")
        String content,

        UUID senderId,
        String senderName,
        LocalDateTime timesTamp

) {

    public static ChatMessageDTO fromMessageResponse(MessageResponse message){
        return new  ChatMessageDTO(
                message.id(),
                message.recipientId(),
                message.listingId(),
                message.content(),
                message.senderId(),
                message.senderName(),
                message.createdAt()
        );
    }
}
