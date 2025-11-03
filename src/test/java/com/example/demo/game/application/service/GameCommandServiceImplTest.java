package com.example.demo.game.application.service;

import com.example.demo.game.application.service.impl.GameCommandServiceImpl;
import com.example.demo.game.domain.model.Game;
import com.example.demo.game.domain.model.GameLaunchRecord;
import com.example.demo.game.domain.model.GamePlayRecord;
import com.example.demo.game.domain.repository.GameLaunchRecordRepository;
import com.example.demo.game.domain.repository.GamePlayRecordRepository;
import com.example.demo.shared.application.dto.event.GameLaunchEvent;
import com.example.demo.shared.application.dto.event.GamePlayEvent;
import com.example.demo.shared.infrastructure.message.EventPublisher;
import com.example.demo.user.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameCommandServiceImplTest {

    @Mock
    private GameLaunchRecordRepository gameLaunchRecordRepository;

    @Mock
    private GamePlayRecordRepository gamePlayRecordRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private GameQueryService gameQueryService;

    @InjectMocks
    private GameCommandServiceImpl gameCommandService;

    private User testUser;
    private Game testGame;
    private GameLaunchRecord testLaunchRecord;
    private GamePlayRecord testPlayRecord;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .points(100)
                .registrationDate(LocalDateTime.now().minusDays(5))
                .createdAt(LocalDateTime.now().minusDays(5))
                .updatedAt(LocalDateTime.now().minusDays(5))
                .build();

        testGame = Game.builder()
                .id(1L)
                .gameCode("GAME001")
                .gameName("Test Game")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testLaunchRecord = GameLaunchRecord.builder()
                .id(1L)
                .userId(1L)
                .gameId(1L)
                .launchTime(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        testPlayRecord = GamePlayRecord.builder()
                .id(1L)
                .userId(1L)
                .gameId(1L)
                .score(1500)
                .playDuration(300)
                .playTime(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void handleGameLaunch_shouldLaunchGame_whenUserAndGameExist() {
        // Given
        when(gameQueryService.findGameByCodeOrThrow("GAME001")).thenReturn(Mono.just(testGame));
        when(gameLaunchRecordRepository.existsByUserIdAndGameId(1L, 1L)).thenReturn(Mono.just(false));
        when(gameLaunchRecordRepository.save(any(GameLaunchRecord.class))).thenReturn(Mono.just(testLaunchRecord));
        when(eventPublisher.publishGameLaunchEvent(any(GameLaunchEvent.class))).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(gameCommandService.handleGameLaunch(testUser, "GAME001"))
                .verifyComplete();

        verify(gameQueryService).findGameByCodeOrThrow("GAME001");
        verify(gameLaunchRecordRepository).existsByUserIdAndGameId(1L, 1L);
        verify(gameLaunchRecordRepository).save(any(GameLaunchRecord.class));
        verify(eventPublisher).publishGameLaunchEvent(any(GameLaunchEvent.class));
    }

    @Test
    void handleGameLaunch_shouldSkipLaunch_whenGameAlreadyLaunched() {
        // Given
        when(gameQueryService.findGameByCodeOrThrow("GAME001")).thenReturn(Mono.just(testGame));
        when(gameLaunchRecordRepository.existsByUserIdAndGameId(1L, 1L)).thenReturn(Mono.just(true));

        // When & Then
        StepVerifier.create(gameCommandService.handleGameLaunch(testUser, "GAME001"))
                .verifyComplete();

        verify(gameQueryService).findGameByCodeOrThrow("GAME001");
        verify(gameLaunchRecordRepository).existsByUserIdAndGameId(1L, 1L);
        verify(gameLaunchRecordRepository, never()).save(any(GameLaunchRecord.class));
        verify(eventPublisher, never()).publishGameLaunchEvent(any(GameLaunchEvent.class));
    }

    @Test
    void handleGameLaunch_shouldThrowError_whenGameNotFound() {
        // Given
        when(gameQueryService.findGameByCodeOrThrow("GAME001")).thenReturn(Mono.error(new IllegalArgumentException("找不到遊戲: GAME001")));

        // When & Then
        StepVerifier.create(gameCommandService.handleGameLaunch(testUser, "GAME001"))
                .expectErrorMatches(throwable -> 
                    throwable instanceof IllegalArgumentException && 
                    throwable.getMessage().contains("找不到遊戲"))
                .verify();

        verify(gameQueryService).findGameByCodeOrThrow("GAME001");
        verify(gameLaunchRecordRepository, never()).save(any(GameLaunchRecord.class));
    }

    @Test
    void handleGameLaunch_shouldThrowError_whenGameNotAvailable() {
        // Given
        Game inactiveGame = testGame.toBuilder().isActive(false).build();
        when(gameQueryService.findGameByCodeOrThrow("GAME001")).thenReturn(Mono.just(inactiveGame));
        when(gameLaunchRecordRepository.existsByUserIdAndGameId(1L, 1L)).thenReturn(Mono.just(false));

        // When & Then
        StepVerifier.create(gameCommandService.handleGameLaunch(testUser, "GAME001"))
                .expectErrorMatches(throwable -> 
                    throwable instanceof IllegalStateException && 
                    throwable.getMessage().contains("遊戲不可用"))
                .verify();

        verify(gameQueryService).findGameByCodeOrThrow("GAME001");
        verify(gameLaunchRecordRepository, never()).save(any(GameLaunchRecord.class));
    }

    @Test
    void handleGameLaunch_shouldPublishEvent_withCorrectData() {
        // Given
        when(gameQueryService.findGameByCodeOrThrow("GAME001")).thenReturn(Mono.just(testGame));
        when(gameLaunchRecordRepository.existsByUserIdAndGameId(1L, 1L)).thenReturn(Mono.just(false));
        when(gameLaunchRecordRepository.save(any(GameLaunchRecord.class))).thenReturn(Mono.just(testLaunchRecord));
        when(eventPublisher.publishGameLaunchEvent(any(GameLaunchEvent.class))).thenReturn(Mono.empty());

        // When
        StepVerifier.create(gameCommandService.handleGameLaunch(testUser, "GAME001"))
                .verifyComplete();

        // Then
        ArgumentCaptor<GameLaunchEvent> eventCaptor = ArgumentCaptor.forClass(GameLaunchEvent.class);
        verify(eventPublisher).publishGameLaunchEvent(eventCaptor.capture());

        GameLaunchEvent publishedEvent = eventCaptor.getValue();
        assertEquals(1L, publishedEvent.userId());
        assertEquals("testuser", publishedEvent.username());
        assertEquals(1L, publishedEvent.gameId());
        assertEquals("GAME001", publishedEvent.gameCode());
        assertNotNull(publishedEvent.launchTime());
    }

    @Test
    void handleGameLaunch_shouldPropagateError_whenEventPublishingFails() {
        // Given
        RuntimeException publishError = new RuntimeException("Event publishing failed");
        when(gameQueryService.findGameByCodeOrThrow("GAME001")).thenReturn(Mono.just(testGame));
        when(gameLaunchRecordRepository.existsByUserIdAndGameId(1L, 1L)).thenReturn(Mono.just(false));
        when(gameLaunchRecordRepository.save(any(GameLaunchRecord.class))).thenReturn(Mono.just(testLaunchRecord));
        when(eventPublisher.publishGameLaunchEvent(any(GameLaunchEvent.class))).thenReturn(Mono.error(publishError));

        // When & Then
        StepVerifier.create(gameCommandService.handleGameLaunch(testUser, "GAME001"))
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException && 
                    throwable.getMessage().equals("Event publishing failed"))
                .verify();

        verify(eventPublisher).publishGameLaunchEvent(any(GameLaunchEvent.class));
    }

    @Test
    void handleGameLaunch_shouldNotPublishEvent_whenUserNotEligibleForMissions() {
        // Given - user registered more than 30 days ago
        User ineligibleUser = User.builder()
                .id(5L)
                .username("olduser")
                .points(100)
                .registrationDate(LocalDateTime.now().minusDays(35)) // Registered 35 days ago
                .createdAt(LocalDateTime.now().minusDays(35))
                .updatedAt(LocalDateTime.now().minusDays(35))
                .build();

        when(gameQueryService.findGameByCodeOrThrow("GAME001")).thenReturn(Mono.just(testGame));
        when(gameLaunchRecordRepository.existsByUserIdAndGameId(5L, 1L)).thenReturn(Mono.just(false));
        when(gameLaunchRecordRepository.save(any(GameLaunchRecord.class))).thenReturn(Mono.just(testLaunchRecord));

        // When & Then
        StepVerifier.create(gameCommandService.handleGameLaunch(ineligibleUser, "GAME001"))
                .verifyComplete();

        verify(gameQueryService).findGameByCodeOrThrow("GAME001");
        verify(gameLaunchRecordRepository).existsByUserIdAndGameId(5L, 1L);
        verify(gameLaunchRecordRepository).save(any(GameLaunchRecord.class));
        // Event should NOT be published for ineligible users
        verify(eventPublisher, never()).publishGameLaunchEvent(any(GameLaunchEvent.class));
    }

    @Test
    void handleGamePlay_shouldRecordPlay_whenUserAndGameExist() {
        // Given
        when(gameQueryService.findGameByCodeOrThrow("GAME001")).thenReturn(Mono.just(testGame));
        when(gamePlayRecordRepository.save(any(GamePlayRecord.class))).thenReturn(Mono.just(testPlayRecord));
        when(eventPublisher.publishGamePlayEvent(any(GamePlayEvent.class))).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(gameCommandService.handleGamePlay(testUser, "GAME001", 1500, 300))
                .verifyComplete();

        verify(gameQueryService).findGameByCodeOrThrow("GAME001");
        verify(gamePlayRecordRepository).save(any(GamePlayRecord.class));
        verify(eventPublisher).publishGamePlayEvent(any(GamePlayEvent.class));
    }

    @Test
    void handleGamePlay_shouldThrowError_whenGameNotFound() {
        // Given
        when(gameQueryService.findGameByCodeOrThrow("GAME001")).thenReturn(Mono.error(new IllegalArgumentException("找不到遊戲: GAME001")));

        // When & Then
        StepVerifier.create(gameCommandService.handleGamePlay(testUser, "GAME001", 1500, 300))
                .expectErrorMatches(throwable -> 
                    throwable instanceof IllegalArgumentException && 
                    throwable.getMessage().contains("找不到遊戲"))
                .verify();

        verify(gameQueryService).findGameByCodeOrThrow("GAME001");
        verify(gamePlayRecordRepository, never()).save(any(GamePlayRecord.class));
    }

    @Test
    void handleGamePlay_shouldThrowError_whenGameNotActive() {
        // Given
        Game inactiveGame = testGame.toBuilder().isActive(false).build();
        when(gameQueryService.findGameByCodeOrThrow("GAME001")).thenReturn(Mono.just(inactiveGame));

        // When & Then
        StepVerifier.create(gameCommandService.handleGamePlay(testUser, "GAME001", 1500, 300))
                .expectErrorMatches(throwable -> 
                    throwable instanceof IllegalStateException && 
                    throwable.getMessage().contains("遊戲未啟用"))
                .verify();

        verify(gameQueryService).findGameByCodeOrThrow("GAME001");
        verify(gamePlayRecordRepository, never()).save(any(GamePlayRecord.class));
    }

    @Test
    void handleGamePlay_shouldPublishEvent_withCorrectData() {
        // Given
        when(gameQueryService.findGameByCodeOrThrow("GAME001")).thenReturn(Mono.just(testGame));
        when(gamePlayRecordRepository.save(any(GamePlayRecord.class))).thenReturn(Mono.just(testPlayRecord));
        when(eventPublisher.publishGamePlayEvent(any(GamePlayEvent.class))).thenReturn(Mono.empty());

        // When
        StepVerifier.create(gameCommandService.handleGamePlay(testUser, "GAME001", 1500, 300))
                .verifyComplete();

        // Then
        ArgumentCaptor<GamePlayEvent> eventCaptor = ArgumentCaptor.forClass(GamePlayEvent.class);
        verify(eventPublisher).publishGamePlayEvent(eventCaptor.capture());

        GamePlayEvent publishedEvent = eventCaptor.getValue();
        assertEquals(1L, publishedEvent.userId());
        assertEquals("testuser", publishedEvent.username());
        assertEquals(1L, publishedEvent.gameId());
        assertEquals("GAME001", publishedEvent.gameCode());
        assertEquals(1500, publishedEvent.score());
        assertEquals(300, publishedEvent.playDuration());
        assertNotNull(publishedEvent.playTime());
    }

    @Test
    void handleGamePlay_shouldPropagateError_whenEventPublishingFails() {
        // Given
        RuntimeException publishError = new RuntimeException("Event publishing failed");
        when(gameQueryService.findGameByCodeOrThrow("GAME001")).thenReturn(Mono.just(testGame));
        when(gamePlayRecordRepository.save(any(GamePlayRecord.class))).thenReturn(Mono.just(testPlayRecord));
        when(eventPublisher.publishGamePlayEvent(any(GamePlayEvent.class))).thenReturn(Mono.error(publishError));

        // When & Then
        StepVerifier.create(gameCommandService.handleGamePlay(testUser, "GAME001", 1500, 300))
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException && 
                    throwable.getMessage().equals("Event publishing failed"))
                .verify();

        verify(eventPublisher).publishGamePlayEvent(any(GamePlayEvent.class));
    }

    @Test
    void handleGamePlay_shouldNotPublishEvent_whenUserNotEligibleForMissions() {
        // Given - user registered more than 30 days ago
        User ineligibleUser = User.builder()
                .id(5L)
                .username("olduser")
                .points(100)
                .registrationDate(LocalDateTime.now().minusDays(35)) // Registered 35 days ago
                .createdAt(LocalDateTime.now().minusDays(35))
                .updatedAt(LocalDateTime.now().minusDays(35))
                .build();

        when(gameQueryService.findGameByCodeOrThrow("GAME001")).thenReturn(Mono.just(testGame));
        when(gamePlayRecordRepository.save(any(GamePlayRecord.class))).thenReturn(Mono.just(testPlayRecord));

        // When & Then
        StepVerifier.create(gameCommandService.handleGamePlay(ineligibleUser, "GAME001", 1500, 300))
                .verifyComplete();

        verify(gameQueryService).findGameByCodeOrThrow("GAME001");
        verify(gamePlayRecordRepository).save(any(GamePlayRecord.class));
        // Event should NOT be published for ineligible users
        verify(eventPublisher, never()).publishGamePlayEvent(any(GamePlayEvent.class));
    }

    @Test
    void handleGamePlay_shouldSavePlayRecord_withCorrectData() {
        // Given
        when(gameQueryService.findGameByCodeOrThrow("GAME001")).thenReturn(Mono.just(testGame));
        when(gamePlayRecordRepository.save(any(GamePlayRecord.class))).thenReturn(Mono.just(testPlayRecord));
        when(eventPublisher.publishGamePlayEvent(any(GamePlayEvent.class))).thenReturn(Mono.empty());

        // When
        StepVerifier.create(gameCommandService.handleGamePlay(testUser, "GAME001", 1500, 300))
                .verifyComplete();

        // Then
        ArgumentCaptor<GamePlayRecord> recordCaptor = ArgumentCaptor.forClass(GamePlayRecord.class);
        verify(gamePlayRecordRepository).save(recordCaptor.capture());

        GamePlayRecord savedRecord = recordCaptor.getValue();
        assertEquals(1L, savedRecord.getUserId());
        assertEquals(1L, savedRecord.getGameId());
        assertEquals(1500, savedRecord.getScore());
        assertEquals(300, savedRecord.getPlayDuration());
    }

    @Test
    void handleGameLaunch_shouldSaveLaunchRecord_withCorrectData() {
        // Given
        when(gameQueryService.findGameByCodeOrThrow("GAME001")).thenReturn(Mono.just(testGame));
        when(gameLaunchRecordRepository.existsByUserIdAndGameId(1L, 1L)).thenReturn(Mono.just(false));
        when(gameLaunchRecordRepository.save(any(GameLaunchRecord.class))).thenReturn(Mono.just(testLaunchRecord));
        when(eventPublisher.publishGameLaunchEvent(any(GameLaunchEvent.class))).thenReturn(Mono.empty());

        // When
        StepVerifier.create(gameCommandService.handleGameLaunch(testUser, "GAME001"))
                .verifyComplete();

        // Then
        ArgumentCaptor<GameLaunchRecord> recordCaptor = ArgumentCaptor.forClass(GameLaunchRecord.class);
        verify(gameLaunchRecordRepository).save(recordCaptor.capture());

        GameLaunchRecord savedRecord = recordCaptor.getValue();
        assertEquals(1L, savedRecord.getUserId());
        assertEquals(1L, savedRecord.getGameId());
    }

    @Test
    void handleGamePlay_shouldHandleNullPlayDuration() {
        // Given
        when(gameQueryService.findGameByCodeOrThrow("GAME001")).thenReturn(Mono.just(testGame));
        when(gamePlayRecordRepository.save(any(GamePlayRecord.class))).thenReturn(Mono.just(testPlayRecord));
        when(eventPublisher.publishGamePlayEvent(any(GamePlayEvent.class))).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(gameCommandService.handleGamePlay(testUser, "GAME001", 1500, null))
                .verifyComplete();

        ArgumentCaptor<GamePlayRecord> recordCaptor = ArgumentCaptor.forClass(GamePlayRecord.class);
        verify(gamePlayRecordRepository).save(recordCaptor.capture());

        GamePlayRecord savedRecord = recordCaptor.getValue();
        assertNull(savedRecord.getPlayDuration());
    }

    @Test
    void handleGamePlay_shouldHandleZeroScore() {
        // Given
        when(gameQueryService.findGameByCodeOrThrow("GAME001")).thenReturn(Mono.just(testGame));
        when(gamePlayRecordRepository.save(any(GamePlayRecord.class))).thenReturn(Mono.just(testPlayRecord));
        when(eventPublisher.publishGamePlayEvent(any(GamePlayEvent.class))).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(gameCommandService.handleGamePlay(testUser, "GAME001", 0, 300))
                .verifyComplete();

        ArgumentCaptor<GamePlayRecord> recordCaptor = ArgumentCaptor.forClass(GamePlayRecord.class);
        verify(gamePlayRecordRepository).save(recordCaptor.capture());

        GamePlayRecord savedRecord = recordCaptor.getValue();
        assertEquals(0, savedRecord.getScore());
    }

    @Test
    void handleGameLaunch_shouldPropagateError_whenRepositoryFails() {
        // Given
        RuntimeException repositoryError = new RuntimeException("Database error");
        when(gameQueryService.findGameByCodeOrThrow("GAME001")).thenReturn(Mono.just(testGame));
        when(gameLaunchRecordRepository.existsByUserIdAndGameId(1L, 1L)).thenReturn(Mono.just(false));
        when(gameLaunchRecordRepository.save(any(GameLaunchRecord.class))).thenReturn(Mono.error(repositoryError));

        // When & Then
        StepVerifier.create(gameCommandService.handleGameLaunch(testUser, "GAME001"))
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException && 
                    throwable.getMessage().equals("Database error"))
                .verify();

        verify(gameLaunchRecordRepository).save(any(GameLaunchRecord.class));
        verify(eventPublisher, never()).publishGameLaunchEvent(any(GameLaunchEvent.class));
    }

    @Test
    void handleGamePlay_shouldPropagateError_whenRepositoryFails() {
        // Given
        RuntimeException repositoryError = new RuntimeException("Database error");
        when(gameQueryService.findGameByCodeOrThrow("GAME001")).thenReturn(Mono.just(testGame));
        when(gamePlayRecordRepository.save(any(GamePlayRecord.class))).thenReturn(Mono.error(repositoryError));

        // When & Then
        StepVerifier.create(gameCommandService.handleGamePlay(testUser, "GAME001", 1500, 300))
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException && 
                    throwable.getMessage().equals("Database error"))
                .verify();

        verify(gamePlayRecordRepository).save(any(GamePlayRecord.class));
        verify(eventPublisher, never()).publishGamePlayEvent(any(GamePlayEvent.class));
    }
}
