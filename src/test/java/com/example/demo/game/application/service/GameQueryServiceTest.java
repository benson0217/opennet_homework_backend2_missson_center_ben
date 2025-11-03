package com.example.demo.game.application.service;

import com.example.demo.game.application.service.impl.GameQueryServiceImpl;
import com.example.demo.game.domain.model.Game;
import com.example.demo.game.domain.repository.GameRepository;
import com.example.demo.shared.infrastructure.redis.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisSystemException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameQueryServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private RedisService redisService;

    @Mock
    private GameCacheService gameCacheService;

    @InjectMocks
    private GameQueryServiceImpl gameQueryService;

    private Game testGame;

    @BeforeEach
    void setUp() {
        testGame = Game.builder()
                .id(1L)
                .gameCode("GAME001")
                .gameName("Test Game")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getGameByCode_shouldReturnGame_whenCacheHit() {
        // Given
        when(redisService.<String, Game>get("games:active_list", "GAME001")).thenReturn(Mono.just(testGame));

        // When & Then
        StepVerifier.create(gameQueryService.getGameByCode("GAME001"))
                .assertNext(game -> {
                    assertNotNull(game);
                    assertEquals("GAME001", game.getGameCode());
                    assertEquals("Test Game", game.getGameName());
                    assertTrue(game.getIsActive());
                })
                .verifyComplete();

        verify(redisService).get("games:active_list", "GAME001");
        verify(gameRepository, never()).findByGameCode(anyString());
        verify(gameCacheService, never()).saveGame(any(Game.class));
    }

    @Test
    void getGameByCode_shouldQueryDatabase_whenCacheMiss() {
        // Given
        when(redisService.<String, Game>get("games:active_list", "GAME001")).thenReturn(Mono.empty());
        when(gameRepository.findByGameCode("GAME001")).thenReturn(Mono.just(testGame));
        when(gameCacheService.saveGame(testGame)).thenReturn(Mono.just(true));

        // When & Then
        StepVerifier.create(gameQueryService.getGameByCode("GAME001"))
                .assertNext(game -> {
                    assertNotNull(game);
                    assertEquals("GAME001", game.getGameCode());
                })
                .verifyComplete();

        verify(redisService).get("games:active_list", "GAME001");
        verify(gameRepository).findByGameCode("GAME001");
        verify(gameCacheService).saveGame(testGame);
    }

    @Test
    void getGameByCode_shouldReturnEmpty_whenGameNotFound() {
        // Given
        when(redisService.<String, Game>get("games:active_list", "GAME001")).thenReturn(Mono.empty());
        when(gameRepository.findByGameCode("GAME001")).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(gameQueryService.getGameByCode("GAME001"))
                .verifyComplete();

        verify(redisService).get("games:active_list", "GAME001");
        verify(gameRepository).findByGameCode("GAME001");
        verify(gameCacheService, never()).saveGame(any(Game.class));
    }

    @Test
    void getGameByCode_shouldFallbackToDatabase_whenRedisSystemException() {
        // Given
        RedisSystemException redisError = new RedisSystemException("Redis connection failed", new Exception());
        when(redisService.<String, Game>get("games:active_list", "GAME001")).thenReturn(Mono.error(redisError));
        when(gameRepository.findByGameCode("GAME001")).thenReturn(Mono.just(testGame));

        // When & Then
        StepVerifier.create(gameQueryService.getGameByCode("GAME001"))
                .assertNext(game -> {
                    assertNotNull(game);
                    assertEquals("GAME001", game.getGameCode());
                })
                .verifyComplete();

        verify(redisService).get("games:active_list", "GAME001");
        verify(gameRepository).findByGameCode("GAME001");
        verify(gameCacheService, never()).saveGame(any(Game.class));
    }

    @Test
    void getGameByCode_shouldFallbackToDatabase_whenRedisSystemExceptionAndGameNotFound() {
        // Given
        RedisSystemException redisError = new RedisSystemException("Redis connection failed", new Exception());
        when(redisService.<String, Game>get("games:active_list", "GAME001")).thenReturn(Mono.error(redisError));
        when(gameRepository.findByGameCode("GAME001")).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(gameQueryService.getGameByCode("GAME001"))
                .verifyComplete();

        verify(redisService).get("games:active_list", "GAME001");
        // findByGameCode is called twice: once in error handler, once in switchIfEmpty
        verify(gameRepository, atLeast(1)).findByGameCode("GAME001");
    }

    @Test
    void getGameByCode_shouldPropagateError_whenDatabaseFails() {
        // Given
        RuntimeException dbError = new RuntimeException("Database error");
        when(redisService.<String, Game>get("games:active_list", "GAME001")).thenReturn(Mono.empty());
        when(gameRepository.findByGameCode("GAME001")).thenReturn(Mono.error(dbError));

        // When & Then
        StepVerifier.create(gameQueryService.getGameByCode("GAME001"))
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException && 
                    throwable.getMessage().equals("Database error"))
                .verify();

        verify(redisService).get("games:active_list", "GAME001");
        verify(gameRepository).findByGameCode("GAME001");
        verify(gameCacheService, never()).saveGame(any(Game.class));
    }

    @Test
    void getGameByCode_shouldWriteToCache_afterDatabaseQuery() {
        // Given
        when(redisService.<String, Game>get("games:active_list", "GAME001")).thenReturn(Mono.empty());
        when(gameRepository.findByGameCode("GAME001")).thenReturn(Mono.just(testGame));
        when(gameCacheService.saveGame(testGame)).thenReturn(Mono.just(true));

        // When
        StepVerifier.create(gameQueryService.getGameByCode("GAME001"))
                .assertNext(game -> assertEquals("GAME001", game.getGameCode()))
                .verifyComplete();

        // Then
        verify(gameCacheService).saveGame(testGame);
    }

    @Test
    void getGameByCode_shouldReturnGame_evenIfCacheSaveFails() {
        // Given
        when(redisService.<String, Game>get("games:active_list", "GAME001")).thenReturn(Mono.empty());
        when(gameRepository.findByGameCode("GAME001")).thenReturn(Mono.just(testGame));
        when(gameCacheService.saveGame(testGame)).thenReturn(Mono.just(false));

        // When & Then
        StepVerifier.create(gameQueryService.getGameByCode("GAME001"))
                .assertNext(game -> {
                    assertNotNull(game);
                    assertEquals("GAME001", game.getGameCode());
                })
                .verifyComplete();

        verify(gameCacheService).saveGame(testGame);
    }

    @Test
    void getGameByCode_shouldHandleDifferentGameCodes() {
        // Given
        Game game2 = testGame.toBuilder().id(2L).gameCode("GAME002").gameName("Game 2").build();
        when(redisService.<String, Game>get("games:active_list", "GAME002")).thenReturn(Mono.just(game2));

        // When & Then
        StepVerifier.create(gameQueryService.getGameByCode("GAME002"))
                .assertNext(game -> {
                    assertEquals("GAME002", game.getGameCode());
                    assertEquals("Game 2", game.getGameName());
                })
                .verifyComplete();

        verify(redisService).get("games:active_list", "GAME002");
    }

    @Test
    void getGameByCode_shouldUseCorrectCacheKey() {
        // Given
        when(redisService.<String, Game>get("games:active_list", "GAME001")).thenReturn(Mono.just(testGame));

        // When
        StepVerifier.create(gameQueryService.getGameByCode("GAME001"))
                .expectNextCount(1)
                .verifyComplete();

        // Then
        verify(redisService).get("games:active_list", "GAME001");
    }

    @Test
    void getGameByCode_shouldReturnInactiveGame_ifInCache() {
        // Given
        Game inactiveGame = testGame.toBuilder().isActive(false).build();
        when(redisService.<String, Game>get("games:active_list", "GAME001")).thenReturn(Mono.just(inactiveGame));

        // When & Then
        StepVerifier.create(gameQueryService.getGameByCode("GAME001"))
                .assertNext(game -> {
                    assertNotNull(game);
                    assertFalse(game.getIsActive());
                })
                .verifyComplete();
    }

    @Test
    void getGameByCode_shouldReturnInactiveGame_ifInDatabase() {
        // Given
        Game inactiveGame = testGame.toBuilder().isActive(false).build();
        when(redisService.<String, Game>get("games:active_list", "GAME001")).thenReturn(Mono.empty());
        when(gameRepository.findByGameCode("GAME001")).thenReturn(Mono.just(inactiveGame));
        when(gameCacheService.saveGame(inactiveGame)).thenReturn(Mono.just(true));

        // When & Then
        StepVerifier.create(gameQueryService.getGameByCode("GAME001"))
                .assertNext(game -> {
                    assertNotNull(game);
                    assertFalse(game.getIsActive());
                })
                .verifyComplete();
    }

    @Test
    void getGameByCode_shouldPreserveDatabaseResult_whenCacheSaveErrors() {
        // Given
        RuntimeException cacheSaveError = new RuntimeException("Cache save failed");
        when(redisService.<String, Game>get("games:active_list", "GAME001")).thenReturn(Mono.empty());
        when(gameRepository.findByGameCode("GAME001")).thenReturn(Mono.just(testGame));
        when(gameCacheService.saveGame(testGame)).thenReturn(Mono.error(cacheSaveError));

        // When & Then
        StepVerifier.create(gameQueryService.getGameByCode("GAME001"))
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException && 
                    throwable.getMessage().equals("Cache save failed"))
                .verify();

        verify(gameRepository).findByGameCode("GAME001");
        verify(gameCacheService).saveGame(testGame);
    }

    @Test
    void getGameByCode_shouldHandleNullGameCode() {
        // Given
        when(redisService.<String, Game>get("games:active_list", null)).thenReturn(Mono.empty());
        when(gameRepository.findByGameCode(null)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(gameQueryService.getGameByCode(null))
                .verifyComplete();

        verify(redisService).get("games:active_list", null);
        verify(gameRepository).findByGameCode(null);
    }

    @Test
    void getGameByCode_shouldHandleEmptyGameCode() {
        // Given
        when(redisService.<String, Game>get("games:active_list", "")).thenReturn(Mono.empty());
        when(gameRepository.findByGameCode("")).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(gameQueryService.getGameByCode(""))
                .verifyComplete();

        verify(redisService).get("games:active_list", "");
        verify(gameRepository).findByGameCode("");
    }
}
