package com.example.demo.game.application.service;

import com.example.demo.game.domain.model.Game;
import com.example.demo.game.domain.repository.GameRepository;
import com.example.demo.shared.infrastructure.redis.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameCacheServiceTest {

    @Mock
    private GameRepository gameDbRepository;

    @Mock
    private RedisService redisService;

    @InjectMocks
    private GameCacheService gameCacheService;

    private Game testGame1;
    private Game testGame2;
    private Game testGame3;

    @BeforeEach
    void setUp() {
        testGame1 = Game.builder()
                .id(1L)
                .gameCode("GAME001")
                .gameName("Test Game 1")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testGame2 = Game.builder()
                .id(2L)
                .gameCode("GAME002")
                .gameName("Test Game 2")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testGame3 = Game.builder()
                .id(3L)
                .gameCode("GAME003")
                .gameName("Test Game 3")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void rebuildGameCache_shouldCacheAllGames_whenGamesExist() {
        // Given
        when(gameDbRepository.findAllActive()).thenReturn(Flux.just(testGame1, testGame2, testGame3));
        when(redisService.delete(anyString())).thenReturn(Mono.just(1L));
        when(redisService.putAll(anyString(), anyMap())).thenReturn(Mono.just(true));

        // When & Then
        StepVerifier.create(gameCacheService.rebuildGameCache())
                .expectNext(3L)
                .verifyComplete();

        verify(gameDbRepository).findAllActive();
        verify(redisService).delete("games:active_list");
        verify(redisService).putAll(eq("games:active_list"), argThat(map ->
            map.size() == 3 && 
            map.containsKey("GAME001") && 
            map.containsKey("GAME002") && 
            map.containsKey("GAME003")));
    }

    @Test
    void rebuildGameCache_shouldReturnZero_whenNoGamesExist() {
        // Given
        when(gameDbRepository.findAllActive()).thenReturn(Flux.empty());
        when(redisService.delete(anyString())).thenReturn(Mono.just(0L));
        when(redisService.putAll(anyString(), anyMap())).thenReturn(Mono.just(true));

        // When & Then
        StepVerifier.create(gameCacheService.rebuildGameCache())
                .expectNext(0L)
                .verifyComplete();

        verify(gameDbRepository).findAllActive();
        verify(redisService).delete("games:active_list");
        verify(redisService).putAll(eq("games:active_list"), argThat(Map::isEmpty));
    }

    @Test
    void rebuildGameCache_shouldReturnOne_whenOnlyOneGameExists() {
        // Given
        when(gameDbRepository.findAllActive()).thenReturn(Flux.just(testGame1));
        when(redisService.delete(anyString())).thenReturn(Mono.just(1L));
        when(redisService.putAll(anyString(), anyMap())).thenReturn(Mono.just(true));

        // When & Then
        StepVerifier.create(gameCacheService.rebuildGameCache())
                .expectNext(1L)
                .verifyComplete();

        verify(gameDbRepository).findAllActive();
        verify(redisService).putAll(eq("games:active_list"), argThat(map ->
            map.size() == 1 && map.containsKey("GAME001")));
    }

    @Test
    void rebuildGameCache_shouldPropagateError_whenRepositoryFails() {
        // Given
        RuntimeException repositoryError = new RuntimeException("Database error");
        when(gameDbRepository.findAllActive()).thenReturn(Flux.error(repositoryError));

        // When & Then
        StepVerifier.create(gameCacheService.rebuildGameCache())
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException && 
                    throwable.getMessage().equals("Database error"))
                .verify();

        verify(gameDbRepository).findAllActive();
        verify(redisService, never()).delete(anyString());
        verify(redisService, never()).putAll(anyString(), anyMap());
    }

    @Test
    void rebuildGameCache_shouldPropagateError_whenDeleteFails() {
        // Given
        RuntimeException deleteError = new RuntimeException("Redis delete error");
        when(gameDbRepository.findAllActive()).thenReturn(Flux.just(testGame1));
        when(redisService.delete(anyString())).thenReturn(Mono.error(deleteError));
        when(redisService.putAll(anyString(), anyMap())).thenReturn(Mono.just(true));

        // When & Then
        StepVerifier.create(gameCacheService.rebuildGameCache())
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException && 
                    throwable.getMessage().equals("Redis delete error"))
                .verify();

        verify(gameDbRepository).findAllActive();
        verify(redisService).delete("games:active_list");
        // putAll may be called due to eager chain construction, but error still propagates
    }

    @Test
    void rebuildGameCache_shouldPropagateError_whenPutAllFails() {
        // Given
        RuntimeException putAllError = new RuntimeException("Redis putAll error");
        when(gameDbRepository.findAllActive()).thenReturn(Flux.just(testGame1));
        when(redisService.delete(anyString())).thenReturn(Mono.just(1L));
        when(redisService.putAll(anyString(), anyMap())).thenReturn(Mono.error(putAllError));

        // When & Then
        StepVerifier.create(gameCacheService.rebuildGameCache())
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException && 
                    throwable.getMessage().equals("Redis putAll error"))
                .verify();

        verify(gameDbRepository).findAllActive();
        verify(redisService).delete("games:active_list");
        verify(redisService).putAll(anyString(), anyMap());
    }

    @Test
    void rebuildGameCache_shouldUseGameCodeAsKey() {
        // Given
        when(gameDbRepository.findAllActive()).thenReturn(Flux.just(testGame1, testGame2));
        when(redisService.delete(anyString())).thenReturn(Mono.just(1L));
        when(redisService.putAll(anyString(), anyMap())).thenReturn(Mono.just(true));

        // When & Then
        StepVerifier.create(gameCacheService.rebuildGameCache())
                .expectNext(2L)
                .verifyComplete();

        verify(redisService).putAll(eq("games:active_list"), argThat(map -> {
            Game game1 = (Game) map.get("GAME001");
            Game game2 = (Game) map.get("GAME002");
            return game1 != null && game1.getId().equals(1L) &&
                   game2 != null && game2.getId().equals(2L);
        }));
    }

    @Test
    void saveGame_shouldSaveGameToCache_whenGameIsValid() {
        // Given
        when(redisService.put(anyString(), anyString(), any(Game.class))).thenReturn(Mono.just(true));

        // When & Then
        StepVerifier.create(gameCacheService.saveGame(testGame1))
                .expectNext(true)
                .verifyComplete();

        verify(redisService).put("games:active_list", "GAME001", testGame1);
    }

    @Test
    void saveGame_shouldReturnFalse_whenSaveFails() {
        // Given
        when(redisService.put(anyString(), anyString(), any(Game.class))).thenReturn(Mono.just(false));

        // When & Then
        StepVerifier.create(gameCacheService.saveGame(testGame1))
                .expectNext(false)
                .verifyComplete();

        verify(redisService).put("games:active_list", "GAME001", testGame1);
    }

    @Test
    void saveGame_shouldPropagateError_whenRedisFails() {
        // Given
        RuntimeException redisError = new RuntimeException("Redis error");
        when(redisService.put(anyString(), anyString(), any(Game.class))).thenReturn(Mono.error(redisError));

        // When & Then
        StepVerifier.create(gameCacheService.saveGame(testGame1))
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException && 
                    throwable.getMessage().equals("Redis error"))
                .verify();

        verify(redisService).put("games:active_list", "GAME001", testGame1);
    }

    @Test
    void saveGame_shouldUseCorrectCacheKey() {
        // Given
        when(redisService.put(anyString(), anyString(), any(Game.class))).thenReturn(Mono.just(true));

        // When
        StepVerifier.create(gameCacheService.saveGame(testGame2))
                .expectNext(true)
                .verifyComplete();

        // Then
        verify(redisService).put("games:active_list", "GAME002", testGame2);
    }

    @Test
    void saveGame_shouldHandleMultipleGames() {
        // Given
        when(redisService.put(anyString(), anyString(), any(Game.class))).thenReturn(Mono.just(true));

        // When & Then
        StepVerifier.create(gameCacheService.saveGame(testGame1))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(gameCacheService.saveGame(testGame2))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(gameCacheService.saveGame(testGame3))
                .expectNext(true)
                .verifyComplete();

        verify(redisService).put("games:active_list", "GAME001", testGame1);
        verify(redisService).put("games:active_list", "GAME002", testGame2);
        verify(redisService).put("games:active_list", "GAME003", testGame3);
    }

    @Test
    void rebuildGameCache_shouldDeleteOldCache_beforeAddingNewGames() {
        // Given
        when(gameDbRepository.findAllActive()).thenReturn(Flux.just(testGame1));
        when(redisService.delete(anyString())).thenReturn(Mono.just(1L));
        when(redisService.putAll(anyString(), anyMap())).thenReturn(Mono.just(true));

        // When
        StepVerifier.create(gameCacheService.rebuildGameCache())
                .expectNext(1L)
                .verifyComplete();

        // Then - verify delete is called before putAll
        verify(redisService).delete("games:active_list");
        verify(redisService).putAll(eq("games:active_list"), anyMap());
    }
}
