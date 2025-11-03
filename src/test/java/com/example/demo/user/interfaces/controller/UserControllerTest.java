package com.example.demo.user.interfaces.controller;

import com.example.demo.mission.application.service.MissionCommandService;
import com.example.demo.shared.application.dto.ApiResponse;
import com.example.demo.shared.application.dto.LoginRequest;
import com.example.demo.user.application.service.UserCommandService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserCommandService userCommandService;

    @Mock
    private MissionCommandService missionCommandService;

    @InjectMocks
    private UserController userController;

    private User testUser;
    private LoginRequest loginRequest;

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

        loginRequest = new LoginRequest("testuser");
    }

    @Test
    void login_shouldReturnSuccess_whenLoginSuccessful() {
        // Given
        when(userCommandService.handleLogin(anyString())).thenReturn(Mono.just(testUser));
        when(missionCommandService.initializeMissions(anyLong())).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(userController.login(loginRequest))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertTrue(response.success());
                    assertEquals("登入成功", response.message());
                    assertNull(response.data());
                })
                .verifyComplete();

        verify(userCommandService).handleLogin("testuser");
        verify(missionCommandService).initializeMissions(1L);
    }

    @Test
    void login_shouldInitializeMissions_afterSuccessfulLogin() {
        // Given
        when(userCommandService.handleLogin(anyString())).thenReturn(Mono.just(testUser));
        when(missionCommandService.initializeMissions(anyLong())).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(userController.login(loginRequest))
                .expectNextCount(1)
                .verifyComplete();

        verify(userCommandService).handleLogin("testuser");
        verify(missionCommandService).initializeMissions(1L);
    }

    @Test
    void login_shouldReturnError_whenUserCommandServiceFails() {
        // Given
        RuntimeException exception = new RuntimeException("Database error");
        when(userCommandService.handleLogin(anyString())).thenReturn(Mono.error(exception));

        // When & Then
        StepVerifier.create(userController.login(loginRequest))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertFalse(response.success());
                    assertTrue(response.message().contains("登入失敗"));
                    assertTrue(response.message().contains("Database error"));
                })
                .verifyComplete();

        verify(userCommandService).handleLogin("testuser");
        verify(missionCommandService, never()).initializeMissions(anyLong());
    }

    @Test
    void login_shouldReturnError_whenMissionInitializationFails() {
        // Given
        RuntimeException exception = new RuntimeException("Mission initialization failed");
        when(userCommandService.handleLogin(anyString())).thenReturn(Mono.just(testUser));
        when(missionCommandService.initializeMissions(anyLong())).thenReturn(Mono.error(exception));

        // When & Then
        StepVerifier.create(userController.login(loginRequest))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertFalse(response.success());
                    assertTrue(response.message().contains("登入失敗"));
                    assertTrue(response.message().contains("Mission initialization failed"));
                })
                .verifyComplete();

        verify(userCommandService).handleLogin("testuser");
        verify(missionCommandService).initializeMissions(1L);
    }

    @Test
    void login_shouldHandleNewUser_successfully() {
        // Given
        User newUser = User.builder()
                .id(2L)
                .username("newuser")
                .points(0)
                .registrationDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        LoginRequest newUserRequest = new LoginRequest("newuser");
        when(userCommandService.handleLogin("newuser")).thenReturn(Mono.just(newUser));
        when(missionCommandService.initializeMissions(2L)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(userController.login(newUserRequest))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertTrue(response.success());
                    assertEquals("登入成功", response.message());
                })
                .verifyComplete();

        verify(userCommandService).handleLogin("newuser");
        verify(missionCommandService).initializeMissions(2L);
    }

    @Test
    void login_shouldHandleExistingUser_successfully() {
        // Given
        when(userCommandService.handleLogin(anyString())).thenReturn(Mono.just(testUser));
        when(missionCommandService.initializeMissions(anyLong())).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(userController.login(loginRequest))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertTrue(response.success());
                    assertEquals("登入成功", response.message());
                })
                .verifyComplete();

        verify(userCommandService).handleLogin("testuser");
        verify(missionCommandService).initializeMissions(1L);
    }

    @Test
    void login_shouldCallServicesInCorrectOrder() {
        // Given
        when(userCommandService.handleLogin(anyString())).thenReturn(Mono.just(testUser));
        when(missionCommandService.initializeMissions(anyLong())).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(userController.login(loginRequest))
                .expectNextCount(1)
                .verifyComplete();

        // Verify order of invocations
        var inOrder = inOrder(userCommandService, missionCommandService);
        inOrder.verify(userCommandService).handleLogin("testuser");
        inOrder.verify(missionCommandService).initializeMissions(1L);
    }

    @Test
    void login_shouldReturnVoidData_whenSuccessful() {
        // Given
        when(userCommandService.handleLogin(anyString())).thenReturn(Mono.just(testUser));
        when(missionCommandService.initializeMissions(anyLong())).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(userController.login(loginRequest))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertNull(response.data());
                    assertTrue(response.success());
                })
                .verifyComplete();
    }

    @Test
    void login_shouldHandleNullPointerException_gracefully() {
        // Given
        when(userCommandService.handleLogin(anyString()))
                .thenReturn(Mono.error(new NullPointerException("Null user")));

        // When & Then
        StepVerifier.create(userController.login(loginRequest))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertFalse(response.success());
                    assertTrue(response.message().contains("登入失敗"));
                    assertTrue(response.message().contains("Null user"));
                })
                .verifyComplete();

        verify(userCommandService).handleLogin("testuser");
        verify(missionCommandService, never()).initializeMissions(anyLong());
    }

    @Test
    void login_shouldHandleIllegalArgumentException_gracefully() {
        // Given
        when(userCommandService.handleLogin(anyString()))
                .thenReturn(Mono.error(new IllegalArgumentException("Invalid username")));

        // When & Then
        StepVerifier.create(userController.login(loginRequest))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertFalse(response.success());
                    assertTrue(response.message().contains("登入失敗"));
                    assertTrue(response.message().contains("Invalid username"));
                })
                .verifyComplete();

        verify(userCommandService).handleLogin("testuser");
        verify(missionCommandService, never()).initializeMissions(anyLong());
    }

    @Test
    void login_shouldHandleGenericException_gracefully() {
        // Given
        when(userCommandService.handleLogin(anyString()))
                .thenReturn(Mono.error(new Exception("Generic error")));

        // When & Then
        StepVerifier.create(userController.login(loginRequest))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertFalse(response.success());
                    assertTrue(response.message().contains("登入失敗"));
                    assertTrue(response.message().contains("Generic error"));
                })
                .verifyComplete();

        verify(userCommandService).handleLogin("testuser");
        verify(missionCommandService, never()).initializeMissions(anyLong());
    }

    @Test
    void login_shouldPassCorrectUsername_toUserCommandService() {
        // Given
        String username = "specificUser";
        LoginRequest request = new LoginRequest(username);
        User user = testUser.toBuilder().username(username).build();

        when(userCommandService.handleLogin(username)).thenReturn(Mono.just(user));
        when(missionCommandService.initializeMissions(anyLong())).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(userController.login(request))
                .expectNextCount(1)
                .verifyComplete();

        verify(userCommandService).handleLogin(username);
        verify(userCommandService, never()).handleLogin(argThat(arg -> !arg.equals(username)));
    }

    @Test
    void login_shouldPassCorrectUserId_toMissionCommandService() {
        // Given
        Long expectedUserId = 123L;
        User user = testUser.toBuilder().id(expectedUserId).build();

        when(userCommandService.handleLogin(anyString())).thenReturn(Mono.just(user));
        when(missionCommandService.initializeMissions(expectedUserId)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(userController.login(loginRequest))
                .expectNextCount(1)
                .verifyComplete();

        verify(missionCommandService).initializeMissions(expectedUserId);
        verify(missionCommandService, never()).initializeMissions(argThat(arg -> !arg.equals(expectedUserId)));
    }

    @Test
    void login_shouldReturnSuccessResponse_withCorrectStructure() {
        // Given
        when(userCommandService.handleLogin(anyString())).thenReturn(Mono.just(testUser));
        when(missionCommandService.initializeMissions(anyLong())).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(userController.login(loginRequest))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertInstanceOf(ApiResponse.class, response);
                    assertTrue(response.success());
                    assertEquals("登入成功", response.message());
                    assertNull(response.data());
                })
                .verifyComplete();
    }

    @Test
    void login_shouldReturnErrorResponse_withCorrectStructure() {
        // Given
        String errorMessage = "Connection timeout";
        when(userCommandService.handleLogin(anyString()))
                .thenReturn(Mono.error(new RuntimeException(errorMessage)));

        // When & Then
        StepVerifier.create(userController.login(loginRequest))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertInstanceOf(ApiResponse.class, response);
                    assertFalse(response.success());
                    assertTrue(response.message().contains("登入失敗"));
                    assertTrue(response.message().contains(errorMessage));
                })
                .verifyComplete();
    }
}
