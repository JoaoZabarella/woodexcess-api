package com.z.c.woodexcess_api.security;

import com.z.c.woodexcess_api.config.RateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
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
@Order(3)
public class MessageRateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties rateLimitProperties;


    @Getter
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {


        if (request.getRequestURI().startsWith("/api/messages") && "POST".equalsIgnoreCase(request.getMethod())) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
                String userId = ((CustomUserDetails) authentication.getPrincipal()).getId().toString();
                Bucket bucket = buckets.computeIfAbsent(userId, this::createBucket);

                if (!bucket.tryConsume(1)) {
                    log.warn("MESSAGE_RATE_LIMIT: Rate limit exceeded for user: {}", userId);
                    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    response.setContentType("application/json");

                    response.getWriter().write("{\"error\":\"Too many messages. Please try again later.\"}");

                    response.getWriter().flush();
                    return;
                }

                log.debug("MESSAGE_RATE_LIMIT: Message allowed for user {} (remaining: {})",
                        userId, bucket.getAvailableTokens());
            }
        }

        filterChain.doFilter(request, response);
    }

    private Bucket createBucket(String userId) {

        Bandwidth limit = Bandwidth.classic(
                rateLimitProperties.getMessage().getCapacity(),
                Refill.greedy(
                        rateLimitProperties.getMessage().getRefillTokens(),
                        Duration.ofMinutes(rateLimitProperties.getMessage().getRefillMinutes())
                )
        );
        log.info("=== MESSAGE RATE LIMIT BUCKET CREATED ===");
        log.info("User: {}", userId);
        log.info("Capacity: {}", rateLimitProperties.getMessage().getCapacity());
        log.info("Refill: {} tokens every {} minutes",
                rateLimitProperties.getMessage().getRefillTokens(),
                rateLimitProperties.getMessage().getRefillMinutes());
        log.info("========================================");
        return Bucket.builder().addLimit(limit).build();
    }


    public void clearBuckets() {
        buckets.clear();
        log.debug("All HTTP rate limit buckets cleared");
    }
}
