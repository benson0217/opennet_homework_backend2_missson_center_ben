package com.example.demo.user.application.service;

import com.example.demo.shared.application.dto.event.UserLoginEvent;
import com.example.demo.shared.infrastructure.message.EventPublisher;
import com.example.demo.user.application.service.impl.UserCommandServiceImpl;
import com.example.demo.user.domain.model.LoginRecord;
import com.example.demo.user.domain.model.User;
import com.example.demo.user.domain.repository.LoginRecordRepository;
import com.example.demo.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserCommandServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoginRecordRepository loginRecordRepository;

    private EventPublisher eventPublisher;

    private UserCommandServiceImpl userCommandService;

    private User existingUser;
    private LoginRecord loginRecord;

    @BeforeEach
    void setUp() {
        eventPublisher = mock(EventPublisher.class);
        userCommandService = new UserCommandServiceImpl(userRepository, loginRecordRepository, eventPublisher);
        
        existingUser = User.builder()
                .id(1L)
                .username("testuser")
                .points(100)
                .registrationDate(LocalDateTime.now().minusDays(5))
                .createdAt(LocalDateTime.now().minusDays(5))
                .updatedAt(LocalDateTime.now().minusDays(5))
                .build();

        loginRecord = LoginRecord.builder()
                .id(1L)
                .userId(1L)
                .loginDate(LocalDate.now())
                .loginTime(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        
        // Lenient stub to prevent NPE in switchIfEmpty when not explicitly stubbed
        lenient().when(userRepository.save(any(User.class))).thenReturn(Mono.just(existingUser));
    }

    @Test
    void handleLogin_shouldReturnExistingUser_whenUserExists() {
        // Given
        String username = "testuser";
        when(userRepository.findByUsername(username)).thenReturn(Mono.just(existingUser));
        when(loginRecordRepository.existsByUserIdAndLoginDate(existingUser.getId(), LocalDate.now()))
                .thenReturn(Mono.just(true));
        when(eventPublisher.publishLoginEvent(any(UserLoginEvent.class))).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(userCommandService.handleLogin(username))
                .assertNext(user -> {
                    assertNotNull(user);
                    assertEquals(existingUser.getId(), user.getId());
                    assertEquals(username, user.getUsername());
                    assertEquals(100, user.getPoints());
                })
                .verifyComplete();

        verify(userRepository).findByUsername(username);
        verify(loginRecordRepository).existsByUserIdAndLoginDate(existingUser.getId(), LocalDate.now());
        verify(loginRecordRepository, never()).save(any(LoginRecord.class));
        verify(eventPublisher).publishLoginEvent(any(UserLoginEvent.class));
    }

    @Test
    void handleLogin_shouldCreateNewUser_whenUserDoesNotExist() {
        // Given
        String username = "newuser";
        User newUser = User.builder()
                .id(2L)
                .username(username)
                .points(0)
                .registrationDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findByUsername(username)).thenReturn(Mono.empty());
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(newUser));
        when(loginRecordRepository.existsByUserIdAndLoginDate(newUser.getId(), LocalDate.now()))
                .thenReturn(Mono.just(false));
        when(loginRecordRepository.save(any(LoginRecord.class))).thenReturn(Mono.just(loginRecord));
        when(eventPublisher.publishLoginEvent(any(UserLoginEvent.class))).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(userCommandService.handleLogin(username))
                .assertNext(user -> {
                    assertNotNull(user);
                    assertEquals(newUser.getId(), user.getId());
                    assertEquals(username, user.getUsername());
                    assertEquals(0, user.getPoints());
                })
                .verifyComplete();

        verify(userRepository).findByUsername(username);
        verify(userRepository).save(any(User.class));
        verify(loginRecordRepository).existsByUserIdAndLoginDate(newUser.getId(), LocalDate.now());
        verify(loginRecordRepository).save(any(LoginRecord.class));
        verify(eventPublisher).publishLoginEvent(any(UserLoginEvent.class));
    }

    @Test
    void handleLogin_shouldCreateLoginRecord_whenFirstLoginOfDay() {
        // Given
        String username = "testuser";
        when(userRepository.findByUsername(username)).thenReturn(Mono.just(existingUser));
        when(loginRecordRepository.existsByUserIdAndLoginDate(existingUser.getId(), LocalDate.now()))
                .thenReturn(Mono.just(false));
        when(loginRecordRepository.save(any(LoginRecord.class))).thenReturn(Mono.just(loginRecord));
        when(eventPublisher.publishLoginEvent(any(UserLoginEvent.class))).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(userCommandService.handleLogin(username))
                .assertNext(user -> {
                    assertNotNull(user);
                    assertEquals(existingUser.getId(), user.getId());
                })
                .verifyComplete();

        ArgumentCaptor<LoginRecord> loginRecordCaptor = ArgumentCaptor.forClass(LoginRecord.class);
        verify(loginRecordRepository).save(loginRecordCaptor.capture());

        LoginRecord savedRecord = loginRecordCaptor.getValue();
        assertEquals(existingUser.getId(), savedRecord.getUserId());
        assertEquals(LocalDate.now(), savedRecord.getLoginDate());
    }

    @Test
    void handleLogin_shouldNotCreateLoginRecord_whenAlreadyLoggedInToday() {
        // Given
        String username = "testuser";
        when(userRepository.findByUsername(username)).thenReturn(Mono.just(existingUser));
        when(loginRecordRepository.existsByUserIdAndLoginDate(existingUser.getId(), LocalDate.now()))
                .thenReturn(Mono.just(true));
        when(eventPublisher.publishLoginEvent(any(UserLoginEvent.class))).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(userCommandService.handleLogin(username))
                .assertNext(user -> {
                    assertNotNull(user);
                    assertEquals(existingUser.getId(), user.getId());
                })
                .verifyComplete();

        verify(loginRecordRepository).existsByUserIdAndLoginDate(existingUser.getId(), LocalDate.now());
        verify(loginRecordRepository, never()).save(any(LoginRecord.class));
    }

    @Test
    void handleLogin_shouldPublishLoginEvent_withCorrectData() {
        // Given
        String username = "testuser";
        when(userRepository.findByUsername(username)).thenReturn(Mono.just(existingUser));
        when(loginRecordRepository.existsByUserIdAndLoginDate(existingUser.getId(), LocalDate.now()))
                .thenReturn(Mono.just(true));
        when(eventPublisher.publishLoginEvent(any(UserLoginEvent.class))).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(userCommandService.handleLogin(username))
                .expectNextCount(1)
                .verifyComplete();

        ArgumentCaptor<UserLoginEvent> eventCaptor = ArgumentCaptor.forClass(UserLoginEvent.class);
        verify(eventPublisher).publishLoginEvent(eventCaptor.capture());

        UserLoginEvent publishedEvent = eventCaptor.getValue();
        assertEquals(existingUser.getId(), publishedEvent.userId());
        assertEquals(existingUser.getUsername(), publishedEvent.username());
        assertNotNull(publishedEvent.loginTime());
    }

    @Test
    void handleLogin_shouldPropagateError_whenEventPublishingFails() {
        // Given
        String username = "testuser";
        RuntimeException publishError = new RuntimeException("Event publishing failed");

        when(userRepository.findByUsername(username)).thenReturn(Mono.just(existingUser));
        when(loginRecordRepository.existsByUserIdAndLoginDate(existingUser.getId(), LocalDate.now()))
                .thenReturn(Mono.just(true));
        when(eventPublisher.publishLoginEvent(any(UserLoginEvent.class)))
                .thenReturn(Mono.error(publishError));

        // When & Then
        StepVerifier.create(userCommandService.handleLogin(username))
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException && 
                    throwable.getMessage().equals("Event publishing failed"))
                .verify();

        verify(eventPublisher).publishLoginEvent(any(UserLoginEvent.class));
    }

    @Test
    void handleLogin_shouldPropagateError_whenUserRepositoryFails() {
        // Given
        String username = "testuser";
        RuntimeException repositoryError = new RuntimeException("Database error");

        when(userRepository.findByUsername(username)).thenReturn(Mono.error(repositoryError));

        // When & Then
        StepVerifier.create(userCommandService.handleLogin(username))
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException && 
                    throwable.getMessage().equals("Database error"))
                .verify();

        verify(userRepository).findByUsername(username);
        verify(loginRecordRepository, never()).existsByUserIdAndLoginDate(anyLong(), any(LocalDate.class));
        verify(eventPublisher, never()).publishLoginEvent(any(UserLoginEvent.class));
    }

    @Test
    void handleLogin_shouldPropagateError_whenLoginRecordRepositoryFails() {
        // Given
        String username = "testuser";
        RuntimeException repositoryError = new RuntimeException("Failed to save login record");

        when(userRepository.findByUsername(username)).thenReturn(Mono.just(existingUser));
        when(loginRecordRepository.existsByUserIdAndLoginDate(existingUser.getId(), LocalDate.now()))
                .thenReturn(Mono.just(false));
        when(loginRecordRepository.save(any(LoginRecord.class)))
                .thenReturn(Mono.error(repositoryError));

        // When & Then
        StepVerifier.create(userCommandService.handleLogin(username))
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException && 
                    throwable.getMessage().equals("Failed to save login record"))
                .verify();

        verify(loginRecordRepository).save(any(LoginRecord.class));
        verify(eventPublisher, never()).publishLoginEvent(any(UserLoginEvent.class));
    }

    @Test
    void handleLogin_shouldPropagateError_whenCreatingNewUserFails() {
        // Given
        String username = "newuser";
        RuntimeException saveError = new RuntimeException("Failed to create user");

        when(userRepository.findByUsername(username)).thenReturn(Mono.empty());
        when(userRepository.save(any(User.class))).thenReturn(Mono.error(saveError));

        // When & Then
        StepVerifier.create(userCommandService.handleLogin(username))
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException && 
                    throwable.getMessage().equals("Failed to create user"))
                .verify();

        verify(userRepository).save(any(User.class));
        verify(loginRecordRepository, never()).existsByUserIdAndLoginDate(anyLong(), any(LocalDate.class));
        verify(eventPublisher, never()).publishLoginEvent(any(UserLoginEvent.class));
    }

    @Test
    void handleLogin_shouldHandleCompleteFlow_forNewUserFirstLogin() {
        // Given
        String username = "brandnewuser";
        User newUser = User.builder()
                .id(3L)
                .username(username)
                .points(0)
                .registrationDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findByUsername(username)).thenReturn(Mono.empty());
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(newUser));
        when(loginRecordRepository.existsByUserIdAndLoginDate(newUser.getId(), LocalDate.now()))
                .thenReturn(Mono.just(false));
        when(loginRecordRepository.save(any(LoginRecord.class))).thenReturn(Mono.just(loginRecord));
        when(eventPublisher.publishLoginEvent(any(UserLoginEvent.class))).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(userCommandService.handleLogin(username))
                .assertNext(user -> {
                    assertNotNull(user);
                    assertEquals(newUser.getId(), user.getId());
                    assertEquals(username, user.getUsername());
                })
                .verifyComplete();

        // Verify all operations were called in correct order
        verify(userRepository).findByUsername(username);
        verify(userRepository).save(any(User.class));
        verify(loginRecordRepository).existsByUserIdAndLoginDate(newUser.getId(), LocalDate.now());
        verify(loginRecordRepository).save(any(LoginRecord.class));
        verify(eventPublisher).publishLoginEvent(any(UserLoginEvent.class));
    }

    @Test
    void handleLogin_shouldPropagateError_whenCheckingExistingLoginRecordFails() {
        // Given
        String username = "testuser";
        RuntimeException checkError = new RuntimeException("Failed to check login record");

        when(userRepository.findByUsername(username)).thenReturn(Mono.just(existingUser));
        when(loginRecordRepository.existsByUserIdAndLoginDate(existingUser.getId(), LocalDate.now()))
                .thenReturn(Mono.error(checkError));

        // When & Then
        StepVerifier.create(userCommandService.handleLogin(username))
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException && 
                    throwable.getMessage().equals("Failed to check login record"))
                .verify();

        verify(loginRecordRepository).existsByUserIdAndLoginDate(existingUser.getId(), LocalDate.now());
        verify(loginRecordRepository, never()).save(any(LoginRecord.class));
        verify(eventPublisher, never()).publishLoginEvent(any(UserLoginEvent.class));
    }

    @Test
    void handleLogin_shouldNotPublishEvent_whenUserNotEligibleForMissions() {
        // Given - user registered more than 30 days ago
        String username = "olduser";
        User ineligibleUser = User.builder()
                .id(5L)
                .username(username)
                .points(100)
                .registrationDate(LocalDateTime.now().minusDays(35)) // Registered 35 days ago
                .createdAt(LocalDateTime.now().minusDays(35))
                .updatedAt(LocalDateTime.now().minusDays(35))
                .build();

        when(userRepository.findByUsername(username)).thenReturn(Mono.just(ineligibleUser));
        when(loginRecordRepository.existsByUserIdAndLoginDate(ineligibleUser.getId(), LocalDate.now()))
                .thenReturn(Mono.just(true));

        // When & Then
        StepVerifier.create(userCommandService.handleLogin(username))
                .assertNext(user -> {
                    assertNotNull(user);
                    assertEquals(ineligibleUser.getId(), user.getId());
                    assertEquals(username, user.getUsername());
                })
                .verifyComplete();

        verify(userRepository).findByUsername(username);
        verify(loginRecordRepository).existsByUserIdAndLoginDate(ineligibleUser.getId(), LocalDate.now());
        // Event should NOT be published for ineligible users
        verify(eventPublisher, never()).publishLoginEvent(any(UserLoginEvent.class));
    }
}
