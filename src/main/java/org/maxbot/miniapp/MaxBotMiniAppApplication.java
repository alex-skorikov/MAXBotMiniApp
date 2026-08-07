package org.maxbot.miniapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.LocalDateTime;

@SpringBootApplication
public class MaxBotMiniAppApplication {

    private static final Logger log = LoggerFactory.getLogger(MaxBotMiniAppApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(MaxBotMiniAppApplication.class, args);
        log.info("BUILD VERSION 8: {}", LocalDateTime.now());
    }
}
