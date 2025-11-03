package com.example.demo.shared.infrastructure.repository.r2dbc.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.RowsFetchSpec;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameLaunchRecordR2dbcRepositoryImplTest {

    @Mock
    private R2dbcEntityTemplate r2dbcEntityTemplate;

    @Mock
    private DatabaseClient databaseClient;

    @Mock
    private DatabaseClient.GenericExecuteSpec genericExecuteSpec;

    @Mock
    private RowsFetchSpec<Object> fetchSpec;

    @InjectMocks
    private GameLaunchRecordR2dbcRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        lenient().when(r2dbcEntityTemplate.getDatabaseClient()).thenReturn(databaseClient);
        lenient().when(databaseClient.sql(anyString())).thenReturn(genericExecuteSpec);
        lenient().when(genericExecuteSpec.bind(anyString(), any())).thenReturn(genericExecuteSpec);
        lenient().when(genericExecuteSpec.map(any(Function.class))).thenReturn(fetchSpec);
    }

    @Test
    void countDistinctGamesLaunchedByUser_shouldReturnCount_whenUserHasLaunchedMultipleGames() {
        // Given
        Long userId = 1L;
        Long expectedCount = 5L;
        when(fetchSpec.one()).thenReturn(Mono.just(expectedCount));

        // When & Then
        StepVerifier.create(repository.countDistinctGamesLaunchedByUser(userId))
                .assertNext(count -> {
                    assertNotNull(count);
                    assertEquals(expectedCount, count);
                })
                .verifyComplete();

        verify(databaseClient).sql("SELECT COUNT(DISTINCT game_id) FROM game_launch_record WHERE user_id = :userId");
        verify(genericExecuteSpec).bind("userId", userId);
    }

    @Test
    void countDistinctGamesLaunchedByUser_shouldReturnZero_whenUserHasNotLaunchedAnyGames() {
        // Given
        Long userId = 1L;
        when(fetchSpec.one()).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(repository.countDistinctGamesLaunchedByUser(userId))
                .assertNext(count -> {
                    assertNotNull(count);
                    assertEquals(0L, count);
                })
                .verifyComplete();

        verify(databaseClient).sql("SELECT COUNT(DISTINCT game_id) FROM game_launch_record WHERE user_id = :userId");
        verify(genericExecuteSpec).bind("userId", userId);
    }

    @Test
    void countDistinctGamesLaunchedByUser_shouldReturnOne_whenUserHasLaunchedOneGame() {
        // Given
        Long userId = 1L;
        Long expectedCount = 1L;
        when(fetchSpec.one()).thenReturn(Mono.just(expectedCount));

        // When & Then
        StepVerifier.create(repository.countDistinctGamesLaunchedByUser(userId))
                .assertNext(count -> {
                    assertNotNull(count);
                    assertEquals(1L, count);
                })
                .verifyComplete();

        verify(databaseClient).sql(anyString());
        verify(genericExecuteSpec).bind("userId", userId);
    }

    @Test
    void countDistinctGamesLaunchedByUser_shouldReturnThree_whenUserHasLaunchedThreeDistinctGames() {
        // Given
        Long userId = 1L;
        Long expectedCount = 3L;
        when(fetchSpec.one()).thenReturn(Mono.just(expectedCount));

        // When & Then
        StepVerifier.create(repository.countDistinctGamesLaunchedByUser(userId))
                .assertNext(count -> assertEquals(3L, count))
                .verifyComplete();

        verify(databaseClient).sql(anyString());
        verify(genericExecuteSpec).bind("userId", userId);
    }

    @Test
    void countDistinctGamesLaunchedByUser_shouldPropagateError_whenDatabaseClientFails() {
        // Given
        Long userId = 1L;
        RuntimeException databaseError = new RuntimeException("Database connection failed");
        when(fetchSpec.one()).thenReturn(Mono.error(databaseError));

        // When & Then
        StepVerifier.create(repository.countDistinctGamesLaunchedByUser(userId))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("Database connection failed"))
                .verify();

        verify(databaseClient).sql(anyString());
        verify(genericExecuteSpec).bind("userId", userId);
    }

    @Test
    void countDistinctGamesLaunchedByUser_shouldHandleDifferentUserIds() {
        // Given
        Long userId = 999L;
        Long expectedCount = 2L;
        when(fetchSpec.one()).thenReturn(Mono.just(expectedCount));

        // When & Then
        StepVerifier.create(repository.countDistinctGamesLaunchedByUser(userId))
                .assertNext(count -> assertEquals(2L, count))
                .verifyComplete();

        verify(genericExecuteSpec).bind("userId", 999L);
    }

    @Test
    void countDistinctGamesLaunchedByUser_shouldUseCorrectSqlQuery() {
        // Given
        Long userId = 1L;
        when(fetchSpec.one()).thenReturn(Mono.just(0L));

        // When
        StepVerifier.create(repository.countDistinctGamesLaunchedByUser(userId))
                .expectNextCount(1)
                .verifyComplete();

        // Then
        verify(databaseClient).sql("SELECT COUNT(DISTINCT game_id) FROM game_launch_record WHERE user_id = :userId");
    }

    @Test
    void countDistinctGamesLaunchedByUser_shouldReturnZeroAsDefault_whenQueryReturnsEmpty() {
        // Given
        Long userId = 1L;
        when(fetchSpec.one()).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(repository.countDistinctGamesLaunchedByUser(userId))
                .assertNext(count -> {
                    assertNotNull(count);
                    assertEquals(0L, count);
                })
                .verifyComplete();
    }

    @Test
    void countDistinctGamesLaunchedByUser_shouldHandleLargeCount() {
        // Given
        Long userId = 1L;
        Long expectedCount = 100L;
        when(fetchSpec.one()).thenReturn(Mono.just(expectedCount));

        // When & Then
        StepVerifier.create(repository.countDistinctGamesLaunchedByUser(userId))
                .assertNext(count -> assertEquals(100L, count))
                .verifyComplete();
    }

    @Test
    void countDistinctGamesLaunchedByUser_shouldBindUserIdParameter() {
        // Given
        Long userId = 42L;
        when(fetchSpec.one()).thenReturn(Mono.just(5L));

        // When
        StepVerifier.create(repository.countDistinctGamesLaunchedByUser(userId))
                .expectNextCount(1)
                .verifyComplete();

        // Then
        verify(genericExecuteSpec).bind("userId", 42L);
    }
}
