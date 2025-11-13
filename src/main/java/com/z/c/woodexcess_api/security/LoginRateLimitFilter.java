package com.z.c.woodexcess_api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {


    @Value("${security.login.rate-limit.capacity}")
    private int capacity;

    @Value("${security.login.rate-limit.refill-tokens}")
    private int refillTokens;

    @Value("${security.login.rate-limit.refill-minutes}")
    private int refillMinutes;

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        if(!request.getRequestURI().startsWith("/api/auth/login") || !request.getMethod().equals("POST")) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = getClientIdentifier(request);
        Bucket bucket = resolveBucket(key);

        if(bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            Map<String, Object> errorResponse = Map.of(
                    "status", 429,
                    "error", "Too Many Requests",
                    "message", "Too many login attempts. PLease try again later.",
                    "path", request.getRequestURI()
            );

            new ObjectMapper().writeValue(response.getWriter(), errorResponse);
        }
    }

    private Bucket resolveBucket(String key){
        return cache.computeIfAbsent(key, k -> createNewBucket());
    }

    private Bucket createNewBucket(){
        Bandwidth limit = Bandwidth.classic(capacity,
                Refill.intervally(refillTokens, Duration.ofMinutes(refillMinutes)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
    private String getClientIdentifier(HttpServletRequest request){
        String ip = request.getHeader("X-Forwarded-For");
        if(ip == null || ip.isEmpty()){
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
