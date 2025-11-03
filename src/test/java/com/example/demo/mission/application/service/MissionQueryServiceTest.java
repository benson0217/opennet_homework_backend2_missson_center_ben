package com.example.demo.mission.application.service;

import com.example.demo.mission.application.service.impl.MissionQueryServiceImpl;
import com.example.demo.mission.domain.model.Mission;
import com.example.demo.mission.domain.model.MissionType;
import com.example.demo.mission.domain.repository.MissionRepository;
import com.example.demo.shared.application.converter.MissionMapper;
import com.example.demo.shared.application.dto.MissionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MissionQueryServiceTest {

    @Mock
    private MissionRepository missionRepository;

    @Mock
    private MissionMapper missionMapper;

    @InjectMocks
    private MissionQueryServiceImpl missionQueryService;

    private Mission consecutiveLoginMission;
    private Mission launchGamesMission;
    private Mission playGamesMission;
    private MissionResponse consecutiveLoginResponse;
    private MissionResponse launchGamesResponse;
    private MissionResponse playGamesResponse;

    @BeforeEach
    void setUp() {
        consecutiveLoginMission = Mission.builder()
                .id(1L)
                .userId(1L)
                .missionType(MissionType.CONSECUTIVE_LOGIN)
                .currentProgress(2)
                .targetProgress(3)
                .isCompleted(false)
                .isRewarded(false)
                .rewardPoints(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        launchGamesMission = Mission.builder()
                .id(2L)
                .userId(1L)
                .missionType(MissionType.LAUNCH_GAMES)
                .currentProgress(3)
                .targetProgress(3)
                .isCompleted(true)
                .completedAt(LocalDateTime.now())
                .isRewarded(false)
                .rewardPoints(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        playGamesMission = Mission.builder()
                .id(3L)
                .userId(1L)
                .missionType(MissionType.PLAY_GAMES)
                .currentProgress(1)
                .targetProgress(3)
                .isCompleted(false)
                .isRewarded(false)
                .rewardPoints(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        consecutiveLoginResponse = new MissionResponse(
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

        launchGamesResponse = new MissionResponse(
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

        playGamesResponse = new MissionResponse(
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
    void getMissionsForUser_shouldReturnMissionList_whenUserHasMissions() {
        // Given
        Long userId = 1L;
        when(missionRepository.findByUserId(userId))
                .thenReturn(Flux.just(consecutiveLoginMission, launchGamesMission, playGamesMission));
        when(missionMapper.toResponse(consecutiveLoginMission)).thenReturn(consecutiveLoginResponse);
        when(missionMapper.toResponse(launchGamesMission)).thenReturn(launchGamesResponse);
        when(missionMapper.toResponse(playGamesMission)).thenReturn(playGamesResponse);

        // When & Then
        StepVerifier.create(missionQueryService.getMissionsForUser(userId))
                .assertNext(missions -> {
                    assertNotNull(missions);
                    assertEquals(3, missions.size());
                    assertEquals("CONSECUTIVE_LOGIN", missions.get(0).missionType());
                    assertEquals("LAUNCH_GAMES", missions.get(1).missionType());
                    assertEquals("PLAY_GAMES", missions.get(2).missionType());
                })
                .verifyComplete();

        verify(missionRepository).findByUserId(userId);
        verify(missionMapper, times(3)).toResponse(any(Mission.class));
    }

    @Test
    void getMissionsForUser_shouldReturnEmptyList_whenUserHasNoMissions() {
        // Given
        Long userId = 1L;
        when(missionRepository.findByUserId(userId)).thenReturn(Flux.empty());

        // When & Then
        StepVerifier.create(missionQueryService.getMissionsForUser(userId))
                .assertNext(missions -> {
                    assertNotNull(missions);
                    assertEquals(0, missions.size());
                    assertTrue(missions.isEmpty());
                })
                .verifyComplete();

        verify(missionRepository).findByUserId(userId);
        verify(missionMapper, never()).toResponse(any(Mission.class));
    }

    @Test
    void getMissionsForUser_shouldPropagateError_whenRepositoryFails() {
        // Given
        Long userId = 1L;
        RuntimeException repositoryError = new RuntimeException("Database error");
        when(missionRepository.findByUserId(userId)).thenReturn(Flux.error(repositoryError));

        // When & Then
        StepVerifier.create(missionQueryService.getMissionsForUser(userId))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("Database error"))
                .verify();

        verify(missionRepository).findByUserId(userId);
        verify(missionMapper, never()).toResponse(any(Mission.class));
    }

    @Test
    void getMissionsForUser_shouldMapAllMissionsCorrectly() {
        // Given
        Long userId = 1L;
        when(missionRepository.findByUserId(userId))
                .thenReturn(Flux.just(consecutiveLoginMission, launchGamesMission, playGamesMission));
        when(missionMapper.toResponse(any(Mission.class))).thenAnswer(invocation -> {
            Mission mission = invocation.getArgument(0);
            return new MissionResponse(
                    mission.getId(),
                    mission.getMissionType().name(),
                    mission.getMissionType().getDescription(),
                    mission.getCurrentProgress(),
                    mission.getTargetProgress(),
                    mission.getProgressPercentage(),
                    mission.getIsCompleted(),
                    mission.getCompletedAt(),
                    mission.getIsRewarded(),
                    mission.getRewardedAt(),
                    mission.getRewardPoints()
            );
        });

        // When & Then
        StepVerifier.create(missionQueryService.getMissionsForUser(userId))
                .assertNext(missions -> {
                    assertNotNull(missions);
                    assertEquals(3, missions.size());

                    // Verify first mission
                    MissionResponse first = missions.getFirst();
                    assertEquals(1L, first.id());
                    assertEquals("CONSECUTIVE_LOGIN", first.missionType());
                    assertEquals(2, first.currentProgress());
                    assertEquals(3, first.targetProgress());
                    assertFalse(first.isCompleted());

                    // Verify second mission
                    MissionResponse second = missions.get(1);
                    assertEquals(2L, second.id());
                    assertEquals("LAUNCH_GAMES", second.missionType());
                    assertEquals(3, second.currentProgress());
                    assertEquals(3, second.targetProgress());
                    assertTrue(second.isCompleted());

                    // Verify third mission
                    MissionResponse third = missions.get(2);
                    assertEquals(3L, third.id());
                    assertEquals("PLAY_GAMES", third.missionType());
                    assertEquals(1, third.currentProgress());
                    assertEquals(3, third.targetProgress());
                    assertFalse(third.isCompleted());
                })
                .verifyComplete();

        verify(missionRepository).findByUserId(userId);
        verify(missionMapper, times(3)).toResponse(any(Mission.class));
    }

    @Test
    void getMissionsForUser_shouldHandleSingleMission() {
        // Given
        Long userId = 1L;
        when(missionRepository.findByUserId(userId)).thenReturn(Flux.just(consecutiveLoginMission));
        when(missionMapper.toResponse(consecutiveLoginMission)).thenReturn(consecutiveLoginResponse);

        // When & Then
        StepVerifier.create(missionQueryService.getMissionsForUser(userId))
                .assertNext(missions -> {
                    assertNotNull(missions);
                    assertEquals(1, missions.size());
                    assertEquals("CONSECUTIVE_LOGIN", missions.getFirst().missionType());
                })
                .verifyComplete();

        verify(missionRepository).findByUserId(userId);
        verify(missionMapper).toResponse(consecutiveLoginMission);
    }

    @Test
    void getMissionsForUser_shouldHandleCompletedMissions() {
        // Given
        Long userId = 1L;
        Mission completedMission = consecutiveLoginMission.toBuilder()
                .isCompleted(true)
                .completedAt(LocalDateTime.now())
                .build();

        MissionResponse completedResponse = new MissionResponse(
                1L,
                "CONSECUTIVE_LOGIN",
                "連續登入",
                3,
                3,
                100.0,
                true,
                LocalDateTime.now(),
                false,
                null,
                0
        );

        when(missionRepository.findByUserId(userId)).thenReturn(Flux.just(completedMission));
        when(missionMapper.toResponse(completedMission)).thenReturn(completedResponse);

        // When & Then
        StepVerifier.create(missionQueryService.getMissionsForUser(userId))
                .assertNext(missions -> {
                    assertNotNull(missions);
                    assertEquals(1, missions.size());
                    assertTrue(missions.getFirst().isCompleted());
                    assertNotNull(missions.getFirst().completedAt());
                })
                .verifyComplete();

        verify(missionRepository).findByUserId(userId);
        verify(missionMapper).toResponse(completedMission);
    }

    @Test
    void getMissionsForUser_shouldHandleRewardedMissions() {
        // Given
        Long userId = 1L;
        Mission rewardedMission = launchGamesMission.toBuilder()
                .isRewarded(true)
                .rewardedAt(LocalDateTime.now())
                .rewardPoints(100)
                .build();

        MissionResponse rewardedResponse = new MissionResponse(
                2L,
                "LAUNCH_GAMES",
                "遊戲啟動",
                3,
                3,
                100.0,
                true,
                LocalDateTime.now(),
                true,
                LocalDateTime.now(),
                100
        );

        when(missionRepository.findByUserId(userId)).thenReturn(Flux.just(rewardedMission));
        when(missionMapper.toResponse(rewardedMission)).thenReturn(rewardedResponse);

        // When & Then
        StepVerifier.create(missionQueryService.getMissionsForUser(userId))
                .assertNext(missions -> {
                    assertNotNull(missions);
                    assertEquals(1, missions.size());
                    assertTrue(missions.getFirst().isRewarded());
                    assertNotNull(missions.getFirst().rewardedAt());
                    assertEquals(100, missions.getFirst().rewardPoints());
                })
                .verifyComplete();

        verify(missionRepository).findByUserId(userId);
        verify(missionMapper).toResponse(rewardedMission);
    }

    @Test
    void getMissionsForUser_shouldPropagateError_whenMapperFails() {
        // Given
        Long userId = 1L;
        RuntimeException mapperError = new RuntimeException("Mapping failed");
        when(missionRepository.findByUserId(userId)).thenReturn(Flux.just(consecutiveLoginMission));
        when(missionMapper.toResponse(consecutiveLoginMission)).thenThrow(mapperError);

        // When & Then
        StepVerifier.create(missionQueryService.getMissionsForUser(userId))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("Mapping failed"))
                .verify();

        verify(missionRepository).findByUserId(userId);
        verify(missionMapper).toResponse(consecutiveLoginMission);
    }

    @Test
    void getMissionsForUser_shouldReturnListInCorrectOrder() {
        // Given
        Long userId = 1L;
        when(missionRepository.findByUserId(userId))
                .thenReturn(Flux.just(playGamesMission, consecutiveLoginMission, launchGamesMission));
        when(missionMapper.toResponse(playGamesMission)).thenReturn(playGamesResponse);
        when(missionMapper.toResponse(consecutiveLoginMission)).thenReturn(consecutiveLoginResponse);
        when(missionMapper.toResponse(launchGamesMission)).thenReturn(launchGamesResponse);

        // When & Then
        StepVerifier.create(missionQueryService.getMissionsForUser(userId))
                .assertNext(missions -> {
                    assertNotNull(missions);
                    assertEquals(3, missions.size());
                    // Verify order is preserved from Flux
                    assertEquals("PLAY_GAMES", missions.get(0).missionType());
                    assertEquals("CONSECUTIVE_LOGIN", missions.get(1).missionType());
                    assertEquals("LAUNCH_GAMES", missions.get(2).missionType());
                })
                .verifyComplete();

        verify(missionRepository).findByUserId(userId);
        verify(missionMapper, times(3)).toResponse(any(Mission.class));
    }
}
