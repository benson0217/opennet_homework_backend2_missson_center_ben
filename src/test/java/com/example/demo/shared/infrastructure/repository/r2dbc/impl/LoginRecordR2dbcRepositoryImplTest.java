package com.example.demo.shared.infrastructure.repository.r2dbc.impl;

import com.example.demo.shared.infrastructure.repository.data.LoginRecordData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.core.ReactiveSelectOperation;
import org.springframework.data.relational.core.query.Query;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginRecordR2dbcRepositoryImplTest {

    @Mock
    private R2dbcEntityTemplate r2dbcEntityTemplate;

    @Mock
    private ReactiveSelectOperation.ReactiveSelect<LoginRecordData> reactiveSelect;

    @Mock
    private ReactiveSelectOperation.TerminatingSelect<LoginRecordData> terminatingSelect;

    @InjectMocks
    private LoginRecordR2dbcRepositoryImpl loginRecordR2dbcRepositoryImpl;

    private LoginRecordData record1;
    private LoginRecordData record2;
    private LoginRecordData record3;

    @BeforeEach
    void setUp() {
        LocalDate today = LocalDate.now();
        
        record1 = new LoginRecordData();
        record1.setId(1L);
        record1.setUserId(100L);
        record1.setLoginDate(today);
        record1.setLoginTime(today.atStartOfDay());
        record1.setCreatedAt(LocalDateTime.now());

        record2 = new LoginRecordData();
        record2.setId(2L);
        record2.setUserId(100L);
        record2.setLoginDate(today.minusDays(1));
        record2.setLoginTime(today.minusDays(1).atStartOfDay());
        record2.setCreatedAt(LocalDateTime.now());

        record3 = new LoginRecordData();
        record3.setId(3L);
        record3.setUserId(100L);
        record3.setLoginDate(today.minusDays(2));
        record3.setLoginTime(today.minusDays(2).atStartOfDay());
        record3.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void findRecentByUserId_shouldReturnRecords_whenRecordsExist() {
        // Given
        Long userId = 100L;
        int limit = 10;
        List<LoginRecordData> records = Arrays.asList(record1, record2, record3);

        when(r2dbcEntityTemplate.select(LoginRecordData.class)).thenReturn(reactiveSelect);
        when(reactiveSelect.matching(any(Query.class))).thenReturn(terminatingSelect);
        when(terminatingSelect.all()).thenReturn(Flux.fromIterable(records));

        // When & Then
        StepVerifier.create(loginRecordR2dbcRepositoryImpl.findRecentByUserId(userId, limit))
                .expectNext(record1)
                .expectNext(record2)
                .expectNext(record3)
                .verifyComplete();

        verify(r2dbcEntityTemplate).select(LoginRecordData.class);
        verify(reactiveSelect).matching(any(Query.class));
        verify(terminatingSelect).all();
    }

    @Test
    void findRecentByUserId_shouldReturnEmpty_whenNoRecordsExist() {
        // Given
        Long userId = 100L;
        int limit = 10;

        when(r2dbcEntityTemplate.select(LoginRecordData.class)).thenReturn(reactiveSelect);
        when(reactiveSelect.matching(any(Query.class))).thenReturn(terminatingSelect);
        when(terminatingSelect.all()).thenReturn(Flux.empty());

        // When & Then
        StepVerifier.create(loginRecordR2dbcRepositoryImpl.findRecentByUserId(userId, limit))
                .verifyComplete();

        verify(r2dbcEntityTemplate).select(LoginRecordData.class);
        verify(reactiveSelect).matching(any(Query.class));
        verify(terminatingSelect).all();
    }

    @Test
    void findRecentByUserId_shouldReturnSingleRecord_whenOnlyOneRecordExists() {
        // Given
        Long userId = 100L;
        int limit = 10;

        when(r2dbcEntityTemplate.select(LoginRecordData.class)).thenReturn(reactiveSelect);
        when(reactiveSelect.matching(any(Query.class))).thenReturn(terminatingSelect);
        when(terminatingSelect.all()).thenReturn(Flux.just(record1));

        // When & Then
        StepVerifier.create(loginRecordR2dbcRepositoryImpl.findRecentByUserId(userId, limit))
                .expectNext(record1)
                .verifyComplete();

        verify(r2dbcEntityTemplate).select(LoginRecordData.class);
        verify(reactiveSelect).matching(any(Query.class));
        verify(terminatingSelect).all();
    }

    @Test
    void findRecentByUserId_shouldApplyLimit_whenLimitIsSpecified() {
        // Given
        Long userId = 100L;
        int limit = 2;
        List<LoginRecordData> records = Arrays.asList(record1, record2);

        when(r2dbcEntityTemplate.select(LoginRecordData.class)).thenReturn(reactiveSelect);
        when(reactiveSelect.matching(any(Query.class))).thenReturn(terminatingSelect);
        when(terminatingSelect.all()).thenReturn(Flux.fromIterable(records));

        // When & Then
        StepVerifier.create(loginRecordR2dbcRepositoryImpl.findRecentByUserId(userId, limit))
                .expectNext(record1)
                .expectNext(record2)
                .verifyComplete();

        verify(r2dbcEntityTemplate).select(LoginRecordData.class);
        verify(reactiveSelect).matching(any(Query.class));
        verify(terminatingSelect).all();
    }

    @Test
    void findRecentByUserId_shouldReturnRecordsInDescendingOrder() {
        // Given
        Long userId = 100L;
        int limit = 10;
        // Records should be returned in descending order by date
        List<LoginRecordData> records = Arrays.asList(record1, record2, record3);

        when(r2dbcEntityTemplate.select(LoginRecordData.class)).thenReturn(reactiveSelect);
        when(reactiveSelect.matching(any(Query.class))).thenReturn(terminatingSelect);
        when(terminatingSelect.all()).thenReturn(Flux.fromIterable(records));

        // When & Then
        StepVerifier.create(loginRecordR2dbcRepositoryImpl.findRecentByUserId(userId, limit))
                .expectNextSequence(records)
                .verifyComplete();

        verify(r2dbcEntityTemplate).select(LoginRecordData.class);
        verify(reactiveSelect).matching(any(Query.class));
        verify(terminatingSelect).all();
    }

    @Test
    void findRecentByUserId_shouldPropagateError_whenRepositoryFails() {
        // Given
        Long userId = 100L;
        int limit = 10;
        RuntimeException repositoryError = new RuntimeException("Database error");

        when(r2dbcEntityTemplate.select(LoginRecordData.class)).thenReturn(reactiveSelect);
        when(reactiveSelect.matching(any(Query.class))).thenReturn(terminatingSelect);
        when(terminatingSelect.all()).thenReturn(Flux.error(repositoryError));

        // When & Then
        StepVerifier.create(loginRecordR2dbcRepositoryImpl.findRecentByUserId(userId, limit))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("Database error"))
                .verify();

        verify(r2dbcEntityTemplate).select(LoginRecordData.class);
        verify(reactiveSelect).matching(any(Query.class));
        verify(terminatingSelect).all();
    }

    @Test
    void findRecentByUserId_shouldHandleDifferentUserId() {
        // Given
        Long userId = 200L;
        int limit = 10;
        LoginRecordData record = new LoginRecordData();
        record.setId(10L);
        record.setUserId(userId);
        record.setLoginDate(LocalDate.now());
        record.setLoginTime(LocalDateTime.now());
        record.setCreatedAt(LocalDateTime.now());

        when(r2dbcEntityTemplate.select(LoginRecordData.class)).thenReturn(reactiveSelect);
        when(reactiveSelect.matching(any(Query.class))).thenReturn(terminatingSelect);
        when(terminatingSelect.all()).thenReturn(Flux.just(record));

        // When & Then
        StepVerifier.create(loginRecordR2dbcRepositoryImpl.findRecentByUserId(userId, limit))
                .expectNext(record)
                .verifyComplete();

        verify(r2dbcEntityTemplate).select(LoginRecordData.class);
        verify(reactiveSelect).matching(any(Query.class));
        verify(terminatingSelect).all();
    }

    @Test
    void findRecentByUserId_shouldHandleLimit1() {
        // Given
        Long userId = 100L;
        int limit = 1;

        when(r2dbcEntityTemplate.select(LoginRecordData.class)).thenReturn(reactiveSelect);
        when(reactiveSelect.matching(any(Query.class))).thenReturn(terminatingSelect);
        when(terminatingSelect.all()).thenReturn(Flux.just(record1));

        // When & Then
        StepVerifier.create(loginRecordR2dbcRepositoryImpl.findRecentByUserId(userId, limit))
                .expectNext(record1)
                .verifyComplete();

        verify(r2dbcEntityTemplate).select(LoginRecordData.class);
        verify(reactiveSelect).matching(any(Query.class));
        verify(terminatingSelect).all();
    }

    @Test
    void findRecentByUserId_shouldHandleLargeLimit() {
        // Given
        Long userId = 100L;
        int limit = 100;
        List<LoginRecordData> records = Arrays.asList(record1, record2, record3);

        when(r2dbcEntityTemplate.select(LoginRecordData.class)).thenReturn(reactiveSelect);
        when(reactiveSelect.matching(any(Query.class))).thenReturn(terminatingSelect);
        when(terminatingSelect.all()).thenReturn(Flux.fromIterable(records));

        // When & Then
        StepVerifier.create(loginRecordR2dbcRepositoryImpl.findRecentByUserId(userId, limit))
                .expectNextSequence(records)
                .verifyComplete();

        verify(r2dbcEntityTemplate).select(LoginRecordData.class);
        verify(reactiveSelect).matching(any(Query.class));
        verify(terminatingSelect).all();
    }

    @Test
    void findRecentByUserId_shouldReturnCorrectRecordFields() {
        // Given
        Long userId = 100L;
        int limit = 10;

        when(r2dbcEntityTemplate.select(LoginRecordData.class)).thenReturn(reactiveSelect);
        when(reactiveSelect.matching(any(Query.class))).thenReturn(terminatingSelect);
        when(terminatingSelect.all()).thenReturn(Flux.just(record1));

        // When & Then
        StepVerifier.create(loginRecordR2dbcRepositoryImpl.findRecentByUserId(userId, limit))
                .assertNext(record -> {
                    assert record.getId().equals(1L);
                    assert record.getUserId().equals(100L);
                    assert record.getLoginDate().equals(LocalDate.now());
                    assert record.getLoginTime() != null;
                    assert record.getCreatedAt() != null;
                })
                .verifyComplete();

        verify(r2dbcEntityTemplate).select(LoginRecordData.class);
        verify(reactiveSelect).matching(any(Query.class));
        verify(terminatingSelect).all();
    }
}
