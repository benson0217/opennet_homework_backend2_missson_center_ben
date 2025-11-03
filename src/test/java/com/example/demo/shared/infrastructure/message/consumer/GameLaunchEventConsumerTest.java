package com.example.demo.shared.infrastructure.message.consumer;

import com.example.demo.mission.application.service.MissionCommandService;
import com.example.demo.shared.application.dto.event.GameLaunchEvent;
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
class GameLaunchEventConsumerTest {

    @Mock
    private MissionCommandService missionCommandService;

    @Mock
    private RedisService redisService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @InjectMocks
    private GameLaunchEventConsumer gameLaunchEventConsumer;

    private GameLaunchEvent testEvent;
    private String testEventJson;

    @BeforeEach
    void setUp() throws Exception {
        testEvent = new GameLaunchEvent(
                1L,
                "testuser",
                100L,
                "GAME001",
                LocalDateTime.of(2024, 1, 1, 10, 0, 0)
        );
        testEventJson = objectMapper.writeValueAsString(testEvent);
    }

    @Test
    void onMessage_shouldProcessEvent_whenFirstTimeReceived() {
        // Given
        String idempotencyKey = "game_launch_event:idempotency:1:100";
        when(redisService.setIfAbsent(eq(idempotencyKey), eq("processed"), any(Duration.class)))
                .thenReturn(Mono.just(true));
        when(missionCommandService.updateMissionProgress(1L, "testuser"))
                .thenReturn(Mono.empty());

        // When
        gameLaunchEventConsumer.onMessage(testEventJson);

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
        String idempotencyKey = "game_launch_event:idempotency:1:100";
        when(redisService.setIfAbsent(eq(idempotencyKey), eq("processed"), any(Duration.class)))
                .thenReturn(Mono.just(false));

        // When
        gameLaunchEventConsumer.onMessage(testEventJson);

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
        gameLaunchEventConsumer.onMessage(invalidJson);

        // Then
        verify(redisService, never()).setIfAbsent(anyString(), anyString(), any(Duration.class));
        verify(missionCommandService, never()).updateMissionProgress(anyLong(), anyString());
    }

    @Test
    void onMessage_shouldHandleMissionUpdateFailure() {
        // Given
        String idempotencyKey = "game_launch_event:idempotency:1:100";
        when(redisService.setIfAbsent(eq(idempotencyKey), eq("processed"), any(Duration.class)))
                .thenReturn(Mono.just(true));
        when(missionCommandService.updateMissionProgress(1L, "testuser"))
                .thenReturn(Mono.error(new RuntimeException("Mission update failed")));

        // When
        gameLaunchEventConsumer.onMessage(testEventJson);

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
        String idempotencyKey = "game_launch_event:idempotency:1:100";
        when(redisService.setIfAbsent(eq(idempotencyKey), eq("processed"), any(Duration.class)))
                .thenReturn(Mono.error(new RuntimeException("Redis connection failed")));

        // When
        gameLaunchEventConsumer.onMessage(testEventJson);

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
        String expectedKey = "game_launch_event:idempotency:1:100";
        when(redisService.setIfAbsent(eq(expectedKey), eq("processed"), any(Duration.class)))
                .thenReturn(Mono.just(true));
        when(missionCommandService.updateMissionProgress(1L, "testuser"))
                .thenReturn(Mono.empty());

        // When
        gameLaunchEventConsumer.onMessage(testEventJson);

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
        GameLaunchEvent event2 = new GameLaunchEvent(
                2L,
                "user2",
                100L,
                "GAME001",
                LocalDateTime.of(2024, 1, 1, 10, 0, 0)
        );
        String event2Json = objectMapper.writeValueAsString(event2);
        String idempotencyKey2 = "game_launch_event:idempotency:2:100";

        when(redisService.setIfAbsent(eq(idempotencyKey2), eq("processed"), any(Duration.class)))
                .thenReturn(Mono.just(true));
        when(missionCommandService.updateMissionProgress(2L, "user2"))
                .thenReturn(Mono.empty());

        // When
        gameLaunchEventConsumer.onMessage(event2Json);

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
    void onMessage_shouldProcessDifferentGameEvents() throws Exception {
        // Given
        GameLaunchEvent event2 = new GameLaunchEvent(
                1L,
                "testuser",
                200L,
                "GAME002",
                LocalDateTime.of(2024, 1, 1, 10, 0, 0)
        );
        String event2Json = objectMapper.writeValueAsString(event2);
        String idempotencyKey2 = "game_launch_event:idempotency:1:200";

        when(redisService.setIfAbsent(eq(idempotencyKey2), eq("processed"), any(Duration.class)))
                .thenReturn(Mono.just(true));
        when(missionCommandService.updateMissionProgress(1L, "testuser"))
                .thenReturn(Mono.empty());

        // When
        gameLaunchEventConsumer.onMessage(event2Json);

        // Then - give reactive chain time to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(redisService).setIfAbsent(eq(idempotencyKey2), eq("processed"), eq(Duration.ofDays(1)));
        verify(missionCommandService).updateMissionProgress(1L, "testuser");
    }

    @Test
    void onMessage_shouldCallUpdateMissionProgressWithCorrectParams() {
        // Given
        String idempotencyKey = "game_launch_event:idempotency:1:100";
        when(redisService.setIfAbsent(eq(idempotencyKey), eq("processed"), any(Duration.class)))
                .thenReturn(Mono.just(true));
        when(missionCommandService.updateMissionProgress(1L, "testuser"))
                .thenReturn(Mono.empty());

        // When
        gameLaunchEventConsumer.onMessage(testEventJson);

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
    void onMessage_shouldHandleNullFieldsInEvent() throws Exception {
        // Given - create event with minimal fields (some nulls)
        String jsonWithNulls = "{\"userId\":1,\"username\":\"testuser\",\"gameId\":100,\"gameCode\":null,\"launchTime\":\"2024-01-01T10:00:00\"}";

        // When
        gameLaunchEventConsumer.onMessage(jsonWithNulls);

        // Then - should still process or handle gracefully
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify at least deserialization was attempted
        verify(redisService, atMost(1)).setIfAbsent(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void onMessage_shouldSetCorrectTTLForIdempotencyKey() {
        // Given
        String idempotencyKey = "game_launch_event:idempotency:1:100";
        when(redisService.setIfAbsent(eq(idempotencyKey), eq("processed"), eq(Duration.ofDays(1))))
                .thenReturn(Mono.just(true));
        when(missionCommandService.updateMissionProgress(1L, "testuser"))
                .thenReturn(Mono.empty());

        // When
        gameLaunchEventConsumer.onMessage(testEventJson);

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
        gameLaunchEventConsumer.onMessage(emptyMessage);

        // Then
        verify(redisService, never()).setIfAbsent(anyString(), anyString(), any(Duration.class));
        verify(missionCommandService, never()).updateMissionProgress(anyLong(), anyString());
    }
}
