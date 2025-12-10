package com.z.c.woodexcess_api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.z.c.woodexcess_api.config.RateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageRateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties rateLimitProperties;
    private final Map<String, Bucket> messageCache = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {


        if (!request.getRequestURI().startsWith("/api/messages") || !"POST".equals(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        String userEmail = authentication.getName();
        Bucket bucket = resolveBucket(userEmail);

        if (bucket.tryConsume(1)) {
            log.debug("[MESSAGE_RATE_LIMIT] Message allowed for user: {}", userEmail);
            filterChain.doFilter(request, response);
        } else {
            log.warn("[MESSAGE_RATE_LIMIT] Message blocked for user: {} - Too many messages", userEmail);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            Map<String, Object> errorResponse = Map.of(
                    "status", 429,
                    "error", "Too Many Requests",
                    "message", "Too many messages sent. Please wait before sending more messages.",
                    "path", request.getRequestURI()
            );

            new ObjectMapper().writeValue(response.getWriter(), errorResponse);
        }
    }

    private Bucket resolveBucket(String userEmail) {
        return messageCache.computeIfAbsent(userEmail, k -> createNewBucket());
    }

    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(
                rateLimitProperties.getMessageCapacity(),
                Refill.intervally(
                        rateLimitProperties.getMessageRefillTokens(),
                        Duration.ofMinutes(rateLimitProperties.getMessageRefillMinutes())
                )
        );
        return Bucket.builder().addLimit(limit).build();
    }

    @Scheduled(fixedRate = 3600000) // cada 1 hora
    public void cleanupOldMessageBuckets() {
        int sizeBefore = messageCache.size();
        messageCache.entrySet().removeIf(entry ->
                entry.getValue().getAvailableTokens() == rateLimitProperties.getMessageCapacity()
        );
        int sizeAfter = messageCache.size();

        if (sizeBefore > sizeAfter) {
            log.info("[MESSAGE_RATE_LIMIT] Cleaned up {} inactive message buckets", sizeBefore - sizeAfter);
        }
    }
}
