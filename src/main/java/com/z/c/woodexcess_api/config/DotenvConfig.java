package com.z.c.woodexcess_api.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

public class DotenvConfig implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();

        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        Map<String, Object> envMap = new HashMap<>();

        dotenv.entries().forEach(entry -> {
            envMap.put(entry.getKey(), entry.getValue());
            System.out.println("✅ Loaded: " + entry.getKey() + " = " +
                    (entry.getKey().contains("PASSWORD") || entry.getKey().contains("SECRET")
                            ? "***HIDDEN***"
                            : entry.getValue()));
        });

        environment.getPropertySources().addFirst(new MapPropertySource("dotenvProperties", envMap));

        System.out.println("🚀 DotenvConfig initialized with " + envMap.size() + " variables");
    }
}
