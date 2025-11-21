package com.z.c.woodexcess_api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties (prefix = "security.login.rate-limit")
@Getter
@Setter
public class RateLimitProperties {

    private int capacity = 5;
    private int refillTokens = 5;
    private int refillMinutes = 15;
}
