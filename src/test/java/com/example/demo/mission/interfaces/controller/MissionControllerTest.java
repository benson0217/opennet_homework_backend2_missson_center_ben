package com.example.demo.mission.interfaces.controller;

import com.example.demo.mission.application.service.MissionQueryService;
import com.example.demo.shared.application.dto.MissionResponse;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MissionControllerTest {

    @Mock
    private UserQueryService userQueryService;

    @Mock
    private MissionQueryService missionQueryService;

    @InjectMocks
    private MissionController missionController;

    private User testUser;
    private MissionResponse mission1;
    private MissionResponse mission2;
    private MissionResponse mission3;

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

        mission1 = new MissionResponse(
                1L,
                "CONSECUTIVE_LOGIN",
                "連續登入",
                2,
                3,
                66.67,
                false,
                null,
                false,
                null,
                0
        );

        mission2 = new MissionResponse(
                2L,
                "LAUNCH_GAMES",
                "遊戲啟動",
                3,
                3,
                100.0,
                true,
                LocalDateTime.now(),
                false,
                null,
                0
        );

        mission3 = new MissionResponse(
                3L,
                "PLAY_GAMES",
                "遊戲遊玩",
                1,
                3,
                33.33,
                false,
                null,
                false,
                null,
                0
        );
    }

    @Test
    void getMissions_shouldReturnMissionList_whenUserExists() {
        // Given
        String username = "testuser";
        List<MissionResponse> missions = Arrays.asList(mission1, mission2, mission3);
        when(userQueryService.getUserByUsername(username)).thenReturn(Mono.just(testUser));
        when(missionQueryService.getMissionsForUser(1L)).thenReturn(Mono.just(missions));

        // When & Then
        StepVerifier.create(missionController.getMissions(username))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertTrue(response.success());
                    assertEquals("任務取得成功", response.message());
                    assertNotNull(response.data());
                    assertEquals(3, response.data().size());
                    assertEquals("CONSECUTIVE_LOGIN", response.data().get(0).missionType());
                    assertEquals("LAUNCH_GAMES", response.data().get(1).missionType());
                    assertEquals("PLAY_GAMES", response.data().get(2).missionType());
                })
                .verifyComplete();

        verify(userQueryService).getUserByUsername(username);
        verify(missionQueryService).getMissionsForUser(1L);
    }

    @Test
    void getMissions_shouldReturnEmptyList_whenUserHasNoMissions() {
        // Given
        String username = "testuser";
        List<MissionResponse> emptyMissions = Collections.emptyList();
        when(userQueryService.getUserByUsername(username)).thenReturn(Mono.just(testUser));
        when(missionQueryService.getMissionsForUser(1L)).thenReturn(Mono.just(emptyMissions));

        // When & Then
        StepVerifier.create(missionController.getMissions(username))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertTrue(response.success());
                    assertEquals("任務取得成功", response.message());
                    assertNotNull(response.data());
                    assertTrue(response.data().isEmpty());
                })
                .verifyComplete();

        verify(userQueryService).getUserByUsername(username);
        verify(missionQueryService).getMissionsForUser(1L);
    }

    @Test
    void getMissions_shouldReturnError_whenUserNotFound() {
        // Given
        String username = "nonexistent";
        when(userQueryService.getUserByUsername(username)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(missionController.getMissions(username))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertFalse(response.success());
                    assertTrue(response.message().contains("取得任務失敗"));
                    assertTrue(response.message().contains("找不到使用者"));
                })
                .verifyComplete();

        verify(userQueryService).getUserByUsername(username);
        verify(missionQueryService, never()).getMissionsForUser(anyLong());
    }

    @Test
    void getMissions_shouldReturnError_whenUserQueryServiceFails() {
        // Given
        String username = "testuser";
        RuntimeException queryError = new RuntimeException("User query failed");
        when(userQueryService.getUserByUsername(username)).thenReturn(Mono.error(queryError));

        // When & Then
        StepVerifier.create(missionController.getMissions(username))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertFalse(response.success());
                    assertTrue(response.message().contains("取得任務失敗"));
                    assertTrue(response.message().contains("User query failed"));
                })
                .verifyComplete();

        verify(userQueryService).getUserByUsername(username);
        verify(missionQueryService, never()).getMissionsForUser(anyLong());
    }

    @Test
    void getMissions_shouldReturnError_whenMissionQueryServiceFails() {
        // Given
        String username = "testuser";
        RuntimeException missionError = new RuntimeException("Mission query failed");
        when(userQueryService.getUserByUsername(username)).thenReturn(Mono.just(testUser));
        when(missionQueryService.getMissionsForUser(1L)).thenReturn(Mono.error(missionError));

        // When & Then
        StepVerifier.create(missionController.getMissions(username))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertFalse(response.success());
                    assertTrue(response.message().contains("取得任務失敗"));
                    assertTrue(response.message().contains("Mission query failed"));
                })
                .verifyComplete();

        verify(userQueryService).getUserByUsername(username);
        verify(missionQueryService).getMissionsForUser(1L);
    }

    @Test
    void getMissions_shouldCallServicesWithCorrectParameters() {
        // Given
        String username = "testuser";
        List<MissionResponse> missions = Arrays.asList(mission1, mission2);
        when(userQueryService.getUserByUsername(username)).thenReturn(Mono.just(testUser));
        when(missionQueryService.getMissionsForUser(1L)).thenReturn(Mono.just(missions));

        // When
        StepVerifier.create(missionController.getMissions(username))
                .expectNextCount(1)
                .verifyComplete();

        // Then
        verify(userQueryService).getUserByUsername("testuser");
        verify(missionQueryService).getMissionsForUser(1L);
    }

    @Test
    void getMissions_shouldReturnCorrectResponseStructure() {
        // Given
        String username = "testuser";
        List<MissionResponse> missions = Collections.singletonList(mission1);
        when(userQueryService.getUserByUsername(username)).thenReturn(Mono.just(testUser));
        when(missionQueryService.getMissionsForUser(1L)).thenReturn(Mono.just(missions));

        // When & Then
        StepVerifier.create(missionController.getMissions(username))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertNotNull(response.message());
                    assertNotNull(response.data());
                    assertTrue(response.success());
                })
                .verifyComplete();
    }

    @Test
    void getMissions_shouldReturnSingleMission_whenUserHasOneMission() {
        // Given
        String username = "testuser";
        List<MissionResponse> missions = Collections.singletonList(mission1);
        when(userQueryService.getUserByUsername(username)).thenReturn(Mono.just(testUser));
        when(missionQueryService.getMissionsForUser(1L)).thenReturn(Mono.just(missions));

        // When & Then
        StepVerifier.create(missionController.getMissions(username))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertTrue(response.success());
                    assertEquals(1, response.data().size());
                    assertEquals("CONSECUTIVE_LOGIN", response.data().getFirst().missionType());
                })
                .verifyComplete();

        verify(userQueryService).getUserByUsername(username);
        verify(missionQueryService).getMissionsForUser(1L);
    }

    @Test
    void getMissions_shouldReturnCompletedMissions() {
        // Given
        String username = "testuser";
        List<MissionResponse> missions = Collections.singletonList(mission2);
        when(userQueryService.getUserByUsername(username)).thenReturn(Mono.just(testUser));
        when(missionQueryService.getMissionsForUser(1L)).thenReturn(Mono.just(missions));

        // When & Then
        StepVerifier.create(missionController.getMissions(username))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertTrue(response.success());
                    assertEquals(1, response.data().size());
                    assertTrue(response.data().getFirst().isCompleted());
                    assertNotNull(response.data().getFirst().completedAt());
                })
                .verifyComplete();

        verify(userQueryService).getUserByUsername(username);
        verify(missionQueryService).getMissionsForUser(1L);
    }

    @Test
    void getMissions_shouldReturnInProgressMissions() {
        // Given
        String username = "testuser";
        List<MissionResponse> missions = Arrays.asList(mission1, mission3);
        when(userQueryService.getUserByUsername(username)).thenReturn(Mono.just(testUser));
        when(missionQueryService.getMissionsForUser(1L)).thenReturn(Mono.just(missions));

        // When & Then
        StepVerifier.create(missionController.getMissions(username))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertTrue(response.success());
                    assertEquals(2, response.data().size());
                    assertFalse(response.data().get(0).isCompleted());
                    assertFalse(response.data().get(1).isCompleted());
                })
                .verifyComplete();

        verify(userQueryService).getUserByUsername(username);
        verify(missionQueryService).getMissionsForUser(1L);
    }

    @Test
    void getMissions_shouldReturnMissionsWithCorrectProgress() {
        // Given
        String username = "testuser";
        List<MissionResponse> missions = Arrays.asList(mission1, mission2, mission3);
        when(userQueryService.getUserByUsername(username)).thenReturn(Mono.just(testUser));
        when(missionQueryService.getMissionsForUser(1L)).thenReturn(Mono.just(missions));

        // When & Then
        StepVerifier.create(missionController.getMissions(username))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertTrue(response.success());
                    assertEquals(3, response.data().size());
                    
                    // Check progress values
                    assertEquals(2, response.data().getFirst().currentProgress());
                    assertEquals(3, response.data().get(0).targetProgress());
                    assertEquals(66.67, response.data().get(0).progressPercentage(), 0.01);
                    
                    assertEquals(3, response.data().get(1).currentProgress());
                    assertEquals(3, response.data().get(1).targetProgress());
                    assertEquals(100.0, response.data().get(1).progressPercentage(), 0.01);
                    
                    assertEquals(1, response.data().get(2).currentProgress());
                    assertEquals(3, response.data().get(2).targetProgress());
                    assertEquals(33.33, response.data().get(2).progressPercentage(), 0.01);
                })
                .verifyComplete();

        verify(userQueryService).getUserByUsername(username);
        verify(missionQueryService).getMissionsForUser(1L);
    }

    @Test
    void getMissions_shouldHandleIllegalArgumentException() {
        // Given
        String username = "invaliduser";
        when(userQueryService.getUserByUsername(username))
                .thenReturn(Mono.error(new IllegalArgumentException("Invalid username")));

        // When & Then
        StepVerifier.create(missionController.getMissions(username))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertFalse(response.success());
                    assertTrue(response.message().contains("Invalid username"));
                })
                .verifyComplete();

        verify(userQueryService).getUserByUsername(username);
        verify(missionQueryService, never()).getMissionsForUser(anyLong());
    }

    @Test
    void getMissions_shouldReturnMissionsInCorrectOrder() {
        // Given
        String username = "testuser";
        List<MissionResponse> missions = Arrays.asList(mission3, mission1, mission2);
        when(userQueryService.getUserByUsername(username)).thenReturn(Mono.just(testUser));
        when(missionQueryService.getMissionsForUser(1L)).thenReturn(Mono.just(missions));

        // When & Then
        StepVerifier.create(missionController.getMissions(username))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertTrue(response.success());
                    assertEquals(3, response.data().size());
                    // Verify order is preserved
                    assertEquals("PLAY_GAMES", response.data().get(0).missionType());
                    assertEquals("CONSECUTIVE_LOGIN", response.data().get(1).missionType());
                    assertEquals("LAUNCH_GAMES", response.data().get(2).missionType());
                })
                .verifyComplete();

        verify(userQueryService).getUserByUsername(username);
        verify(missionQueryService).getMissionsForUser(1L);
    }

    @Test
    void getMissions_shouldReturnMissionsWithAllFields() {
        // Given
        String username = "testuser";
        MissionResponse rewardedMission = new MissionResponse(
                4L,
                "CONSECUTIVE_LOGIN",
                "連續登入",
                3,
                3,
                100.0,
                true,
                LocalDateTime.now(),
                true,
                LocalDateTime.now(),
                100
        );
        List<MissionResponse> missions = Collections.singletonList(rewardedMission);
        when(userQueryService.getUserByUsername(username)).thenReturn(Mono.just(testUser));
        when(missionQueryService.getMissionsForUser(1L)).thenReturn(Mono.just(missions));

        // When & Then
        StepVerifier.create(missionController.getMissions(username))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertTrue(response.success());
                    assertEquals(1, response.data().size());
                    
                    MissionResponse mission = response.data().getFirst();
                    assertEquals(4L, mission.id());
                    assertEquals("CONSECUTIVE_LOGIN", mission.missionType());
                    assertEquals("連續登入", mission.description());
                    assertTrue(mission.isCompleted());
                    assertTrue(mission.isRewarded());
                    assertNotNull(mission.completedAt());
                    assertNotNull(mission.rewardedAt());
                    assertEquals(100, mission.rewardPoints());
                })
                .verifyComplete();

        verify(userQueryService).getUserByUsername(username);
        verify(missionQueryService).getMissionsForUser(1L);
    }
}
