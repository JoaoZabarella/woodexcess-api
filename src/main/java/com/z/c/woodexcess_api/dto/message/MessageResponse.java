package com.z.c.woodexcess_api.dto.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.z.c.woodexcess_api.model.Message;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;


@Builder
public record MessageResponse (

     UUID id,
     UUID senderId,
     String senderEmail,
     String senderName,
     UUID recipientId,
     String recipientEmail,
     String recipientName,
     UUID listingId,
     String listingTitle,
     String content,
     Boolean isRead,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime updatedAt

    ){
    public static MessageResponse fromMessage(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .senderId(message.getSender().getId())
                .senderEmail(message.getSender().getEmail())
                .senderName(message.getSender().getName())
                .recipientId(message.getRecipient().getId())
                .recipientEmail(message.getRecipient().getEmail())
                .recipientName(message.getRecipient().getName())
                .listingId(message.getListing().getId())
                .listingTitle(message.getListing().getTitle())
                .content(message.getContent())
                .isRead(message.getIsRead())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .build();
    }
}
