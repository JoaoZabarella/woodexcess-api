package com.z.c.woodexcess_api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.rate-limit")
@Getter
@Setter
public class RateLimitProperties {

    private LoginRateLimit login = new LoginRateLimit();
    private MessageRateLimit message = new MessageRateLimit();
    private WebSocketRateLimit websocket = new WebSocketRateLimit();

    @Getter
    @Setter
    public static class LoginRateLimit {
        private int capacity;
        private int refillTokens;
        private int refillMinutes;
    }

    @Getter
    @Setter
    public static class MessageRateLimit {
        private int capacity;
        private int refillTokens;
        private int refillMinutes;
    }

    @Getter
    @Setter
    public static class WebSocketRateLimit {
        private int capacity;
        private int refillTokens;
        private int refillMinutes;
    }
}
