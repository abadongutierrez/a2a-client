package com.jabaddon.learning.a2a.a2aclient;

import io.a2a.A2A;
import io.a2a.client.A2AClient;
import io.a2a.spec.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

@SpringBootApplication
public class A2aClientApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(A2aClientApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Application started successfully! Running post-initialization logic...");
        
        try {
            A2AClient client = new A2AClient("http://localhost:9090/a2a");

            AgentCard agentCard = client.getAgentCard("/a2a/.well-known/agent.json", Map.of());
            System.out.println("Agent Name: " + agentCard);

            Message userMessage = A2A.toUserMessage("tell me a story about cats");
            MessageSendParams messageSendParams = new MessageSendParams.Builder().message(userMessage).build();

            SendMessageResponse response = client.sendMessage(messageSendParams);

            System.out.println("Agent Response: " + response.getResult());
            
            switch (response.getResult()) {
                case Task task -> {
                    System.out.println("Task ID: " + task.getId());
                    System.out.println("Task Status: " + task.getStatus());
                    Part<?> first = task.getArtifacts().getFirst().parts().getFirst();
                    System.out.println("Artifact: " + first.getKind());
                    
                    switch (first) {
                        case DataPart dataPart -> System.out.println("data: " + dataPart.getData());
                        default -> System.out.println("Artifact Content is empty.");
                    }
                }
                default -> System.out.println("Received non-task response: " + response.getResult());
            }

        } catch (Exception e) {
            System.err.println("Error calling A2A agent: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
