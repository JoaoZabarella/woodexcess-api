package com.z.c.woodexcess_api.config;

import com.z.c.woodexcess_api.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtProvider jwtProvider;
    private final UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            log.warn("StompHeaderAccessor is null");
            return message;
        }

        StompCommand command = accessor.getCommand();
        log.debug("WebSocket command: {}", command);

        if (StompCommand.CONNECT.equals(command)) {
            authenticateWebSocketConnection(accessor);
        } else if (StompCommand.SEND.equals(command)) {
            if (accessor.getUser() == null) {
                log.error("CRITICAL: User is NULL on SEND command! Session: {}", accessor.getSessionId());
            } else {
                log.debug("SEND command - User: {}", accessor.getUser().getName());
            }
        }

        return message;
    }

    private void authenticateWebSocketConnection(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");

        log.info("WebSocket CONNECT attempt - Session: {}", accessor.getSessionId());
        log.debug("Authorization header present: {}", authHeader != null);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.error("WebSocket CONNECT failed: Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7);
        log.debug("Token length: {} characters", token.length());

        try {
            if (!jwtProvider.validateJwtToken(token)) {
                log.error("WebSocket CONNECT failed: Invalid or expired JWT token");
                return;
            }

            String username = jwtProvider.getEmailFromToken(token);
            log.info("JWT token valid for user: {}", username);

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            accessor.setUser(authentication);

            log.info("WebSocket authenticated successfully: user={}, session={}",
                    username, accessor.getSessionId());

        } catch (Exception e) {
            log.error("WebSocket authentication exception: {}", e.getMessage(), e);
        }
    }
}
