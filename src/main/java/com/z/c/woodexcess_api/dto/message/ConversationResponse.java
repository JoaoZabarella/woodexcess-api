package com.z.c.woodexcess_api.dto.message;

import java.util.UUID;
import java.util.List;
public record ConversationResponse(
        UUID listingId,
        String listingTitle,
        UUID otherUserId,
        String otherUsername,
        MessageResponse message,
        Long unreadCount,
        List<MessageResponse> recentMessages
) {
}
