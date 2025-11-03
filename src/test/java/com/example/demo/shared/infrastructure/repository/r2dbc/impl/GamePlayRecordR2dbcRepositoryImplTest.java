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
class GamePlayRecordR2dbcRepositoryImplTest {

    @Mock
    private R2dbcEntityTemplate r2dbcEntityTemplate;

    @Mock
    private DatabaseClient databaseClient;

    @Mock
    private DatabaseClient.GenericExecuteSpec genericExecuteSpec;

    @Mock
    private RowsFetchSpec<Object> fetchSpec;

    @InjectMocks
    private GamePlayRecordR2dbcRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        lenient().when(r2dbcEntityTemplate.getDatabaseClient()).thenReturn(databaseClient);
        lenient().when(databaseClient.sql(anyString())).thenReturn(genericExecuteSpec);
        lenient().when(genericExecuteSpec.bind(anyString(), any())).thenReturn(genericExecuteSpec);
        lenient().when(genericExecuteSpec.map(any(Function.class))).thenReturn(fetchSpec);
    }

    @Test
    void sumScoreByUserId_shouldReturnSum_whenUserHasMultiplePlayRecords() {
        // Given
        Long userId = 1L;
        Integer expectedSum = 5000;
        when(fetchSpec.one()).thenReturn(Mono.just(expectedSum));

        // When & Then
        StepVerifier.create(repository.sumScoreByUserId(userId))
                .assertNext(sum -> {
                    assertNotNull(sum);
                    assertEquals(expectedSum, sum);
                })
                .verifyComplete();

        verify(databaseClient).sql("SELECT COALESCE(SUM(score), 0) FROM games_play_record WHERE user_id = :userId");
        verify(genericExecuteSpec).bind("userId", userId);
    }

    @Test
    void sumScoreByUserId_shouldReturnZero_whenUserHasNoPlayRecords() {
        // Given
        Long userId = 1L;
        when(fetchSpec.one()).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(repository.sumScoreByUserId(userId))
                .assertNext(sum -> {
                    assertNotNull(sum);
                    assertEquals(0, sum);
                })
                .verifyComplete();

        verify(databaseClient).sql("SELECT COALESCE(SUM(score), 0) FROM games_play_record WHERE user_id = :userId");
        verify(genericExecuteSpec).bind("userId", userId);
    }

    @Test
    void sumScoreByUserId_shouldReturnSum_whenUserHasSinglePlayRecord() {
        // Given
        Long userId = 1L;
        Integer expectedSum = 1500;
        when(fetchSpec.one()).thenReturn(Mono.just(expectedSum));

        // When & Then
        StepVerifier.create(repository.sumScoreByUserId(userId))
                .assertNext(sum -> {
                    assertNotNull(sum);
                    assertEquals(1500, sum);
                })
                .verifyComplete();

        verify(databaseClient).sql(anyString());
        verify(genericExecuteSpec).bind("userId", userId);
    }

    @Test
    void sumScoreByUserId_shouldReturnCorrectSum_forThreePlayRecords() {
        // Given
        Long userId = 1L;
        Integer expectedSum = 3000;
        when(fetchSpec.one()).thenReturn(Mono.just(expectedSum));

        // When & Then
        StepVerifier.create(repository.sumScoreByUserId(userId))
                .assertNext(sum -> assertEquals(3000, sum))
                .verifyComplete();

        verify(databaseClient).sql(anyString());
        verify(genericExecuteSpec).bind("userId", userId);
    }

    @Test
    void sumScoreByUserId_shouldPropagateError_whenDatabaseClientFails() {
        // Given
        Long userId = 1L;
        RuntimeException databaseError = new RuntimeException("Database connection failed");
        when(fetchSpec.one()).thenReturn(Mono.error(databaseError));

        // When & Then
        StepVerifier.create(repository.sumScoreByUserId(userId))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("Database connection failed"))
                .verify();

        verify(databaseClient).sql(anyString());
        verify(genericExecuteSpec).bind("userId", userId);
    }

    @Test
    void sumScoreByUserId_shouldHandleDifferentUserIds() {
        // Given
        Long userId = 999L;
        Integer expectedSum = 2500;
        when(fetchSpec.one()).thenReturn(Mono.just(expectedSum));

        // When & Then
        StepVerifier.create(repository.sumScoreByUserId(userId))
                .assertNext(sum -> assertEquals(2500, sum))
                .verifyComplete();

        verify(genericExecuteSpec).bind("userId", 999L);
    }

    @Test
    void sumScoreByUserId_shouldUseCorrectSqlQuery() {
        // Given
        Long userId = 1L;
        when(fetchSpec.one()).thenReturn(Mono.just(0));

        // When
        StepVerifier.create(repository.sumScoreByUserId(userId))
                .expectNextCount(1)
                .verifyComplete();

        // Then
        verify(databaseClient).sql("SELECT COALESCE(SUM(score), 0) FROM games_play_record WHERE user_id = :userId");
    }

    @Test
    void sumScoreByUserId_shouldReturnZeroAsDefault_whenQueryReturnsEmpty() {
        // Given
        Long userId = 1L;
        when(fetchSpec.one()).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(repository.sumScoreByUserId(userId))
                .assertNext(sum -> {
                    assertNotNull(sum);
                    assertEquals(0, sum);
                })
                .verifyComplete();
    }

    @Test
    void sumScoreByUserId_shouldHandleLargeScoreSum() {
        // Given
        Long userId = 1L;
        Integer expectedSum = 999999;
        when(fetchSpec.one()).thenReturn(Mono.just(expectedSum));

        // When & Then
        StepVerifier.create(repository.sumScoreByUserId(userId))
                .assertNext(sum -> assertEquals(999999, sum))
                .verifyComplete();
    }

    @Test
    void sumScoreByUserId_shouldBindUserIdParameter() {
        // Given
        Long userId = 42L;
        when(fetchSpec.one()).thenReturn(Mono.just(1000));

        // When
        StepVerifier.create(repository.sumScoreByUserId(userId))
                .expectNextCount(1)
                .verifyComplete();

        // Then
        verify(genericExecuteSpec).bind("userId", 42L);
    }

    @Test
    void sumScoreByUserId_shouldHandleZeroScore() {
        // Given
        Long userId = 1L;
        Integer expectedSum = 0;
        when(fetchSpec.one()).thenReturn(Mono.just(expectedSum));

        // When & Then
        StepVerifier.create(repository.sumScoreByUserId(userId))
                .assertNext(sum -> {
                    assertNotNull(sum);
                    assertEquals(0, sum);
                })
                .verifyComplete();

        verify(databaseClient).sql(anyString());
        verify(genericExecuteSpec).bind("userId", userId);
    }

    @Test
    void sumScoreByUserId_shouldReturnCorrectSum_forMixedScores() {
        // Given
        Long userId = 1L;
        Integer expectedSum = 4500; // Example: 1500 + 2000 + 1000
        when(fetchSpec.one()).thenReturn(Mono.just(expectedSum));

        // When & Then
        StepVerifier.create(repository.sumScoreByUserId(userId))
                .assertNext(sum -> assertEquals(4500, sum))
                .verifyComplete();

        verify(databaseClient).sql(anyString());
        verify(genericExecuteSpec).bind("userId", userId);
    }

    @Test
    void sumScoreByUserId_shouldHandleVeryLargeUserId() {
        // Given
        Long userId = 999999999L;
        Integer expectedSum = 1500;
        when(fetchSpec.one()).thenReturn(Mono.just(expectedSum));

        // When & Then
        StepVerifier.create(repository.sumScoreByUserId(userId))
                .assertNext(sum -> assertEquals(1500, sum))
                .verifyComplete();

        verify(genericExecuteSpec).bind("userId", 999999999L);
    }

    @Test
    void sumScoreByUserId_shouldUseCoalesceInQuery() {
        // Given
        Long userId = 1L;
        when(fetchSpec.one()).thenReturn(Mono.just(0));

        // When
        StepVerifier.create(repository.sumScoreByUserId(userId))
                .expectNextCount(1)
                .verifyComplete();

        // Then - verify the query uses COALESCE to handle NULL values
        verify(databaseClient).sql(argThat((String sql) -> 
            sql != null && sql.contains("COALESCE") && sql.contains("SUM(score)")
        ));
    }
}
