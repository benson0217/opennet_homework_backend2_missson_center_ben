package com.example.demo.user.domain.service;

import com.example.demo.user.domain.model.LoginRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserDomainServiceTest {

    private UserDomainService userDomainService;

    @BeforeEach
    void setUp() {
        userDomainService = new UserDomainService();
    }

    @Test
    void calculateConsecutiveLoginDays_shouldReturnZero_whenLoginRecordsIsNull() {
        // When
        int result = userDomainService.calculateConsecutiveLoginDays(null);

        // Then
        assertEquals(0, result);
    }

    @Test
    void calculateConsecutiveLoginDays_shouldReturnZero_whenLoginRecordsIsEmpty() {
        // Given
        List<LoginRecord> loginRecords = Collections.emptyList();

        // When
        int result = userDomainService.calculateConsecutiveLoginDays(loginRecords);

        // Then
        assertEquals(0, result);
    }

    @Test
    void calculateConsecutiveLoginDays_shouldReturnOne_whenOnlyOneLoginRecord() {
        // Given
        LocalDate today = LocalDate.now();
        List<LoginRecord> loginRecords = Arrays.asList(
                createLoginRecord(1L, 1L, today)
        );

        // When
        int result = userDomainService.calculateConsecutiveLoginDays(loginRecords);

        // Then
        assertEquals(1, result);
    }

    @Test
    void calculateConsecutiveLoginDays_shouldReturnCorrectDays_whenConsecutiveDaysFromToday() {
        // Given
        LocalDate today = LocalDate.now();
        List<LoginRecord> loginRecords = Arrays.asList(
                createLoginRecord(1L, 1L, today),
                createLoginRecord(2L, 1L, today.minusDays(1)),
                createLoginRecord(3L, 1L, today.minusDays(2))
        );

        // When
        int result = userDomainService.calculateConsecutiveLoginDays(loginRecords);

        // Then
        assertEquals(3, result);
    }

    @Test
    void calculateConsecutiveLoginDays_shouldReturnCorrectDays_whenConsecutiveDaysWithFiveRecords() {
        // Given
        LocalDate today = LocalDate.now();
        List<LoginRecord> loginRecords = Arrays.asList(
                createLoginRecord(1L, 1L, today),
                createLoginRecord(2L, 1L, today.minusDays(1)),
                createLoginRecord(3L, 1L, today.minusDays(2)),
                createLoginRecord(4L, 1L, today.minusDays(3)),
                createLoginRecord(5L, 1L, today.minusDays(4))
        );

        // When
        int result = userDomainService.calculateConsecutiveLoginDays(loginRecords);

        // Then
        assertEquals(5, result);
    }

    @Test
    void calculateConsecutiveLoginDays_shouldStopCounting_whenThereIsAGap() {
        // Given
        LocalDate today = LocalDate.now();
        List<LoginRecord> loginRecords = Arrays.asList(
                createLoginRecord(1L, 1L, today),
                createLoginRecord(2L, 1L, today.minusDays(1)),
                createLoginRecord(3L, 1L, today.minusDays(3)), // Gap of 1 day
                createLoginRecord(4L, 1L, today.minusDays(4))
        );

        // When
        int result = userDomainService.calculateConsecutiveLoginDays(loginRecords);

        // Then
        assertEquals(2, result, "Should only count consecutive days before the gap");
    }

    @Test
    void calculateConsecutiveLoginDays_shouldHandleLargeGap() {
        // Given
        LocalDate today = LocalDate.now();
        List<LoginRecord> loginRecords = Arrays.asList(
                createLoginRecord(1L, 1L, today),
                createLoginRecord(2L, 1L, today.minusDays(10)), // Large gap
                createLoginRecord(3L, 1L, today.minusDays(11))
        );

        // When
        int result = userDomainService.calculateConsecutiveLoginDays(loginRecords);

        // Then
        assertEquals(1, result, "Should only count the most recent login");
    }

    @Test
    void calculateConsecutiveLoginDays_shouldHandleDuplicateDates() {
        // Given
        LocalDate today = LocalDate.now();
        List<LoginRecord> loginRecords = Arrays.asList(
                createLoginRecord(1L, 1L, today),
                createLoginRecord(2L, 1L, today), // Duplicate
                createLoginRecord(3L, 1L, today.minusDays(1)),
                createLoginRecord(4L, 1L, today.minusDays(1)) // Duplicate
        );

        // When
        int result = userDomainService.calculateConsecutiveLoginDays(loginRecords);

        // Then
        assertEquals(2, result, "Should count distinct dates only");
    }

    @Test
    void calculateConsecutiveLoginDays_shouldHandleUnsortedDates() {
        // Given
        LocalDate today = LocalDate.now();
        List<LoginRecord> loginRecords = Arrays.asList(
                createLoginRecord(1L, 1L, today.minusDays(2)),
                createLoginRecord(2L, 1L, today),
                createLoginRecord(3L, 1L, today.minusDays(1))
        );

        // When
        int result = userDomainService.calculateConsecutiveLoginDays(loginRecords);

        // Then
        assertEquals(3, result, "Should sort dates internally and count correctly");
    }

    @Test
    void calculateConsecutiveLoginDays_shouldCountFromMostRecentDate() {
        // Given
        LocalDate today = LocalDate.now();
        List<LoginRecord> loginRecords = Arrays.asList(
                createLoginRecord(1L, 1L, today.minusDays(5)),
                createLoginRecord(2L, 1L, today.minusDays(6)),
                createLoginRecord(3L, 1L, today.minusDays(7)),
                createLoginRecord(4L, 1L, today.minusDays(10)), // Gap before this
                createLoginRecord(5L, 1L, today.minusDays(11))
        );

        // When
        int result = userDomainService.calculateConsecutiveLoginDays(loginRecords);

        // Then
        assertEquals(3, result, "Should count consecutive days from the most recent date");
    }


    @Test
    void calculateConsecutiveLoginDays_shouldHandleTenConsecutiveDays() {
        // Given
        LocalDate today = LocalDate.now();
        List<LoginRecord> loginRecords = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            loginRecords.add(createLoginRecord((long) (i + 1), 1L, today.minusDays(i)));
        }

        // When
        int result = userDomainService.calculateConsecutiveLoginDays(loginRecords);

        // Then
        assertEquals(10, result);
    }

    @Test
    void calculateConsecutiveLoginDays_shouldHandleOnlyNonConsecutiveDates() {
        // Given
        LocalDate today = LocalDate.now();
        List<LoginRecord> loginRecords = Arrays.asList(
                createLoginRecord(1L, 1L, today),
                createLoginRecord(2L, 1L, today.minusDays(3)),
                createLoginRecord(3L, 1L, today.minusDays(6)),
                createLoginRecord(4L, 1L, today.minusDays(10))
        );

        // When
        int result = userDomainService.calculateConsecutiveLoginDays(loginRecords);

        // Then
        assertEquals(1, result, "Should return 1 when no consecutive days exist");
    }

    @Test
    void calculateConsecutiveLoginDays_shouldHandleMixedUserIds() {
        // Given - multiple user IDs in the list (although in practice, should be filtered by userId)
        LocalDate today = LocalDate.now();
        List<LoginRecord> loginRecords = Arrays.asList(
                createLoginRecord(1L, 1L, today),
                createLoginRecord(2L, 2L, today), // Different user
                createLoginRecord(3L, 1L, today.minusDays(1)),
                createLoginRecord(4L, 1L, today.minusDays(2))
        );

        // When
        int result = userDomainService.calculateConsecutiveLoginDays(loginRecords);

        // Then
        assertEquals(3, result, "Should count consecutive days regardless of userId in the records");
    }


    @Test
    void calculateConsecutiveLoginDays_shouldHandleFutureDates() {
        // Given
        LocalDate today = LocalDate.now();
        List<LoginRecord> loginRecords = Arrays.asList(
                createLoginRecord(1L, 1L, today.plusDays(1)), // Future date
                createLoginRecord(2L, 1L, today),
                createLoginRecord(3L, 1L, today.minusDays(1))
        );

        // When
        int result = userDomainService.calculateConsecutiveLoginDays(loginRecords);

        // Then
        assertEquals(3, result, "Should handle future dates correctly by sorting");
    }

    private LoginRecord createLoginRecord(Long id, Long userId, LocalDate loginDate) {
        return LoginRecord.builder()
                .id(id)
                .userId(userId)
                .loginDate(loginDate)
                .loginTime(loginDate != null ? loginDate.atStartOfDay() : LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
