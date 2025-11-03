package com.example.demo.user.application.service;

import com.example.demo.user.application.service.impl.UserQueryServiceImpl;
import com.example.demo.user.domain.model.LoginRecord;
import com.example.demo.user.domain.model.User;
import com.example.demo.user.domain.repository.LoginRecordRepository;
import com.example.demo.user.domain.repository.UserRepository;
import com.example.demo.user.domain.service.UserDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoginRecordRepository loginRecordRepository;

    @Mock
    private UserDomainService userDomainService;

    @InjectMocks
    private UserQueryServiceImpl userQueryService;

    private User testUser;

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
    }

    @Test
    void getUserByUsername_shouldReturnUser_whenUserExists() {
        // Given
        String username = "testuser";
        when(userRepository.findByUsername(username)).thenReturn(Mono.just(testUser));

        // When & Then
        StepVerifier.create(userQueryService.getUserByUsername(username))
                .assertNext(user -> {
                    assertNotNull(user);
                    assertEquals(testUser.getId(), user.getId());
                    assertEquals(username, user.getUsername());
                    assertEquals(100, user.getPoints());
                })
                .verifyComplete();

        verify(userRepository).findByUsername(username);
    }

    @Test
    void getUserByUsername_shouldThrowError_whenUserDoesNotExist() {
        // Given
        String username = "nonexistentuser";
        when(userRepository.findByUsername(username)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(userQueryService.getUserByUsername(username))
                .expectErrorMatches(throwable ->
                    throwable instanceof IllegalArgumentException &&
                    throwable.getMessage().contains("找不到使用者"))
                .verify();

        verify(userRepository).findByUsername(username);
    }

    @Test
    void getUserByUsername_shouldPropagateError_whenRepositoryFails() {
        // Given
        String username = "testuser";
        RuntimeException repositoryError = new RuntimeException("Database error");
        when(userRepository.findByUsername(username)).thenReturn(Mono.error(repositoryError));

        // When & Then
        StepVerifier.create(userQueryService.getUserByUsername(username))
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException && 
                    throwable.getMessage().equals("Database error"))
                .verify();

        verify(userRepository).findByUsername(username);
    }

    @Test
    void getConsecutiveLoginDays_shouldReturnZero_whenNoLoginRecords() {
        // Given
        Long userId = 1L;
        when(loginRecordRepository.findRecentByUserId(userId, 10)).thenReturn(Flux.empty());
        when(userDomainService.calculateConsecutiveLoginDays(Collections.emptyList())).thenReturn(0);

        // When & Then
        StepVerifier.create(userQueryService.getConsecutiveLoginDays(userId))
                .assertNext(days -> assertEquals(0, days))
                .verifyComplete();

        verify(loginRecordRepository).findRecentByUserId(userId, 10);
        verify(userDomainService).calculateConsecutiveLoginDays(Collections.emptyList());
    }

    @Test
    void getConsecutiveLoginDays_shouldReturnOne_whenOnlyOneLoginRecord() {
        // Given
        Long userId = 1L;
        LoginRecord loginRecord = createLoginRecord(1L, userId, LocalDate.now());
        
        when(loginRecordRepository.findRecentByUserId(userId, 10)).thenReturn(Flux.just(loginRecord));
        when(userDomainService.calculateConsecutiveLoginDays(any(List.class))).thenReturn(1);

        // When & Then
        StepVerifier.create(userQueryService.getConsecutiveLoginDays(userId))
                .assertNext(days -> assertEquals(1, days))
                .verifyComplete();

        verify(loginRecordRepository).findRecentByUserId(userId, 10);
        verify(userDomainService).calculateConsecutiveLoginDays(any(List.class));
    }

    @Test
    void getConsecutiveLoginDays_shouldReturnCorrectDays_whenMultipleConsecutiveDays() {
        // Given
        Long userId = 1L;
        LocalDate today = LocalDate.now();
        List<LoginRecord> loginRecords = Arrays.asList(
                createLoginRecord(1L, userId, today),
                createLoginRecord(2L, userId, today.minusDays(1)),
                createLoginRecord(3L, userId, today.minusDays(2))
        );
        
        when(loginRecordRepository.findRecentByUserId(userId, 10))
                .thenReturn(Flux.fromIterable(loginRecords));
        when(userDomainService.calculateConsecutiveLoginDays(any(List.class))).thenReturn(3);

        // When & Then
        StepVerifier.create(userQueryService.getConsecutiveLoginDays(userId))
                .assertNext(days -> assertEquals(3, days))
                .verifyComplete();

        verify(loginRecordRepository).findRecentByUserId(userId, 10);
        verify(userDomainService).calculateConsecutiveLoginDays(any(List.class));
    }

    @Test
    void getConsecutiveLoginDays_shouldReturnCorrectDays_whenThereIsAGap() {
        // Given
        Long userId = 1L;
        LocalDate today = LocalDate.now();
        List<LoginRecord> loginRecords = Arrays.asList(
                createLoginRecord(1L, userId, today),
                createLoginRecord(2L, userId, today.minusDays(1)),
                createLoginRecord(3L, userId, today.minusDays(3)) // Gap of 1 day
        );
        
        when(loginRecordRepository.findRecentByUserId(userId, 10))
                .thenReturn(Flux.fromIterable(loginRecords));
        when(userDomainService.calculateConsecutiveLoginDays(any(List.class))).thenReturn(2);

        // When & Then
        StepVerifier.create(userQueryService.getConsecutiveLoginDays(userId))
                .assertNext(days -> assertEquals(2, days))
                .verifyComplete();

        verify(loginRecordRepository).findRecentByUserId(userId, 10);
        verify(userDomainService).calculateConsecutiveLoginDays(any(List.class));
    }

    @Test
    void getConsecutiveLoginDays_shouldHandleLargeNumberOfRecords() {
        // Given
        Long userId = 1L;
        LocalDate today = LocalDate.now();
        List<LoginRecord> loginRecords = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            loginRecords.add(createLoginRecord((long) (i + 1), userId, today.minusDays(i)));
        }
        
        when(loginRecordRepository.findRecentByUserId(userId, 10))
                .thenReturn(Flux.fromIterable(loginRecords));
        when(userDomainService.calculateConsecutiveLoginDays(any(List.class))).thenReturn(10);

        // When & Then
        StepVerifier.create(userQueryService.getConsecutiveLoginDays(userId))
                .assertNext(days -> assertEquals(10, days))
                .verifyComplete();

        verify(loginRecordRepository).findRecentByUserId(userId, 10);
        verify(userDomainService).calculateConsecutiveLoginDays(any(List.class));
    }

    @Test
    void getConsecutiveLoginDays_shouldPropagateError_whenRepositoryFails() {
        // Given
        Long userId = 1L;
        RuntimeException repositoryError = new RuntimeException("Database error");
        when(loginRecordRepository.findRecentByUserId(userId, 10))
                .thenReturn(Flux.error(repositoryError));

        // When & Then
        StepVerifier.create(userQueryService.getConsecutiveLoginDays(userId))
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException && 
                    throwable.getMessage().equals("Database error"))
                .verify();

        verify(loginRecordRepository).findRecentByUserId(userId, 10);
        verify(userDomainService, never()).calculateConsecutiveLoginDays(any(List.class));
    }

    @Test
    void getConsecutiveLoginDays_shouldCallDomainService_withCorrectList() {
        // Given
        Long userId = 1L;
        LocalDate today = LocalDate.now();
        List<LoginRecord> loginRecords = Arrays.asList(
                createLoginRecord(1L, userId, today),
                createLoginRecord(2L, userId, today.minusDays(1))
        );
        
        when(loginRecordRepository.findRecentByUserId(userId, 10))
                .thenReturn(Flux.fromIterable(loginRecords));
        when(userDomainService.calculateConsecutiveLoginDays(any(List.class))).thenReturn(2);

        // When & Then
        StepVerifier.create(userQueryService.getConsecutiveLoginDays(userId))
                .assertNext(days -> assertEquals(2, days))
                .verifyComplete();

        verify(userDomainService).calculateConsecutiveLoginDays(argThat(list -> 
            list != null && 
            list.size() == 2 && 
            list.get(0).getUserId().equals(userId) &&
            list.get(1).getUserId().equals(userId)
        ));
    }

    @Test
    void getConsecutiveLoginDays_shouldHandleDuplicateDates() {
        // Given
        Long userId = 1L;
        LocalDate today = LocalDate.now();
        List<LoginRecord> loginRecords = Arrays.asList(
                createLoginRecord(1L, userId, today),
                createLoginRecord(2L, userId, today), // Duplicate date
                createLoginRecord(3L, userId, today.minusDays(1))
        );
        
        when(loginRecordRepository.findRecentByUserId(userId, 10))
                .thenReturn(Flux.fromIterable(loginRecords));
        when(userDomainService.calculateConsecutiveLoginDays(any(List.class))).thenReturn(2);

        // When & Then
        StepVerifier.create(userQueryService.getConsecutiveLoginDays(userId))
                .assertNext(days -> assertEquals(2, days))
                .verifyComplete();

        verify(loginRecordRepository).findRecentByUserId(userId, 10);
        verify(userDomainService).calculateConsecutiveLoginDays(any(List.class));
    }

    @Test
    void getUserByUsername_shouldReturnCorrectUserType() {
        // Given
        String username = "testuser";
        when(userRepository.findByUsername(username)).thenReturn(Mono.just(testUser));

        // When & Then
        StepVerifier.create(userQueryService.getUserByUsername(username))
                .assertNext(user -> {
                    assertNotNull(user);
                    assertTrue(user instanceof User);
                    assertEquals(User.class, user.getClass());
                })
                .verifyComplete();

        verify(userRepository).findByUsername(username);
    }

    @Test
    void getConsecutiveLoginDays_shouldRequestExactlyTenRecords() {
        // Given
        Long userId = 1L;
        when(loginRecordRepository.findRecentByUserId(userId, 10)).thenReturn(Flux.empty());
        when(userDomainService.calculateConsecutiveLoginDays(Collections.emptyList())).thenReturn(0);

        // When
        StepVerifier.create(userQueryService.getConsecutiveLoginDays(userId))
                .expectNext(0)
                .verifyComplete();

        // Then
        verify(loginRecordRepository).findRecentByUserId(eq(userId), eq(10));
    }

    @Test
    void getUserByUsername_shouldHandleNullUsername() {
        // Given
        String username = null;
        when(userRepository.findByUsername(username)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(userQueryService.getUserByUsername(username))
                .expectErrorMatches(throwable ->
                    throwable instanceof IllegalArgumentException &&
                    throwable.getMessage().contains("找不到使用者"))
                .verify();

        verify(userRepository).findByUsername(username);
    }

    @Test
    void getUserByUsername_shouldHandleEmptyUsername() {
        // Given
        String username = "";
        when(userRepository.findByUsername(username)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(userQueryService.getUserByUsername(username))
                .expectErrorMatches(throwable ->
                    throwable instanceof IllegalArgumentException &&
                    throwable.getMessage().contains("找不到使用者"))
                .verify();

        verify(userRepository).findByUsername(username);
    }

    @Test
    void getConsecutiveLoginDays_shouldHandleNonConsecutiveDates() {
        // Given
        Long userId = 1L;
        LocalDate today = LocalDate.now();
        List<LoginRecord> loginRecords = Arrays.asList(
                createLoginRecord(1L, userId, today),
                createLoginRecord(2L, userId, today.minusDays(5)),
                createLoginRecord(3L, userId, today.minusDays(10))
        );
        
        when(loginRecordRepository.findRecentByUserId(userId, 10))
                .thenReturn(Flux.fromIterable(loginRecords));
        when(userDomainService.calculateConsecutiveLoginDays(any(List.class))).thenReturn(1);

        // When & Then
        StepVerifier.create(userQueryService.getConsecutiveLoginDays(userId))
                .assertNext(days -> assertEquals(1, days))
                .verifyComplete();

        verify(loginRecordRepository).findRecentByUserId(userId, 10);
        verify(userDomainService).calculateConsecutiveLoginDays(any(List.class));
    }

    @Test
    void findUserByIdOrThrow_shouldReturnUser_whenUserExists() {
        // Given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Mono.just(testUser));

        // When & Then
        StepVerifier.create(userQueryService.findUserByIdOrThrow(userId))
                .assertNext(user -> {
                    assertNotNull(user);
                    assertEquals(userId, user.getId());
                    assertEquals("testuser", user.getUsername());
                    assertEquals(100, user.getPoints());
                })
                .verifyComplete();

        verify(userRepository).findById(userId);
    }

    @Test
    void findUserByIdOrThrow_shouldThrowError_whenUserNotFound() {
        // Given
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(userQueryService.findUserByIdOrThrow(userId))
                .expectErrorMatches(throwable ->
                    throwable instanceof IllegalArgumentException &&
                    throwable.getMessage().equals("找不到使用者: " + userId))
                .verify();

        verify(userRepository).findById(userId);
    }

    @Test
    void findUserByIdOrThrow_shouldPropagateError_whenRepositoryFails() {
        // Given
        Long userId = 1L;
        RuntimeException repositoryError = new RuntimeException("Database error");
        when(userRepository.findById(userId)).thenReturn(Mono.error(repositoryError));

        // When & Then
        StepVerifier.create(userQueryService.findUserByIdOrThrow(userId))
                .expectErrorMatches(throwable ->
                    throwable instanceof RuntimeException &&
                    throwable.getMessage().equals("Database error"))
                .verify();

        verify(userRepository).findById(userId);
    }

    private LoginRecord createLoginRecord(Long id, Long userId, LocalDate loginDate) {
        return LoginRecord.builder()
                .id(id)
                .userId(userId)
                .loginDate(loginDate)
                .loginTime(loginDate.atStartOfDay())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
