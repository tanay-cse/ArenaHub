package com.arenahub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ArenaHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArenaHubApplication.class, args);
    }
}
