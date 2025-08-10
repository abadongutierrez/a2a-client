package com.jabaddon.learning.a2a.a2aclient;

import io.a2a.A2A;
import io.a2a.client.A2AClient;
import io.a2a.spec.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service
public class A2aServiceImpl {
    
    private static final Logger logger = LoggerFactory.getLogger(A2aServiceImpl.class);
    private static final int STREAMING_TIMEOUT_SECONDS = 60;
    private static final TimeUnit STREAMING_TIMEOUT_UNIT = TimeUnit.SECONDS;

    private final A2aConfiguration configuration;
    
    public A2aServiceImpl(A2aConfiguration configuration) {
        this.configuration = configuration;
    }
    
    public void getAgentCard(Map<String, String> parameters) throws Exception {
        logger.debug("Creating A2A client with URL: {}", configuration.getUrl());
        A2AClient client = new A2AClient(configuration.getUrl());
        String cardPath = configuration.getCard().getPath();
        
        logger.info("Fetching agent card from path: {}", cardPath);
        AgentCard agentCard = client.getAgentCard(cardPath, parameters);
        logger.info("Agent card retrieved: {}", agentCard);

    }
    
    public void sendMessage(String message) throws Exception {
        logger.debug("Creating A2A client with URL: {}", configuration.getUrl());
        A2AClient client = new A2AClient(configuration.getUrl());
        MessageSendParams params = buildMessageSendParams(message);
        
        logger.info("Sending non-streaming message: {}", message);
        SendMessageResponse response = client.sendMessage(params);
        logger.info("Received response: {}", response.getResult());
        
        switch (response.getResult()) {
            case Task task -> {
                logger.info("Task ID: {}, Status: {}", task.getId(), task.getStatus());
                logArtifacts(task.getArtifacts());
            }
            default -> logger.info("Received non-task response: {}", response.getResult());
        }

    }
    
    public void sendStreamingMessage(String message) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        logger.debug("Creating A2A client with URL: {}", configuration.getUrl());
        A2AClient client = new A2AClient(configuration.getUrl());
        MessageSendParams params = buildMessageSendParams(message);
        CountDownLatch latch1 = new CountDownLatch(1);

        // Wrap handlers to countdown latch on completion
        Consumer<StreamingEventKind> wrappedEventHandler = event -> {
            getDefaultEventHandler(latch).accept(event);
            if (shouldCountDown(event)) {
                latch1.countDown();
            }
        };

        Consumer<JSONRPCError> wrappedErrorHandler = error -> {
            getDefaultErrorHandler(latch).accept(error);
            latch1.countDown();
        };

        Runnable wrappedFailureHandler = () -> {
            getDefaultFailureHandler(latch).run();
            latch1.countDown();
        };

        logger.info("Sending streaming message: {}", message);
        client.sendStreamingMessage(params, wrappedEventHandler, wrappedErrorHandler, wrappedFailureHandler);

        logger.info("Listening for streaming events...");
        boolean completed = latch1.await(STREAMING_TIMEOUT_SECONDS, STREAMING_TIMEOUT_UNIT);

        if (!completed) {
            logger.info("Streaming session timed out after {} {}", (long) STREAMING_TIMEOUT_SECONDS, STREAMING_TIMEOUT_UNIT.toString().toLowerCase());
        } else {
            logger.info("Streaming session completed");
        }

    }

    private Consumer<StreamingEventKind> getDefaultEventHandler(CountDownLatch latch) {
        return event -> {
            logger.info("Received event: {}", event.getClass().getSimpleName());
            
            if (event instanceof Message responseMessage) {
                logger.info("Message ID: {}", responseMessage.getMessageId());
                logParts(responseMessage.getParts());
            } else if (event instanceof Task task) {
                logger.info("Task ID: {}, Status: {}", task.getId(), task.getStatus());
                if (task.getStatus().state() == TaskState.COMPLETED) {
                    logArtifacts(task.getArtifacts());
                }
            } else if (event instanceof TaskStatusUpdateEvent statusEvent) {
                logger.info("Task status updated to: {}", statusEvent.getStatus().state());
            } else if (event instanceof TaskArtifactUpdateEvent artifactEvent) {
                logger.info("New artifact received:");
                logArtifacts(List.of(artifactEvent.getArtifact()));
            }
        };
    }
    
    private Consumer<JSONRPCError> getDefaultErrorHandler(CountDownLatch latch) {
        return error -> {
            logger.error("Error. message={}, code={}", error.getMessage(), error.getCode());
            if (error.getData() != null) {
                logger.error("Error data: {}", error.getData());
            }
        };
    }
    
    private Runnable getDefaultFailureHandler(CountDownLatch latch) {
        return () -> logger.error("Connection failed or interrupted");
    }
    
    private boolean shouldCountDown(StreamingEventKind event) {
        return (event instanceof Task task && task.getStatus().state() == TaskState.COMPLETED) ||
               (event instanceof TaskStatusUpdateEvent statusEvent && statusEvent.getStatus().state() == TaskState.COMPLETED);
    }

    private MessageSendParams buildMessageSendParams(String text) {
        Message userMessage = A2A.toUserMessage(text);
        userMessage.setTaskId("task-" + System.currentTimeMillis());
        userMessage.setContextId("context-" + System.currentTimeMillis());
        MessageSendConfiguration configuration = new MessageSendConfiguration.Builder()
                .acceptedOutputModes(List.of("text"))
                .blocking(false)
                .build();
        MessageSendParams params = new MessageSendParams.Builder()
                .message(userMessage).
                configuration(configuration).
                build();
        return params;
    }

    private void logArtifacts(List<Artifact> artifacts) {
        if (artifacts.isEmpty()) {
            logger.info("No artifacts available.");
            return;
        }

        for (Artifact artifact : artifacts) {
            logger.info("Artifact ID: {}", artifact.artifactId());
            List<Part<?>> parts = artifact.parts();
            logParts(parts);
        }
    }

    private void logParts(List<Part<?>> parts) {
        parts.forEach(part -> {
            switch (part) {
                case DataPart dataPart -> logger.info("Data: {}", dataPart.getData());
                case TextPart textPart -> logger.info("Text: {}", textPart.getText());
                case FilePart filePart -> logger.info("File: {}", filePart.getFile().name());
                default -> logger.info("Unknown part kind: {}", part.getKind());
            }
        });
    }
}