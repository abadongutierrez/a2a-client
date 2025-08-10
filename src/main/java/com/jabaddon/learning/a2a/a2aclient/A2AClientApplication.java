package com.jabaddon.learning.a2a.a2aclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

@SpringBootApplication
public class A2AClientApplication implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(A2AClientApplication.class);

    
    private final A2aServiceImpl a2aService;
    
    public A2AClientApplication(A2aServiceImpl a2aService) {
        this.a2aService = a2aService;
    }

    public static void main(String[] args) {
        SpringApplication.run(A2AClientApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            a2aService.getAgentCard(Map.of());

            a2aService.sendMessage("tell me a story about cats");
            
            a2aService.sendStreamingMessage("tell me a story about cats");
            
        } catch (Exception e) {
            logger.error("An error occurred: {}", e.getMessage());
            throw e;
        }
    }

}
