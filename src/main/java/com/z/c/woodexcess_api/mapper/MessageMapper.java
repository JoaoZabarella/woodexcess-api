package com.z.c.woodexcess_api.mapper;

import com.z.c.woodexcess_api.dto.message.ConversationResponse;
import com.z.c.woodexcess_api.dto.message.MessageResponse;
import com.z.c.woodexcess_api.model.Message;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class MessageMapper {

    public MessageResponse toResponse(Message message){
        return new MessageResponse(
                message.getId(),
                message.getSender().getId(),
                message.getSender().getEmail(),
                message.getSender().getName(),
                message.getRecipient().getId(),
                message.getRecipient().getEmail(),
                message.getRecipient().getName(),
                message.getListing().getId(),
                message.getListing().getTitle(),
                message.getContent(),
                message.getIsRead(),
                message.getCreatedAt(),
                message.getUpdatedAt()
        );
    }

    public ConversationResponse toConversationResponse(
            Message message,
            UUID currentUserId,
            Long unreadCount){

        boolean isSender = message.getSender().getId().equals(currentUserId);
        UUID otherUserId = isSender ? message.getRecipient().getId() :
                message.getSender().getId();
        String otherUserName = isSender ? message.getRecipient().getName() :
                message.getSender().getName();

        return new ConversationResponse(
                message.getListing().getId(),
                message.getListing().getTitle(),
                otherUserId,
                otherUserName,
                toResponse(message),
                unreadCount,
                null
        );
    }
}
