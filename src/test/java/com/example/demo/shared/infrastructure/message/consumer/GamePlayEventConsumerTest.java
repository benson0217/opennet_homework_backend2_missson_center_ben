package com.example.demo.shared.infrastructure.message.consumer;

import com.example.demo.mission.application.service.MissionCommandService;
import com.example.demo.shared.application.dto.event.GamePlayEvent;
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
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GamePlayEventConsumerTest {

    @Mock
    private MissionCommandService missionCommandService;

    @Mock
    private RedisService redisService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @InjectMocks
    private GamePlayEventConsumer gamePlayEventConsumer;

    private GamePlayEvent testEvent;
    private String testEventJson;
    private LocalDateTime testPlayTime;

    @BeforeEach
    void setUp() throws Exception {
        testPlayTime = LocalDateTime.of(2024, 1, 1, 10, 0, 0);
        testEvent = new GamePlayEvent(
                1L,
                "testuser",
                100L,
                "GAME001",
                1500,
                300,
                testPlayTime
        );
        testEventJson = objectMapper.writeValueAsString(testEvent);
    }

    @Test
    void onMessage_shouldProcessEvent_whenFirstTimeReceived() {
        // Given
        long epochSecond = testPlayTime.toEpochSecond(ZoneOffset.UTC);
        String idempotencyKey = "game_play_event:idempotency:1:100:1500:" + epochSecond;
        when(redisService.setIfAbsent(eq(idempotencyKey), eq("processed"), any(Duration.class)))
                .thenReturn(Mono.just(true));
        when(missionCommandService.updateMissionProgress(1L, "testuser"))
                .thenReturn(Mono.empty());

        // When
        gamePlayEventConsumer.onMessage(testEventJson);

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
        long epochSecond = testPlayTime.toEpochSecond(ZoneOffset.UTC);
        String idempotencyKey = "game_play_event:idempotency:1:100:1500:" + epochSecond;
        when(redisService.setIfAbsent(eq(idempotencyKey), eq("processed"), any(Duration.class)))
                .thenReturn(Mono.just(false));

        // When
        gamePlayEventConsumer.onMessage(testEventJson);

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
        gamePlayEventConsumer.onMessage(invalidJson);

        // Then
        verify(redisService, never()).setIfAbsent(anyString(), anyString(), any(Duration.class));
        verify(missionCommandService, never()).updateMissionProgress(anyLong(), anyString());
    }

    @Test
    void onMessage_shouldHandleMissionUpdateFailure() {
        // Given
        long epochSecond = testPlayTime.toEpochSecond(ZoneOffset.UTC);
        String idempotencyKey = "game_play_event:idempotency:1:100:1500:" + epochSecond;
        when(redisService.setIfAbsent(eq(idempotencyKey), eq("processed"), any(Duration.class)))
                .thenReturn(Mono.just(true));
        when(missionCommandService.updateMissionProgress(1L, "testuser"))
                .thenReturn(Mono.error(new RuntimeException("Mission update failed")));

        // When
        gamePlayEventConsumer.onMessage(testEventJson);

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
        long epochSecond = testPlayTime.toEpochSecond(ZoneOffset.UTC);
        String idempotencyKey = "game_play_event:idempotency:1:100:1500:" + epochSecond;
        when(redisService.setIfAbsent(eq(idempotencyKey), eq("processed"), any(Duration.class)))
                .thenReturn(Mono.error(new RuntimeException("Redis connection failed")));

        // When
        gamePlayEventConsumer.onMessage(testEventJson);

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
    void onMessage_shouldProcessEventWithDifferentScore() throws Exception {
        // Given
        GamePlayEvent event2 = new GamePlayEvent(
                1L,
                "testuser",
                100L,
                "GAME001",
                2000,
                300,
                testPlayTime
        );
        String event2Json = objectMapper.writeValueAsString(event2);
        long epochSecond = testPlayTime.toEpochSecond(ZoneOffset.UTC);
        String idempotencyKey2 = "game_play_event:idempotency:1:100:2000:" + epochSecond;

        when(redisService.setIfAbsent(eq(idempotencyKey2), eq("processed"), any(Duration.class)))
                .thenReturn(Mono.just(true));
        when(missionCommandService.updateMissionProgress(1L, "testuser"))
                .thenReturn(Mono.empty());

        // When
        gamePlayEventConsumer.onMessage(event2Json);

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
    void onMessage_shouldProcessEventWithZeroScore() throws Exception {
        // Given
        GamePlayEvent event2 = new GamePlayEvent(
                1L,
                "testuser",
                100L,
                "GAME001",
                0,
                300,
                testPlayTime
        );
        String event2Json = objectMapper.writeValueAsString(event2);
        long epochSecond = testPlayTime.toEpochSecond(ZoneOffset.UTC);
        String idempotencyKey2 = "game_play_event:idempotency:1:100:0:" + epochSecond;

        when(redisService.setIfAbsent(eq(idempotencyKey2), eq("processed"), any(Duration.class)))
                .thenReturn(Mono.just(true));
        when(missionCommandService.updateMissionProgress(1L, "testuser"))
                .thenReturn(Mono.empty());

        // When
        gamePlayEventConsumer.onMessage(event2Json);

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
    void onMessage_shouldProcessEventWithNullPlayDuration() throws Exception {
        // Given
        GamePlayEvent event2 = new GamePlayEvent(
                1L,
                "testuser",
                100L,
                "GAME001",
                1500,
                null,
                testPlayTime
        );
        String event2Json = objectMapper.writeValueAsString(event2);
        long epochSecond = testPlayTime.toEpochSecond(ZoneOffset.UTC);
        String idempotencyKey2 = "game_play_event:idempotency:1:100:1500:" + epochSecond;

        when(redisService.setIfAbsent(eq(idempotencyKey2), eq("processed"), any(Duration.class)))
                .thenReturn(Mono.just(true));
        when(missionCommandService.updateMissionProgress(1L, "testuser"))
                .thenReturn(Mono.empty());

        // When
        gamePlayEventConsumer.onMessage(event2Json);

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
    void onMessage_shouldProcessDifferentUserEvents() throws Exception {
        // Given
        GamePlayEvent event2 = new GamePlayEvent(
                2L,
                "user2",
                100L,
                "GAME001",
                1500,
                300,
                testPlayTime
        );
        String event2Json = objectMapper.writeValueAsString(event2);
        long epochSecond = testPlayTime.toEpochSecond(ZoneOffset.UTC);
        String idempotencyKey2 = "game_play_event:idempotency:2:100:1500:" + epochSecond;

        when(redisService.setIfAbsent(eq(idempotencyKey2), eq("processed"), any(Duration.class)))
                .thenReturn(Mono.just(true));
        when(missionCommandService.updateMissionProgress(2L, "user2"))
                .thenReturn(Mono.empty());

        // When
        gamePlayEventConsumer.onMessage(event2Json);

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
        GamePlayEvent event2 = new GamePlayEvent(
                1L,
                "testuser",
                200L,
                "GAME002",
                1500,
                300,
                testPlayTime
        );
        String event2Json = objectMapper.writeValueAsString(event2);
        long epochSecond = testPlayTime.toEpochSecond(ZoneOffset.UTC);
        String idempotencyKey2 = "game_play_event:idempotency:1:200:1500:" + epochSecond;

        when(redisService.setIfAbsent(eq(idempotencyKey2), eq("processed"), any(Duration.class)))
                .thenReturn(Mono.just(true));
        when(missionCommandService.updateMissionProgress(1L, "testuser"))
                .thenReturn(Mono.empty());

        // When
        gamePlayEventConsumer.onMessage(event2Json);

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
        long epochSecond = testPlayTime.toEpochSecond(ZoneOffset.UTC);
        String idempotencyKey = "game_play_event:idempotency:1:100:1500:" + epochSecond;
        when(redisService.setIfAbsent(eq(idempotencyKey), eq("processed"), any(Duration.class)))
                .thenReturn(Mono.just(true));
        when(missionCommandService.updateMissionProgress(1L, "testuser"))
                .thenReturn(Mono.empty());

        // When
        gamePlayEventConsumer.onMessage(testEventJson);

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
        long epochSecond = testPlayTime.toEpochSecond(ZoneOffset.UTC);
        String idempotencyKey = "game_play_event:idempotency:1:100:1500:" + epochSecond;
        when(redisService.setIfAbsent(eq(idempotencyKey), eq("processed"), eq(Duration.ofDays(1))))
                .thenReturn(Mono.just(true));
        when(missionCommandService.updateMissionProgress(1L, "testuser"))
                .thenReturn(Mono.empty());

        // When
        gamePlayEventConsumer.onMessage(testEventJson);

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
        gamePlayEventConsumer.onMessage(emptyMessage);

        // Then
        verify(redisService, never()).setIfAbsent(anyString(), anyString(), any(Duration.class));
        verify(missionCommandService, never()).updateMissionProgress(anyLong(), anyString());
    }

    @Test
    void onMessage_shouldDistinguishEventsByPlayTime() throws Exception {
        // Given - same user, game, score but different play time
        LocalDateTime laterTime = testPlayTime.plusHours(1);
        GamePlayEvent event2 = new GamePlayEvent(
                1L,
                "testuser",
                100L,
                "GAME001",
                1500,
                300,
                laterTime
        );
        String event2Json = objectMapper.writeValueAsString(event2);
        long epochSecond2 = laterTime.toEpochSecond(ZoneOffset.UTC);
        String idempotencyKey2 = "game_play_event:idempotency:1:100:1500:" + epochSecond2;

        when(redisService.setIfAbsent(eq(idempotencyKey2), eq("processed"), any(Duration.class)))
                .thenReturn(Mono.just(true));
        when(missionCommandService.updateMissionProgress(1L, "testuser"))
                .thenReturn(Mono.empty());

        // When
        gamePlayEventConsumer.onMessage(event2Json);

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
    void onMessage_shouldHandleHighScore() throws Exception {
        // Given
        GamePlayEvent highScoreEvent = new GamePlayEvent(
                1L,
                "testuser",
                100L,
                "GAME001",
                999999,
                300,
                testPlayTime
        );
        String highScoreJson = objectMapper.writeValueAsString(highScoreEvent);
        long epochSecond = testPlayTime.toEpochSecond(ZoneOffset.UTC);
        String idempotencyKey = "game_play_event:idempotency:1:100:999999:" + epochSecond;

        when(redisService.setIfAbsent(eq(idempotencyKey), eq("processed"), any(Duration.class)))
                .thenReturn(Mono.just(true));
        when(missionCommandService.updateMissionProgress(1L, "testuser"))
                .thenReturn(Mono.empty());

        // When
        gamePlayEventConsumer.onMessage(highScoreJson);

        // Then - give reactive chain time to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(redisService).setIfAbsent(eq(idempotencyKey), eq("processed"), eq(Duration.ofDays(1)));
        verify(missionCommandService).updateMissionProgress(1L, "testuser");
    }
}
