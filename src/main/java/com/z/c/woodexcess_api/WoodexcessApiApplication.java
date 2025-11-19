package com.z.c.woodexcess_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
@EnableFeignClients
@EnableCaching
public class WoodexcessApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(WoodexcessApiApplication.class, args);
    }
}

