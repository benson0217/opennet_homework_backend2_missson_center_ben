package com.example.demo.shared.infrastructure.repository.r2dbc.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.FetchSpec;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserR2DbcR2dbcRepositoryImplTest {

    @Mock
    private DatabaseClient databaseClient;

    @Mock
    private DatabaseClient.GenericExecuteSpec genericExecuteSpec;

    @Mock
    private FetchSpec<Long> fetchSpec;

    @InjectMocks
    private UserR2DbcR2dbcRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        lenient().when(databaseClient.sql(anyString())).thenReturn(genericExecuteSpec);
        lenient().when(genericExecuteSpec.bind(anyString(), any())).thenReturn(genericExecuteSpec);
        lenient().when(genericExecuteSpec.fetch()).thenReturn((FetchSpec) fetchSpec);
    }

    @Test
    void addPoints_shouldReturnOne_whenUserExistsAndUpdateSucceeds() {
        // Given
        Long userId = 1L;
        int pointsToAdd = 50;
        when(fetchSpec.rowsUpdated()).thenReturn(Mono.just(1L));

        // When & Then
        StepVerifier.create(repository.addPoints(userId, pointsToAdd))
                .assertNext(rowsUpdated -> {
                    assertNotNull(rowsUpdated);
                    assertEquals(1L, rowsUpdated);
                })
                .verifyComplete();

        verify(databaseClient).sql("UPDATE users SET points = points + :pointsToAdd, updated_at = NOW() WHERE id = :userId");
        verify(genericExecuteSpec).bind("userId", userId);
        verify(genericExecuteSpec).bind("pointsToAdd", pointsToAdd);
    }

    @Test
    void addPoints_shouldReturnZero_whenUserDoesNotExist() {
        // Given
        Long userId = 999L;
        int pointsToAdd = 50;
        when(fetchSpec.rowsUpdated()).thenReturn(Mono.just(0L));

        // When & Then
        StepVerifier.create(repository.addPoints(userId, pointsToAdd))
                .assertNext(rowsUpdated -> {
                    assertNotNull(rowsUpdated);
                    assertEquals(0L, rowsUpdated);
                })
                .verifyComplete();

        verify(databaseClient).sql(anyString());
        verify(genericExecuteSpec).bind("userId", userId);
        verify(genericExecuteSpec).bind("pointsToAdd", pointsToAdd);
    }

    @Test
    void addPoints_shouldHandlePositivePoints() {
        // Given
        Long userId = 1L;
        int pointsToAdd = 100;
        when(fetchSpec.rowsUpdated()).thenReturn(Mono.just(1L));

        // When & Then
        StepVerifier.create(repository.addPoints(userId, pointsToAdd))
                .assertNext(rowsUpdated -> assertEquals(1L, rowsUpdated))
                .verifyComplete();

        verify(genericExecuteSpec).bind("pointsToAdd", 100);
    }

    @Test
    void addPoints_shouldHandleZeroPoints() {
        // Given
        Long userId = 1L;
        int pointsToAdd = 0;
        when(fetchSpec.rowsUpdated()).thenReturn(Mono.just(1L));

        // When & Then
        StepVerifier.create(repository.addPoints(userId, pointsToAdd))
                .assertNext(rowsUpdated -> assertEquals(1L, rowsUpdated))
                .verifyComplete();

        verify(genericExecuteSpec).bind("pointsToAdd", 0);
    }

    @Test
    void addPoints_shouldHandleNegativePoints() {
        // Given
        Long userId = 1L;
        int pointsToAdd = -50;
        when(fetchSpec.rowsUpdated()).thenReturn(Mono.just(1L));

        // When & Then
        StepVerifier.create(repository.addPoints(userId, pointsToAdd))
                .assertNext(rowsUpdated -> assertEquals(1L, rowsUpdated))
                .verifyComplete();

        verify(genericExecuteSpec).bind("pointsToAdd", -50);
    }

    @Test
    void addPoints_shouldHandleLargePointValue() {
        // Given
        Long userId = 1L;
        int pointsToAdd = 999999;
        when(fetchSpec.rowsUpdated()).thenReturn(Mono.just(1L));

        // When & Then
        StepVerifier.create(repository.addPoints(userId, pointsToAdd))
                .assertNext(rowsUpdated -> assertEquals(1L, rowsUpdated))
                .verifyComplete();

        verify(genericExecuteSpec).bind("pointsToAdd", 999999);
    }

    @Test
    void addPoints_shouldPropagateError_whenDatabaseClientFails() {
        // Given
        Long userId = 1L;
        int pointsToAdd = 50;
        RuntimeException databaseError = new RuntimeException("Database connection failed");
        when(fetchSpec.rowsUpdated()).thenReturn(Mono.error(databaseError));

        // When & Then
        StepVerifier.create(repository.addPoints(userId, pointsToAdd))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("Database connection failed"))
                .verify();

        verify(databaseClient).sql(anyString());
        verify(genericExecuteSpec).bind("userId", userId);
        verify(genericExecuteSpec).bind("pointsToAdd", pointsToAdd);
    }

    @Test
    void addPoints_shouldUseCorrectSqlQuery() {
        // Given
        Long userId = 1L;
        int pointsToAdd = 50;
        when(fetchSpec.rowsUpdated()).thenReturn(Mono.just(1L));

        // When
        StepVerifier.create(repository.addPoints(userId, pointsToAdd))
                .expectNextCount(1)
                .verifyComplete();

        // Then
        verify(databaseClient).sql("UPDATE users SET points = points + :pointsToAdd, updated_at = NOW() WHERE id = :userId");
    }

    @Test
    void addPoints_shouldBindBothParameters() {
        // Given
        Long userId = 42L;
        int pointsToAdd = 123;
        when(fetchSpec.rowsUpdated()).thenReturn(Mono.just(1L));

        // When
        StepVerifier.create(repository.addPoints(userId, pointsToAdd))
                .expectNextCount(1)
                .verifyComplete();

        // Then
        verify(genericExecuteSpec).bind("userId", 42L);
        verify(genericExecuteSpec).bind("pointsToAdd", 123);
    }

    @Test
    void addPoints_shouldHandleDifferentUserIds() {
        // Given
        Long userId = 5L;
        int pointsToAdd = 10;
        when(fetchSpec.rowsUpdated()).thenReturn(Mono.just(1L));

        // When & Then
        StepVerifier.create(repository.addPoints(userId, pointsToAdd))
                .assertNext(rowsUpdated -> assertEquals(1L, rowsUpdated))
                .verifyComplete();

        verify(genericExecuteSpec).bind("userId", 5L);
    }

    @Test
    void addPoints_shouldReturnRowsUpdatedDirectly() {
        // Given
        Long userId = 1L;
        int pointsToAdd = 25;
        when(fetchSpec.rowsUpdated()).thenReturn(Mono.just(1L));

        // When & Then
        StepVerifier.create(repository.addPoints(userId, pointsToAdd))
                .assertNext(rowsUpdated -> {
                    assertNotNull(rowsUpdated);
                    assertInstanceOf(Long.class, rowsUpdated);
                })
                .verifyComplete();
    }

    @Test
    void addPoints_shouldHandleSmallPointValue() {
        // Given
        Long userId = 1L;
        int pointsToAdd = 1;
        when(fetchSpec.rowsUpdated()).thenReturn(Mono.just(1L));

        // When & Then
        StepVerifier.create(repository.addPoints(userId, pointsToAdd))
                .assertNext(rowsUpdated -> assertEquals(1L, rowsUpdated))
                .verifyComplete();

        verify(genericExecuteSpec).bind("pointsToAdd", 1);
    }

    @Test
    void addPoints_shouldHandleEmptyResult() {
        // Given
        Long userId = 1L;
        int pointsToAdd = 50;
        when(fetchSpec.rowsUpdated()).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(repository.addPoints(userId, pointsToAdd))
                .verifyComplete();

        verify(databaseClient).sql(anyString());
        verify(genericExecuteSpec).bind("userId", userId);
        verify(genericExecuteSpec).bind("pointsToAdd", pointsToAdd);
    }

    @Test
    void addPoints_shouldHandleVeryLargeUserId() {
        // Given
        Long userId = Long.MAX_VALUE;
        int pointsToAdd = 50;
        when(fetchSpec.rowsUpdated()).thenReturn(Mono.just(1L));

        // When & Then
        StepVerifier.create(repository.addPoints(userId, pointsToAdd))
                .assertNext(rowsUpdated -> assertEquals(1L, rowsUpdated))
                .verifyComplete();

        verify(genericExecuteSpec).bind("userId", Long.MAX_VALUE);
    }

    @Test
    void addPoints_shouldHandleMultipleUpdates() {
        // Given
        Long userId = 1L;
        when(fetchSpec.rowsUpdated()).thenReturn(Mono.just(1L));

        // When & Then - first update
        StepVerifier.create(repository.addPoints(userId, 50))
                .expectNext(1L)
                .verifyComplete();

        // When & Then - second update
        StepVerifier.create(repository.addPoints(userId, 30))
                .expectNext(1L)
                .verifyComplete();

        verify(databaseClient, times(2)).sql(anyString());
    }

}
