package com.z.c.woodexcess_api.dto.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;


@Builder
public record MessageResponse (

     UUID id,
     UUID senderId,
     String senderName,
     UUID receiverId,
     String receiverName,
     UUID listingId,
     String listingTitle,
     String content,
     Boolean isRead,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime updatedAt

    ){

}
