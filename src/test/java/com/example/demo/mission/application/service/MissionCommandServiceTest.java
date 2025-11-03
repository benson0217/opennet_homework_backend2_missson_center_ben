package com.example.demo.mission.application.service;

import com.example.demo.game.domain.repository.GameLaunchRecordRepository;
import com.example.demo.game.domain.repository.GamePlayRecordRepository;
import com.example.demo.mission.application.service.impl.MissionCommandServiceImpl;
import com.example.demo.mission.domain.model.Mission;
import com.example.demo.mission.domain.model.MissionType;
import com.example.demo.mission.domain.repository.MissionRepository;
import com.example.demo.shared.application.dto.event.MissionCompletedEvent;
import com.example.demo.shared.infrastructure.message.EventPublisher;
import com.example.demo.user.application.service.UserQueryService;
import com.example.demo.user.domain.model.User;
import com.example.demo.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MissionCommandServiceTest {

    @Mock
    private MissionRepository missionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GameLaunchRecordRepository gameLaunchRecordRepository;

    @Mock
    private GamePlayRecordRepository gamePlayRecordRepository;

    @Mock
    private UserQueryService userQueryService;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private MissionCommandServiceImpl missionCommandService;

    private User testUser;
    private Mission consecutiveLoginMission;
    private Mission launchGamesMission;
    private Mission playGamesMission;

    @BeforeEach
    void setUp() {
        // Set configuration values
        ReflectionTestUtils.setField(missionCommandService, "consecutiveLoginDays", 3);
        ReflectionTestUtils.setField(missionCommandService, "launchGamesCount", 3);
        ReflectionTestUtils.setField(missionCommandService, "playGamesCount", 3);
        ReflectionTestUtils.setField(missionCommandService, "playGamesMinScore", 1000);
        ReflectionTestUtils.setField(missionCommandService, "completionRewardPoints", 777);

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .points(100)
                .registrationDate(LocalDateTime.now().minusDays(5))
                .createdAt(LocalDateTime.now().minusDays(5))
                .updatedAt(LocalDateTime.now().minusDays(5))
                .build();

        consecutiveLoginMission = Mission.builder()
                .id(1L)
                .userId(1L)
                .missionType(MissionType.CONSECUTIVE_LOGIN)
                .currentProgress(0)
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
                .currentProgress(0)
                .targetProgress(3)
                .isCompleted(false)
                .isRewarded(false)
                .rewardPoints(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        playGamesMission = Mission.builder()
                .id(3L)
                .userId(1L)
                .missionType(MissionType.PLAY_GAMES)
                .currentProgress(0)
                .targetProgress(3)
                .isCompleted(false)
                .isRewarded(false)
                .rewardPoints(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void initializeMissions_shouldCreateMissions_whenUserHasNoMissions() {
        // Given
        Long userId = 1L;
        when(missionRepository.existsByUserIdAndMissionType(userId, MissionType.CONSECUTIVE_LOGIN))
                .thenReturn(Mono.just(false));
        when(missionRepository.save(any(Mission.class)))
                .thenReturn(Mono.just(consecutiveLoginMission));

        // When & Then
        StepVerifier.create(missionCommandService.initializeMissions(userId))
                .verifyComplete();

        verify(missionRepository).existsByUserIdAndMissionType(userId, MissionType.CONSECUTIVE_LOGIN);
        verify(missionRepository, times(3)).save(any(Mission.class));
    }

    @Test
    void initializeMissions_shouldNotCreateMissions_whenUserAlreadyHasMissions() {
        // Given
        Long userId = 1L;
        when(missionRepository.existsByUserIdAndMissionType(userId, MissionType.CONSECUTIVE_LOGIN))
                .thenReturn(Mono.just(true));

        // When & Then
        StepVerifier.create(missionCommandService.initializeMissions(userId))
                .verifyComplete();

        verify(missionRepository).existsByUserIdAndMissionType(userId, MissionType.CONSECUTIVE_LOGIN);
        verify(missionRepository, never()).save(any(Mission.class));
    }

    @Test
    void updateMissionProgress_shouldUpdateAllMissions_successfully() {
        // Given
        Long userId = 1L;
        String userName = "testuser";

        when(missionRepository.findByUserIdAndMissionType(userId, MissionType.CONSECUTIVE_LOGIN))
                .thenReturn(Mono.just(consecutiveLoginMission));
        when(missionRepository.findByUserIdAndMissionType(userId, MissionType.LAUNCH_GAMES))
                .thenReturn(Mono.just(launchGamesMission));
        when(missionRepository.findByUserIdAndMissionType(userId, MissionType.PLAY_GAMES))
                .thenReturn(Mono.just(playGamesMission));

        when(userQueryService.getConsecutiveLoginDays(userId)).thenReturn(Mono.just(2));
        when(gameLaunchRecordRepository.countDistinctGamesLaunchedByUser(userId)).thenReturn(Mono.just(2L));
        when(gamePlayRecordRepository.countByUserId(userId)).thenReturn(Mono.just(2L));
        when(gamePlayRecordRepository.sumScoreByUserId(userId)).thenReturn(Mono.just(800));

        when(missionRepository.save(any(Mission.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(missionRepository.areAllMissionsCompleted(userId)).thenReturn(Mono.just(false));

        // When & Then
        StepVerifier.create(missionCommandService.updateMissionProgress(userId, userName))
                .verifyComplete();

        verify(userQueryService).getConsecutiveLoginDays(userId);
        verify(gameLaunchRecordRepository).countDistinctGamesLaunchedByUser(userId);
        verify(gamePlayRecordRepository).countByUserId(userId);
        verify(gamePlayRecordRepository).sumScoreByUserId(userId);
        verify(missionRepository, times(3)).save(any(Mission.class));
    }

    @Test
    void updateConsecutiveLoginMission_shouldCompleteMission_whenTargetReached() {
        // Given
        Long userId = 1L;
        when(missionRepository.findByUserIdAndMissionType(userId, MissionType.CONSECUTIVE_LOGIN))
                .thenReturn(Mono.just(consecutiveLoginMission));
        when(missionRepository.findByUserIdAndMissionType(userId, MissionType.LAUNCH_GAMES))
                .thenReturn(Mono.just(launchGamesMission));
        when(missionRepository.findByUserIdAndMissionType(userId, MissionType.PLAY_GAMES))
                .thenReturn(Mono.just(playGamesMission));
        when(userQueryService.getConsecutiveLoginDays(userId)).thenReturn(Mono.just(3));
        when(gameLaunchRecordRepository.countDistinctGamesLaunchedByUser(userId)).thenReturn(Mono.just(2L));
        when(gamePlayRecordRepository.countByUserId(userId)).thenReturn(Mono.just(2L));
        when(gamePlayRecordRepository.sumScoreByUserId(userId)).thenReturn(Mono.just(800));
        when(missionRepository.save(any(Mission.class))).thenAnswer(invocation -> {
            Mission saved = invocation.getArgument(0);
            return Mono.just(saved);
        });
        when(userRepository.findById(userId)).thenReturn(Mono.just(testUser));
        when(eventPublisher.publishMissionCompletedEvent(any(MissionCompletedEvent.class)))
                .thenReturn(Mono.empty());
        when(missionRepository.areAllMissionsCompleted(userId)).thenReturn(Mono.just(false));

        // When & Then
        StepVerifier.create(missionCommandService.updateMissionProgress(userId, "testuser"))
                .verifyComplete();

        verify(eventPublisher).publishMissionCompletedEvent(any(MissionCompletedEvent.class));
    }

    @Test
    void updateLaunchGamesMission_shouldCompleteMission_whenTargetReached() {
        // Given
        Long userId = 1L;
        when(missionRepository.findByUserIdAndMissionType(userId, MissionType.CONSECUTIVE_LOGIN))
                .thenReturn(Mono.just(consecutiveLoginMission));
        when(missionRepository.findByUserIdAndMissionType(userId, MissionType.LAUNCH_GAMES))
                .thenReturn(Mono.just(launchGamesMission));
        when(missionRepository.findByUserIdAndMissionType(userId, MissionType.PLAY_GAMES))
                .thenReturn(Mono.just(playGamesMission));
        when(userQueryService.getConsecutiveLoginDays(userId)).thenReturn(Mono.just(2));
        when(gameLaunchRecordRepository.countDistinctGamesLaunchedByUser(userId)).thenReturn(Mono.just(3L));
        when(gamePlayRecordRepository.countByUserId(userId)).thenReturn(Mono.just(2L));
        when(gamePlayRecordRepository.sumScoreByUserId(userId)).thenReturn(Mono.just(800));
        when(missionRepository.save(any(Mission.class))).thenAnswer(invocation -> {
            Mission saved = invocation.getArgument(0);
            return Mono.just(saved);
        });
        when(userRepository.findById(userId)).thenReturn(Mono.just(testUser));
        when(eventPublisher.publishMissionCompletedEvent(any(MissionCompletedEvent.class)))
                .thenReturn(Mono.empty());
        when(missionRepository.areAllMissionsCompleted(userId)).thenReturn(Mono.just(false));

        // When & Then
        StepVerifier.create(missionCommandService.updateMissionProgress(userId, "testuser"))
                .verifyComplete();

        verify(eventPublisher).publishMissionCompletedEvent(any(MissionCompletedEvent.class));
    }

    @Test
    void updatePlayGamesMission_shouldCompleteMission_whenBothConditionsMet() {
        // Given
        Long userId = 1L;
        when(missionRepository.findByUserIdAndMissionType(userId, MissionType.CONSECUTIVE_LOGIN))
                .thenReturn(Mono.just(consecutiveLoginMission));
        when(missionRepository.findByUserIdAndMissionType(userId, MissionType.LAUNCH_GAMES))
                .thenReturn(Mono.just(launchGamesMission));
        when(missionRepository.findByUserIdAndMissionType(userId, MissionType.PLAY_GAMES))
                .thenReturn(Mono.just(playGamesMission));
        when(userQueryService.getConsecutiveLoginDays(userId)).thenReturn(Mono.just(2));
        when(gameLaunchRecordRepository.countDistinctGamesLaunchedByUser(userId)).thenReturn(Mono.just(2L));
        when(gamePlayRecordRepository.countByUserId(userId)).thenReturn(Mono.just(3L));
        when(gamePlayRecordRepository.sumScoreByUserId(userId)).thenReturn(Mono.just(1500));
        when(missionRepository.save(any(Mission.class))).thenAnswer(invocation -> {
            Mission saved = invocation.getArgument(0);
            return Mono.just(saved);
        });
        when(userRepository.findById(userId)).thenReturn(Mono.just(testUser));
        when(eventPublisher.publishMissionCompletedEvent(any(MissionCompletedEvent.class)))
                .thenReturn(Mono.empty());
        when(missionRepository.areAllMissionsCompleted(userId)).thenReturn(Mono.just(false));

        // When & Then
        StepVerifier.create(missionCommandService.updateMissionProgress(userId, "testuser"))
                .verifyComplete();

        verify(eventPublisher).publishMissionCompletedEvent(any(MissionCompletedEvent.class));
    }

    @Test
    void calculatePlayGameProgress_shouldReturnFullProgress_whenBothConditionsMet() {
        // When
        int progress = ReflectionTestUtils.invokeMethod(missionCommandService, 
                "calculatePlayGameProgress", 3L, 1500);

        // Then
        assertEquals(3, progress);
    }

    @Test
    void calculatePlayGameProgress_shouldReturnPartialProgress_whenOnlyCountMet() {
        // When
        int progress = ReflectionTestUtils.invokeMethod(missionCommandService, 
                "calculatePlayGameProgress", 3L, 800);

        // Then
        assertEquals(2, progress);
    }

    @Test
    void calculatePlayGameProgress_shouldReturnCountProgress_whenNeitherMet() {
        // When
        int progress = ReflectionTestUtils.invokeMethod(missionCommandService, 
                "calculatePlayGameProgress", 2L, 500);

        // Then
        assertEquals(2, progress);
    }

    @Test
    void checkAndDistributeRewards_shouldDistributeRewards_whenAllMissionsCompleted() {
        // Given
        Long userId = 1L;
        String userName = "testuser";

        Mission completedMission1 = consecutiveLoginMission.toBuilder()
                .isCompleted(true)
                .completedAt(LocalDateTime.now())
                .build();
        Mission completedMission2 = launchGamesMission.toBuilder()
                .isCompleted(true)
                .completedAt(LocalDateTime.now())
                .build();
        Mission completedMission3 = playGamesMission.toBuilder()
                .isCompleted(true)
                .completedAt(LocalDateTime.now())
                .build();

        when(missionRepository.areAllMissionsCompleted(userId)).thenReturn(Mono.just(true));
        when(userRepository.addPoints(userId, 777)).thenReturn(Mono.empty());
        when(missionRepository.findByUserId(userId))
                .thenReturn(Flux.just(completedMission1, completedMission2, completedMission3));
        when(missionRepository.save(any(Mission.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // When & Then
        StepVerifier.create(missionCommandService.checkAndDistributeRewards(userId, userName))
                .verifyComplete();

        verify(userRepository).addPoints(userId, 777);
        verify(missionRepository, times(3)).save(any(Mission.class));
    }

    @Test
    void checkAndDistributeRewards_shouldNotDistributeRewards_whenNotAllMissionsCompleted() {
        // Given
        Long userId = 1L;
        String userName = "testuser";
        when(missionRepository.areAllMissionsCompleted(userId)).thenReturn(Mono.just(false));

        // When & Then
        StepVerifier.create(missionCommandService.checkAndDistributeRewards(userId, userName))
                .verifyComplete();

        verify(userRepository, never()).addPoints(anyLong(), anyInt());
        verify(missionRepository, never()).save(any(Mission.class));
    }

    @Test
    void publishMissionCompletedEvent_shouldPublishEvent_successfully() {
        // Given
        Long userId = 1L;

        when(userRepository.findById(userId)).thenReturn(Mono.just(testUser));
        when(eventPublisher.publishMissionCompletedEvent(any(MissionCompletedEvent.class)))
                .thenReturn(Mono.empty());

        // When & Then - Testing through updateMissionProgress which calls publishMissionCompletedEvent
        when(missionRepository.findByUserIdAndMissionType(userId, MissionType.CONSECUTIVE_LOGIN))
                .thenReturn(Mono.just(consecutiveLoginMission));
        when(missionRepository.findByUserIdAndMissionType(userId, MissionType.LAUNCH_GAMES))
                .thenReturn(Mono.just(launchGamesMission));
        when(missionRepository.findByUserIdAndMissionType(userId, MissionType.PLAY_GAMES))
                .thenReturn(Mono.just(playGamesMission));
        when(userQueryService.getConsecutiveLoginDays(userId)).thenReturn(Mono.just(3));
        when(gameLaunchRecordRepository.countDistinctGamesLaunchedByUser(userId)).thenReturn(Mono.just(2L));
        when(gamePlayRecordRepository.countByUserId(userId)).thenReturn(Mono.just(2L));
        when(gamePlayRecordRepository.sumScoreByUserId(userId)).thenReturn(Mono.just(800));
        when(missionRepository.save(any(Mission.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(missionRepository.areAllMissionsCompleted(userId)).thenReturn(Mono.just(false));

        StepVerifier.create(missionCommandService.updateMissionProgress(userId, "testuser"))
                .verifyComplete();

        ArgumentCaptor<MissionCompletedEvent> eventCaptor = ArgumentCaptor.forClass(MissionCompletedEvent.class);
        verify(eventPublisher).publishMissionCompletedEvent(eventCaptor.capture());

        MissionCompletedEvent event = eventCaptor.getValue();
        assertEquals(userId, event.userId());
        assertEquals("testuser", event.username());
        assertNotNull(event.completedAt());
    }

    @Test
    void publishMissionCompletedEvent_shouldHandleError_gracefully() {
        // Given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Mono.just(testUser));
        when(eventPublisher.publishMissionCompletedEvent(any(MissionCompletedEvent.class)))
                .thenReturn(Mono.error(new RuntimeException("Event publishing failed")));

        when(missionRepository.findByUserIdAndMissionType(userId, MissionType.CONSECUTIVE_LOGIN))
                .thenReturn(Mono.just(consecutiveLoginMission));
        when(missionRepository.findByUserIdAndMissionType(userId, MissionType.LAUNCH_GAMES))
                .thenReturn(Mono.just(launchGamesMission));
        when(missionRepository.findByUserIdAndMissionType(userId, MissionType.PLAY_GAMES))
                .thenReturn(Mono.just(playGamesMission));
        when(userQueryService.getConsecutiveLoginDays(userId)).thenReturn(Mono.just(3));
        when(gameLaunchRecordRepository.countDistinctGamesLaunchedByUser(userId)).thenReturn(Mono.just(2L));
        when(gamePlayRecordRepository.countByUserId(userId)).thenReturn(Mono.just(2L));
        when(gamePlayRecordRepository.sumScoreByUserId(userId)).thenReturn(Mono.just(800));
        when(missionRepository.save(any(Mission.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(missionRepository.areAllMissionsCompleted(userId)).thenReturn(Mono.just(false));

        // When & Then - Should complete successfully despite event error (onErrorResume)
        StepVerifier.create(missionCommandService.updateMissionProgress(userId, "testuser"))
                .verifyComplete();

        verify(eventPublisher).publishMissionCompletedEvent(any(MissionCompletedEvent.class));
    }

    @Test
    void updateMissionProgress_shouldPropagateError_whenRepositoryFails() {
        // Given
        Long userId = 1L;
        String userName = "testuser";
        RuntimeException repositoryError = new RuntimeException("Database error");

        when(missionRepository.findByUserIdAndMissionType(userId, MissionType.CONSECUTIVE_LOGIN))
                .thenReturn(Mono.error(repositoryError));
        when(missionRepository.findByUserIdAndMissionType(userId, MissionType.LAUNCH_GAMES))
                .thenReturn(Mono.just(launchGamesMission));
        when(missionRepository.findByUserIdAndMissionType(userId, MissionType.PLAY_GAMES))
                .thenReturn(Mono.just(playGamesMission));
        when(userQueryService.getConsecutiveLoginDays(userId)).thenReturn(Mono.just(3));
        when(gameLaunchRecordRepository.countDistinctGamesLaunchedByUser(userId)).thenReturn(Mono.just(2L));
        when(gamePlayRecordRepository.countByUserId(userId)).thenReturn(Mono.just(2L));
        when(gamePlayRecordRepository.sumScoreByUserId(userId)).thenReturn(Mono.just(800));

        // When & Then
        StepVerifier.create(missionCommandService.updateMissionProgress(userId, userName))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("Database error"))
                .verify();

        verify(missionRepository).findByUserIdAndMissionType(userId, MissionType.CONSECUTIVE_LOGIN);
    }

    @Test
    void checkAndDistributeRewards_shouldPropagateError_whenAddPointsFails() {
        // Given
        Long userId = 1L;
        String userName = "testuser";
        RuntimeException addPointsError = new RuntimeException("Failed to add points");

        when(missionRepository.areAllMissionsCompleted(userId)).thenReturn(Mono.just(true));
        when(userRepository.addPoints(userId, 777)).thenReturn(Mono.error(addPointsError));
        when(missionRepository.findByUserId(userId)).thenReturn(Flux.empty());

        // When & Then
        StepVerifier.create(missionCommandService.checkAndDistributeRewards(userId, userName))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("Failed to add points"))
                .verify();

        verify(userRepository).addPoints(userId, 777);
    }

    @Test
    void initializeMissions_shouldCreateAllThreeMissionTypes() {
        // Given
        Long userId = 1L;
        when(missionRepository.existsByUserIdAndMissionType(userId, MissionType.CONSECUTIVE_LOGIN))
                .thenReturn(Mono.just(false));
        when(missionRepository.save(any(Mission.class)))
                .thenReturn(Mono.just(consecutiveLoginMission));

        // When
        StepVerifier.create(missionCommandService.initializeMissions(userId))
                .verifyComplete();

        // Then
        ArgumentCaptor<Mission> missionCaptor = ArgumentCaptor.forClass(Mission.class);
        verify(missionRepository, times(3)).save(missionCaptor.capture());

        var savedMissions = missionCaptor.getAllValues();
        assertEquals(3, savedMissions.size());
        assertTrue(savedMissions.stream().anyMatch(m -> m.getMissionType() == MissionType.CONSECUTIVE_LOGIN));
        assertTrue(savedMissions.stream().anyMatch(m -> m.getMissionType() == MissionType.LAUNCH_GAMES));
        assertTrue(savedMissions.stream().anyMatch(m -> m.getMissionType() == MissionType.PLAY_GAMES));
    }

    @Test
    void updateMissionProgress_shouldNotPublishEvent_whenMissionNotCompleted() {
        // Given
        Long userId = 1L;
        String userName = "testuser";

        when(missionRepository.findByUserIdAndMissionType(userId, MissionType.CONSECUTIVE_LOGIN))
                .thenReturn(Mono.just(consecutiveLoginMission));
        when(missionRepository.findByUserIdAndMissionType(userId, MissionType.LAUNCH_GAMES))
                .thenReturn(Mono.just(launchGamesMission));
        when(missionRepository.findByUserIdAndMissionType(userId, MissionType.PLAY_GAMES))
                .thenReturn(Mono.just(playGamesMission));

        when(userQueryService.getConsecutiveLoginDays(userId)).thenReturn(Mono.just(1));
        when(gameLaunchRecordRepository.countDistinctGamesLaunchedByUser(userId)).thenReturn(Mono.just(1L));
        when(gamePlayRecordRepository.countByUserId(userId)).thenReturn(Mono.just(1L));
        when(gamePlayRecordRepository.sumScoreByUserId(userId)).thenReturn(Mono.just(500));

        when(missionRepository.save(any(Mission.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(missionRepository.areAllMissionsCompleted(userId)).thenReturn(Mono.just(false));

        // When & Then
        StepVerifier.create(missionCommandService.updateMissionProgress(userId, userName))
                .verifyComplete();

        verify(eventPublisher, never()).publishMissionCompletedEvent(any(MissionCompletedEvent.class));
    }
}
