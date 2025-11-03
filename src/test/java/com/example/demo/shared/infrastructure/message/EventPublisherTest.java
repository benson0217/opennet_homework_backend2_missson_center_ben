package com.example.demo.shared.infrastructure.message;

import com.example.demo.shared.application.dto.event.UserLoginEvent;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventPublisherTest {

    @Mock
    private RocketMQTemplate rocketMQTemplate;

    private EventPublisher eventPublisher;

    private UserLoginEvent testEvent;

    @BeforeEach
    void setUp() {
        eventPublisher = new EventPublisher(rocketMQTemplate);
        testEvent = new UserLoginEvent(1L, "testuser", LocalDateTime.now());
    }

    @Test
    void publishEvent_shouldPublishSuccessfully_whenValidEvent() {
        // Given
        String topic = "test-topic";
        ArgumentCaptor<SendCallback> callbackCaptor = ArgumentCaptor.forClass(SendCallback.class);

        doAnswer(invocation -> {
            SendCallback callback = invocation.getArgument(2);
            callback.onSuccess(mock(SendResult.class));
            return null;
        }).when(rocketMQTemplate).asyncSend(anyString(), any(Message.class), any(SendCallback.class));

        // When & Then
        StepVerifier.create(eventPublisher.publishEvent(topic, testEvent))
                .verifyComplete();

        verify(rocketMQTemplate).asyncSend(eq("task-center-test-topic"), any(Message.class), callbackCaptor.capture());
    }

    @Test
    void publishEvent_shouldPrependTopicPrefix() {
        // Given
        String topic = "my-topic";
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);

        doAnswer(invocation -> {
            SendCallback callback = invocation.getArgument(2);
            callback.onSuccess(mock(SendResult.class));
            return null;
        }).when(rocketMQTemplate).asyncSend(anyString(), any(Message.class), any(SendCallback.class));

        // When
        StepVerifier.create(eventPublisher.publishEvent(topic, testEvent))
                .verifyComplete();

        // Then
        verify(rocketMQTemplate).asyncSend(topicCaptor.capture(), any(Message.class), any(SendCallback.class));
        assertEquals("task-center-my-topic", topicCaptor.getValue());
    }

    @Test
    void publishEvent_shouldPropagateError_whenRocketMQFails() {
        // Given
        String topic = "test-topic";
        RuntimeException rocketMQError = new RuntimeException("RocketMQ error");

        doAnswer(invocation -> {
            SendCallback callback = invocation.getArgument(2);
            callback.onException(rocketMQError);
            return null;
        }).when(rocketMQTemplate).asyncSend(anyString(), any(Message.class), any(SendCallback.class));

        // When & Then
        StepVerifier.create(eventPublisher.publishEvent(topic, testEvent))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("RocketMQ error"))
                .verify();

        verify(rocketMQTemplate).asyncSend(anyString(), any(Message.class), any(SendCallback.class));
    }

    @Test
    void publishEvent_shouldHandleSerializationError() {
        // Given
        String topic = "test-topic";
        // Create an object that will fail serialization by creating a circular reference
        Object problematicEvent = new Object() {
        };

        // When & Then
        StepVerifier.create(eventPublisher.publishEvent(topic, problematicEvent))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().contains("發布事件失敗"))
                .verify();

        verify(rocketMQTemplate, never()).asyncSend(anyString(), any(Message.class), any(SendCallback.class));
    }

    @Test
    void publishEventWithRetry_shouldSucceed_onFirstAttempt() {
        // Given
        String topic = "test-topic";

        doAnswer(invocation -> {
            SendCallback callback = invocation.getArgument(2);
            callback.onSuccess(mock(SendResult.class));
            return null;
        }).when(rocketMQTemplate).asyncSend(anyString(), any(Message.class), any(SendCallback.class));

        // When & Then
        StepVerifier.create(eventPublisher.publishEventWithRetry(topic, testEvent))
                .verifyComplete();

        verify(rocketMQTemplate, times(1)).asyncSend(anyString(), any(Message.class), any(SendCallback.class));
    }


    @Test
    void publishLoginEvent_shouldUseCorrectTopic() {
        // Given
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);

        doAnswer(invocation -> {
            SendCallback callback = invocation.getArgument(2);
            callback.onSuccess(mock(SendResult.class));
            return null;
        }).when(rocketMQTemplate).asyncSend(anyString(), any(Message.class), any(SendCallback.class));

        // When
        StepVerifier.create(eventPublisher.publishLoginEvent(testEvent))
                .verifyComplete();

        // Then
        verify(rocketMQTemplate).asyncSend(topicCaptor.capture(), any(Message.class), any(SendCallback.class));
        assertEquals("task-center-user-login", topicCaptor.getValue());
    }

    @Test
    void publishGameLaunchEvent_shouldUseCorrectTopic() {
        // Given
        UserLoginEvent gameLaunchEvent = testEvent; // Use serializable event
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);

        doAnswer(invocation -> {
            SendCallback callback = invocation.getArgument(2);
            callback.onSuccess(mock(SendResult.class));
            return null;
        }).when(rocketMQTemplate).asyncSend(anyString(), any(Message.class), any(SendCallback.class));

        // When
        StepVerifier.create(eventPublisher.publishGameLaunchEvent(gameLaunchEvent))
                .verifyComplete();

        // Then
        verify(rocketMQTemplate).asyncSend(topicCaptor.capture(), any(Message.class), any(SendCallback.class));
        assertEquals("task-center-game-launch", topicCaptor.getValue());
    }

    @Test
    void publishGamePlayEvent_shouldUseCorrectTopic() {
        // Given
        UserLoginEvent gamePlayEvent = testEvent; // Use serializable event
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);

        doAnswer(invocation -> {
            SendCallback callback = invocation.getArgument(2);
            callback.onSuccess(mock(SendResult.class));
            return null;
        }).when(rocketMQTemplate).asyncSend(anyString(), any(Message.class), any(SendCallback.class));

        // When
        StepVerifier.create(eventPublisher.publishGamePlayEvent(gamePlayEvent))
                .verifyComplete();

        // Then
        verify(rocketMQTemplate).asyncSend(topicCaptor.capture(), any(Message.class), any(SendCallback.class));
        assertEquals("task-center-game-play", topicCaptor.getValue());
    }

    @Test
    void publishMissionCompletedEvent_shouldUseCorrectTopic() {
        // Given
        UserLoginEvent missionCompletedEvent = testEvent; // Use serializable event
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);

        doAnswer(invocation -> {
            SendCallback callback = invocation.getArgument(2);
            callback.onSuccess(mock(SendResult.class));
            return null;
        }).when(rocketMQTemplate).asyncSend(anyString(), any(Message.class), any(SendCallback.class));

        // When
        StepVerifier.create(eventPublisher.publishMissionCompletedEvent(missionCompletedEvent))
                .verifyComplete();

        // Then
        verify(rocketMQTemplate).asyncSend(topicCaptor.capture(), any(Message.class), any(SendCallback.class));
        assertEquals("task-center-mission-completed", topicCaptor.getValue());
    }


    @Test
    void publishEvent_shouldSerializeEventCorrectly() {
        // Given
        String topic = "test-topic";
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

        doAnswer(invocation -> {
            SendCallback callback = invocation.getArgument(2);
            callback.onSuccess(mock(SendResult.class));
            return null;
        }).when(rocketMQTemplate).asyncSend(anyString(), any(Message.class), any(SendCallback.class));

        // When
        StepVerifier.create(eventPublisher.publishEvent(topic, testEvent))
                .verifyComplete();

        // Then
        verify(rocketMQTemplate).asyncSend(anyString(), messageCaptor.capture(), any(SendCallback.class));
        Message capturedMessage = messageCaptor.getValue();
        assertNotNull(capturedMessage);
        assertNotNull(capturedMessage.getPayload());
        assertInstanceOf(String.class, capturedMessage.getPayload());
        String payload = (String) capturedMessage.getPayload();
        assertTrue(payload.contains("testuser"));
    }

    @Test
    void eventPublishException_shouldContainCause() {
        // Given
        RuntimeException cause = new RuntimeException("Original error");
        String message = "Failed after retries";

        // When
        EventPublisher.EventPublishException exception = 
                new EventPublisher.EventPublishException(message, cause);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

}
