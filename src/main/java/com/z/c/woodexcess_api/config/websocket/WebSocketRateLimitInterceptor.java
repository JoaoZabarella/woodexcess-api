package com.z.c.woodexcess_api.config.websocket;

import com.z.c.woodexcess_api.config.RateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketRateLimitInterceptor implements ChannelInterceptor {

    private final RateLimitProperties rateLimitProperties;


    @Getter
    private final Map<String, BucketWrapper> buckets = new ConcurrentHashMap<>();

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.SEND.equals(accessor.getCommand())) {
            String username = accessor.getUser() != null
                    ? accessor.getUser().getName()
                    : accessor.getSessionId();

            BucketWrapper wrapper = buckets.computeIfAbsent(username, this::createBucketWrapper);
            wrapper.updateLastAccess();

            if (!wrapper.getBucket().tryConsume(1)) {
                log.warn("WEBSOCKET_RATE_LIMIT: Rate limit exceeded for user: {}", username);
                return null;
            }

            log.debug("WEBSOCKET_RATE_LIMIT: Message allowed for user {} (remaining: {})",
                    username, wrapper.getBucket().getAvailableTokens());
        }

        return message;
    }

    private BucketWrapper createBucketWrapper(String username) {

        Bandwidth limit = Bandwidth.classic(
                rateLimitProperties.getWebsocket().getCapacity(),
                Refill.greedy(
                        rateLimitProperties.getWebsocket().getRefillTokens(),
                        Duration.ofMinutes(rateLimitProperties.getWebsocket().getRefillMinutes())
                )
        );
        Bucket bucket = Bucket.builder().addLimit(limit).build();

        log.info("=== WEBSOCKET RATE LIMIT BUCKET CREATED ===");
        log.info("User: {}", username);
        log.info("Capacity: {}", rateLimitProperties.getWebsocket().getCapacity());
        log.info("Refill: {} tokens every {} minutes",
                rateLimitProperties.getWebsocket().getRefillTokens(),
                rateLimitProperties.getWebsocket().getRefillMinutes());
        log.info("==========================================");

        return new BucketWrapper(bucket);
    }

    @Scheduled(fixedRate = 3600000) // 1 hora
    public void cleanupOldBuckets() {
        int sizeBefore = buckets.size();
        Instant oneHourAgo = Instant.now().minus(Duration.ofHours(1));

        buckets.entrySet().removeIf(entry ->
                entry.getValue().getLastAccess().isBefore(oneHourAgo)
        );

        int removed = sizeBefore - buckets.size();
        if (removed > 0) {
            log.info("WebSocket rate limit cleanup: removed {} old buckets", removed);
        }
    }


    public void clearBuckets() {
        buckets.clear();
        log.debug("All WebSocket rate limit buckets cleared");
    }

    private static class BucketWrapper {
        private final Bucket bucket;
        private Instant lastAccess;

        public BucketWrapper(Bucket bucket) {
            this.bucket = bucket;
            this.lastAccess = Instant.now();
        }

        public Bucket getBucket() {
            return bucket;
        }

        public Instant getLastAccess() {
            return lastAccess;
        }

        public void updateLastAccess() {
            this.lastAccess = Instant.now();
        }
    }
}
