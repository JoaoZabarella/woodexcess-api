package com.z.c.woodexcess_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class WoodexcessApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(WoodexcessApiApplication.class, args);
    }
}

