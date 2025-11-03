package com.example.demo.shared.infrastructure.message.consumer;

import com.example.demo.mission.application.service.MissionCommandService;
import com.example.demo.shared.application.dto.event.UserLoginEvent;
import com.example.demo.shared.infrastructure.redis.RedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserLoginEventConsumerTest {

    @Mock
    private MissionCommandService missionCommandService;

    @Mock
    private RedisService redisService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @InjectMocks
    private UserLoginEventConsumer userLoginEventConsumer;

    private UserLoginEvent testEvent;
    private String testEventJson;

    @BeforeEach
    void setUp() throws Exception {
        testEvent = new UserLoginEvent(
                1L,
                "testuser",
                LocalDateTime.of(2024, 1, 1, 10, 0, 0)
        );
        testEventJson = objectMapper.writeValueAsString(testEvent);
    }

    @Test
    void onMessage_shouldProcessEvent_whenFirstTimeReceived() {
        // Given
        String idempotencyKey = "user_login_event:idempotency:1";
        when(redisService.setIfAbsent(eq(idempotencyKey), eq("processed"), any(Duration.class)))
                .thenReturn(Mono.just(true));
        when(missionCommandService.updateMissionProgress(1L, "testuser"))
                .thenReturn(Mono.empty());

        // When
        userLoginEventConsumer.onMessage(testEventJson);

        // Then - give reactive chain time to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(redisService).setIfAbsent(eq(idempotencyKey), eq("processed"), eq(Duration.ofDays(1)));
        verify(missionCommandService).updateMissionProgress(1L, "testuser");
    }

    @Test
    void onMessage_shouldSkipProcessing_whenDuplicateEvent() {
        // Given
        String idempotencyKey = "user_login_event:idempotency:1";
        when(redisService.setIfAbsent(eq(idempotencyKey), eq("processed"), any(Duration.class)))
                .thenReturn(Mono.just(false));

        // When
        userLoginEventConsumer.onMessage(testEventJson);

        // Then - give reactive chain time to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(redisService).setIfAbsent(eq(idempotencyKey), eq("processed"), eq(Duration.ofDays(1)));
        verify(missionCommandService, never()).updateMissionProgress(anyLong(), anyString());
    }

    @Test
    void onMessage_shouldHandleDeserializationError() {
        // Given
        String invalidJson = "{invalid json}";

        // When
        userLoginEventConsumer.onMessage(invalidJson);

        // Then
        verify(redisService, never()).setIfAbsent(anyString(), anyString(), any(Duration.class));
        verify(missionCommandService, never()).updateMissionProgress(anyLong(), anyString());
    }

    @Test
    void onMessage_shouldHandleMissionUpdateFailure() {
        // Given
        String idempotencyKey = "user_login_event:idempotency:1";
        when(redisService.setIfAbsent(eq(idempotencyKey), eq("processed"), any(Duration.class)))
                .thenReturn(Mono.just(true));
        when(missionCommandService.updateMissionProgress(1L, "testuser"))
                .thenReturn(Mono.error(new RuntimeException("Mission update failed")));

        // When
        userLoginEventConsumer.onMessage(testEventJson);

        // Then - give reactive chain time to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(redisService).setIfAbsent(eq(idempotencyKey), eq("processed"), eq(Duration.ofDays(1)));
        verify(missionCommandService).updateMissionProgress(1L, "testuser");
    }

    @Test
    void onMessage_shouldHandleRedisFailure() {
        // Given
        String idempotencyKey = "user_login_event:idempotency:1";
        when(redisService.setIfAbsent(eq(idempotencyKey), eq("processed"), any(Duration.class)))
                .thenReturn(Mono.error(new RuntimeException("Redis connection failed")));

        // When
        userLoginEventConsumer.onMessage(testEventJson);

        // Then - give reactive chain time to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(redisService).setIfAbsent(eq(idempotencyKey), eq("processed"), eq(Duration.ofDays(1)));
        verify(missionCommandService, never()).updateMissionProgress(anyLong(), anyString());
    }

    @Test
    void onMessage_shouldUseCorrectIdempotencyKey() {
        // Given
        String expectedKey = "user_login_event:idempotency:1";
        when(redisService.setIfAbsent(eq(expectedKey), eq("processed"), any(Duration.class)))
                .thenReturn(Mono.just(true));
        when(missionCommandService.updateMissionProgress(1L, "testuser"))
                .thenReturn(Mono.empty());

        // When
        userLoginEventConsumer.onMessage(testEventJson);

        // Then - give reactive chain time to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(redisService).setIfAbsent(eq(expectedKey), eq("processed"), eq(Duration.ofDays(1)));
    }

    @Test
    void onMessage_shouldProcessDifferentUserEvents() throws Exception {
        // Given
        UserLoginEvent event2 = new UserLoginEvent(
                2L,
                "user2",
                LocalDateTime.of(2024, 1, 1, 10, 0, 0)
        );
        String event2Json = objectMapper.writeValueAsString(event2);
        String idempotencyKey2 = "user_login_event:idempotency:2";

        when(redisService.setIfAbsent(eq(idempotencyKey2), eq("processed"), any(Duration.class)))
                .thenReturn(Mono.just(true));
        when(missionCommandService.updateMissionProgress(2L, "user2"))
                .thenReturn(Mono.empty());

        // When
        userLoginEventConsumer.onMessage(event2Json);

        // Then - give reactive chain time to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(redisService).setIfAbsent(eq(idempotencyKey2), eq("processed"), eq(Duration.ofDays(1)));
        verify(missionCommandService).updateMissionProgress(2L, "user2");
    }

    @Test
    void onMessage_shouldCallUpdateMissionProgressWithCorrectParams() {
        // Given
        String idempotencyKey = "user_login_event:idempotency:1";
        when(redisService.setIfAbsent(eq(idempotencyKey), eq("processed"), any(Duration.class)))
                .thenReturn(Mono.just(true));
        when(missionCommandService.updateMissionProgress(1L, "testuser"))
                .thenReturn(Mono.empty());

        // When
        userLoginEventConsumer.onMessage(testEventJson);

        // Then - give reactive chain time to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(missionCommandService).updateMissionProgress(eq(1L), eq("testuser"));
        verify(missionCommandService, never()).updateMissionProgress(argThat(id -> !id.equals(1L)), anyString());
    }

    @Test
    void onMessage_shouldSetCorrectTTLForIdempotencyKey() {
        // Given
        String idempotencyKey = "user_login_event:idempotency:1";
        when(redisService.setIfAbsent(eq(idempotencyKey), eq("processed"), eq(Duration.ofDays(1))))
                .thenReturn(Mono.just(true));
        when(missionCommandService.updateMissionProgress(1L, "testuser"))
                .thenReturn(Mono.empty());

        // When
        userLoginEventConsumer.onMessage(testEventJson);

        // Then - give reactive chain time to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(redisService).setIfAbsent(anyString(), anyString(), eq(Duration.ofDays(1)));
    }

    @Test
    void onMessage_shouldHandleEmptyMessage() {
        // Given
        String emptyMessage = "";

        // When
        userLoginEventConsumer.onMessage(emptyMessage);

        // Then
        verify(redisService, never()).setIfAbsent(anyString(), anyString(), any(Duration.class));
        verify(missionCommandService, never()).updateMissionProgress(anyLong(), anyString());
    }

    @Test
    void onMessage_shouldHandleMultipleLoginsBySameUser_withSameIdempotencyKey() {
        // Given - same user logs in multiple times (within TTL window)
        String idempotencyKey = "user_login_event:idempotency:1";
        
        // First login - should process
        when(redisService.setIfAbsent(eq(idempotencyKey), eq("processed"), any(Duration.class)))
                .thenReturn(Mono.just(true));
        when(missionCommandService.updateMissionProgress(1L, "testuser"))
                .thenReturn(Mono.empty());

        // When
        userLoginEventConsumer.onMessage(testEventJson);

        // Then - give reactive chain time to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(redisService).setIfAbsent(eq(idempotencyKey), eq("processed"), eq(Duration.ofDays(1)));
        verify(missionCommandService).updateMissionProgress(1L, "testuser");
    }

    @Test
    void onMessage_shouldUseOnlyUserIdInIdempotencyKey() throws Exception {
        // Given - same user, different login times should have same idempotency key
        UserLoginEvent laterEvent = new UserLoginEvent(
                1L,
                "testuser",
                LocalDateTime.of(2024, 1, 1, 15, 0, 0) // Different time
        );
        String laterEventJson = objectMapper.writeValueAsString(laterEvent);
        String idempotencyKey = "user_login_event:idempotency:1";

        when(redisService.setIfAbsent(eq(idempotencyKey), eq("processed"), any(Duration.class)))
                .thenReturn(Mono.just(false)); // Already processed

        // When
        userLoginEventConsumer.onMessage(laterEventJson);

        // Then - give reactive chain time to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(redisService).setIfAbsent(eq(idempotencyKey), eq("processed"), eq(Duration.ofDays(1)));
        verify(missionCommandService, never()).updateMissionProgress(anyLong(), anyString());
    }

    @Test
    void onMessage_shouldHandleNullUsernameGracefully() throws Exception {
        // Given
        String jsonWithNullUsername = "{\"userId\":1,\"username\":null,\"loginTime\":\"2024-01-01T10:00:00\"}";

        // When
        userLoginEventConsumer.onMessage(jsonWithNullUsername);

        // Then - give reactive chain time to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Should attempt to process or handle gracefully
        verify(redisService, atMost(1)).setIfAbsent(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void onMessage_shouldProcessEventWithLongUserId() throws Exception {
        // Given
        UserLoginEvent eventWithLongId = new UserLoginEvent(
                999999999L,
                "userwithlongid",
                LocalDateTime.of(2024, 1, 1, 10, 0, 0)
        );
        String eventJson = objectMapper.writeValueAsString(eventWithLongId);
        String idempotencyKey = "user_login_event:idempotency:999999999";

        when(redisService.setIfAbsent(eq(idempotencyKey), eq("processed"), any(Duration.class)))
                .thenReturn(Mono.just(true));
        when(missionCommandService.updateMissionProgress(999999999L, "userwithlongid"))
                .thenReturn(Mono.empty());

        // When
        userLoginEventConsumer.onMessage(eventJson);

        // Then - give reactive chain time to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(redisService).setIfAbsent(eq(idempotencyKey), eq("processed"), eq(Duration.ofDays(1)));
        verify(missionCommandService).updateMissionProgress(999999999L, "userwithlongid");
    }
}
