package com.example.demo.game.interfaces.controller;

import com.example.demo.game.application.service.impl.GameCommandServiceImpl;
import com.example.demo.shared.application.dto.LaunchGameRequest;
import com.example.demo.shared.application.dto.PlayGameRequest;
import com.example.demo.user.application.service.UserQueryService;
import com.example.demo.user.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class GameControllerTest {

    @Mock
    private UserQueryService userQueryService;

    @Mock
    private GameCommandServiceImpl gameCommandService;

    @InjectMocks
    private GameController gameController;

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
    void launchGame_shouldReturnSuccess_whenGameLaunchSuccessful() {
        // Given
        LaunchGameRequest request = new LaunchGameRequest("testuser", "GAME001");
        when(userQueryService.getUserByUsername("testuser")).thenReturn(Mono.just(testUser));
        when(gameCommandService.handleGameLaunch(testUser, "GAME001")).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(gameController.launchGame(request))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertTrue(response.success());
                    assertEquals("遊戲啟動成功", response.message());
                    assertNull(response.data());
                })
                .verifyComplete();

        verify(userQueryService).getUserByUsername("testuser");
        verify(gameCommandService).handleGameLaunch(testUser, "GAME001");
    }

    @Test
    void launchGame_shouldReturnError_whenUserNotFound() {
        // Given
        LaunchGameRequest request = new LaunchGameRequest("nonexistent", "GAME001");
        when(userQueryService.getUserByUsername("nonexistent"))
                .thenReturn(Mono.error(new IllegalArgumentException("找不到使用者: nonexistent")));

        // When & Then
        StepVerifier.create(gameController.launchGame(request))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertFalse(response.success());
                    assertTrue(response.message().contains("啟動遊戲失敗"));
                    assertTrue(response.message().contains("找不到使用者"));
                })
                .verifyComplete();

        verify(userQueryService).getUserByUsername("nonexistent");
        verify(gameCommandService, never()).handleGameLaunch(any(User.class), anyString());
    }

    @Test
    void launchGame_shouldReturnError_whenGameServiceFails() {
        // Given
        LaunchGameRequest request = new LaunchGameRequest("testuser", "GAME001");
        RuntimeException serviceError = new RuntimeException("Game service error");
        when(userQueryService.getUserByUsername("testuser")).thenReturn(Mono.just(testUser));
        when(gameCommandService.handleGameLaunch(testUser, "GAME001")).thenReturn(Mono.error(serviceError));

        // When & Then
        StepVerifier.create(gameController.launchGame(request))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertFalse(response.success());
                    assertTrue(response.message().contains("啟動遊戲失敗"));
                    assertTrue(response.message().contains("Game service error"));
                })
                .verifyComplete();

        verify(userQueryService).getUserByUsername("testuser");
        verify(gameCommandService).handleGameLaunch(testUser, "GAME001");
    }

    @Test
    void launchGame_shouldReturnError_whenUserQueryServiceFails() {
        // Given
        LaunchGameRequest request = new LaunchGameRequest("testuser", "GAME001");
        RuntimeException queryError = new RuntimeException("User query failed");
        when(userQueryService.getUserByUsername("testuser")).thenReturn(Mono.error(queryError));

        // When & Then
        StepVerifier.create(gameController.launchGame(request))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertFalse(response.success());
                    assertTrue(response.message().contains("啟動遊戲失敗"));
                    assertTrue(response.message().contains("User query failed"));
                })
                .verifyComplete();

        verify(userQueryService).getUserByUsername("testuser");
        verify(gameCommandService, never()).handleGameLaunch(any(User.class), anyString());
    }

    @Test
    void launchGame_shouldCallServicesWithCorrectParameters() {
        // Given
        LaunchGameRequest request = new LaunchGameRequest("testuser", "GAME001");
        when(userQueryService.getUserByUsername("testuser")).thenReturn(Mono.just(testUser));
        when(gameCommandService.handleGameLaunch(testUser, "GAME001")).thenReturn(Mono.empty());

        // When
        StepVerifier.create(gameController.launchGame(request))
                .expectNextCount(1)
                .verifyComplete();

        // Then
        verify(userQueryService).getUserByUsername("testuser");
        verify(gameCommandService).handleGameLaunch(testUser, "GAME001");
    }

    @Test
    void playGame_shouldReturnSuccess_whenGamePlaySuccessful() {
        // Given
        PlayGameRequest request = new PlayGameRequest("testuser", "GAME001", 1500, 300);
        when(userQueryService.getUserByUsername("testuser")).thenReturn(Mono.just(testUser));
        when(gameCommandService.handleGamePlay(testUser, "GAME001", 1500, 300)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(gameController.playGame(request))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertTrue(response.success());
                    assertEquals("遊戲記錄成功", response.message());
                    assertNull(response.data());
                })
                .verifyComplete();

        verify(userQueryService).getUserByUsername("testuser");
        verify(gameCommandService).handleGamePlay(testUser, "GAME001", 1500, 300);
    }

    @Test
    void playGame_shouldReturnError_whenUserNotFound() {
        // Given
        PlayGameRequest request = new PlayGameRequest("nonexistent", "GAME001", 1500, 300);
        when(userQueryService.getUserByUsername("nonexistent"))
                .thenReturn(Mono.error(new IllegalArgumentException("找不到使用者: nonexistent")));

        // When & Then
        StepVerifier.create(gameController.playGame(request))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertFalse(response.success());
                    assertTrue(response.message().contains("遊玩遊戲失敗"));
                    assertTrue(response.message().contains("找不到使用者"));
                })
                .verifyComplete();

        verify(userQueryService).getUserByUsername("nonexistent");
        verify(gameCommandService, never()).handleGamePlay(any(User.class), anyString(), anyInt(), any());
    }

    @Test
    void playGame_shouldReturnError_whenGameServiceFails() {
        // Given
        PlayGameRequest request = new PlayGameRequest("testuser", "GAME001", 1500, 300);
        RuntimeException serviceError = new RuntimeException("Game play failed");
        when(userQueryService.getUserByUsername("testuser")).thenReturn(Mono.just(testUser));
        when(gameCommandService.handleGamePlay(testUser, "GAME001", 1500, 300)).thenReturn(Mono.error(serviceError));

        // When & Then
        StepVerifier.create(gameController.playGame(request))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertFalse(response.success());
                    assertTrue(response.message().contains("遊玩遊戲失敗"));
                    assertTrue(response.message().contains("Game play failed"));
                })
                .verifyComplete();

        verify(userQueryService).getUserByUsername("testuser");
        verify(gameCommandService).handleGamePlay(testUser, "GAME001", 1500, 300);
    }

    @Test
    void playGame_shouldReturnError_whenUserQueryServiceFails() {
        // Given
        PlayGameRequest request = new PlayGameRequest("testuser", "GAME001", 1500, 300);
        RuntimeException queryError = new RuntimeException("User query failed");
        when(userQueryService.getUserByUsername("testuser")).thenReturn(Mono.error(queryError));

        // When & Then
        StepVerifier.create(gameController.playGame(request))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertFalse(response.success());
                    assertTrue(response.message().contains("遊玩遊戲失敗"));
                    assertTrue(response.message().contains("User query failed"));
                })
                .verifyComplete();

        verify(userQueryService).getUserByUsername("testuser");
        verify(gameCommandService, never()).handleGamePlay(any(User.class), anyString(), anyInt(), any());
    }

    @Test
    void playGame_shouldCallServicesWithCorrectParameters() {
        // Given
        PlayGameRequest request = new PlayGameRequest("testuser", "GAME001", 1500, 300);
        when(userQueryService.getUserByUsername("testuser")).thenReturn(Mono.just(testUser));
        when(gameCommandService.handleGamePlay(testUser, "GAME001", 1500, 300)).thenReturn(Mono.empty());

        // When
        StepVerifier.create(gameController.playGame(request))
                .expectNextCount(1)
                .verifyComplete();

        // Then
        verify(userQueryService).getUserByUsername("testuser");
        verify(gameCommandService).handleGamePlay(testUser, "GAME001", 1500, 300);
    }

    @Test
    void playGame_shouldHandleNullPlayDuration() {
        // Given
        PlayGameRequest request = new PlayGameRequest("testuser", "GAME001", 1500, null);
        when(userQueryService.getUserByUsername("testuser")).thenReturn(Mono.just(testUser));
        when(gameCommandService.handleGamePlay(testUser, "GAME001", 1500, null)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(gameController.playGame(request))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertTrue(response.success());
                    assertEquals("遊戲記錄成功", response.message());
                })
                .verifyComplete();

        verify(gameCommandService).handleGamePlay(testUser, "GAME001", 1500, null);
    }

    @Test
    void playGame_shouldHandleZeroScore() {
        // Given
        PlayGameRequest request = new PlayGameRequest("testuser", "GAME001", 0, 300);
        when(userQueryService.getUserByUsername("testuser")).thenReturn(Mono.just(testUser));
        when(gameCommandService.handleGamePlay(testUser, "GAME001", 0, 300)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(gameController.playGame(request))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertTrue(response.success());
                })
                .verifyComplete();

        verify(gameCommandService).handleGamePlay(testUser, "GAME001", 0, 300);
    }

    @Test
    void launchGame_shouldHandleIllegalArgumentException() {
        // Given
        LaunchGameRequest request = new LaunchGameRequest("testuser", "INVALID_GAME");
        when(userQueryService.getUserByUsername("testuser")).thenReturn(Mono.just(testUser));
        when(gameCommandService.handleGameLaunch(testUser, "INVALID_GAME"))
                .thenReturn(Mono.error(new IllegalArgumentException("找不到遊戲: INVALID_GAME")));

        // When & Then
        StepVerifier.create(gameController.launchGame(request))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertFalse(response.success());
                    assertTrue(response.message().contains("找不到遊戲"));
                })
                .verifyComplete();

        verify(gameCommandService).handleGameLaunch(testUser, "INVALID_GAME");
    }

    @Test
    void playGame_shouldHandleIllegalArgumentException() {
        // Given
        PlayGameRequest request = new PlayGameRequest("testuser", "INVALID_GAME", 1500, 300);
        when(userQueryService.getUserByUsername("testuser")).thenReturn(Mono.just(testUser));
        when(gameCommandService.handleGamePlay(testUser, "INVALID_GAME", 1500, 300))
                .thenReturn(Mono.error(new IllegalArgumentException("找不到遊戲: INVALID_GAME")));

        // When & Then
        StepVerifier.create(gameController.playGame(request))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertFalse(response.success());
                    assertTrue(response.message().contains("找不到遊戲"));
                })
                .verifyComplete();

        verify(gameCommandService).handleGamePlay(testUser, "INVALID_GAME", 1500, 300);
    }

    @Test
    void launchGame_shouldReturnCorrectResponseStructure() {
        // Given
        LaunchGameRequest request = new LaunchGameRequest("testuser", "GAME001");
        when(userQueryService.getUserByUsername("testuser")).thenReturn(Mono.just(testUser));
        when(gameCommandService.handleGameLaunch(testUser, "GAME001")).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(gameController.launchGame(request))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertNotNull(response.message());
                    assertTrue(response.success());
                })
                .verifyComplete();
    }

    @Test
    void playGame_shouldReturnCorrectResponseStructure() {
        // Given
        PlayGameRequest request = new PlayGameRequest("testuser", "GAME001", 1500, 300);
        when(userQueryService.getUserByUsername("testuser")).thenReturn(Mono.just(testUser));
        when(gameCommandService.handleGamePlay(testUser, "GAME001", 1500, 300)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(gameController.playGame(request))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertNotNull(response.message());
                    assertTrue(response.success());
                })
                .verifyComplete();
    }
}
