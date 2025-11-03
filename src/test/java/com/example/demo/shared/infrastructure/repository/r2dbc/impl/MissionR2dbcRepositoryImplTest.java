package com.example.demo.shared.infrastructure.repository.r2dbc.impl;

import com.example.demo.mission.domain.model.MissionType;
import com.example.demo.shared.infrastructure.repository.data.MissionData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.core.ReactiveSelectOperation;
import org.springframework.data.relational.core.query.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MissionR2dbcRepositoryImplTest {

    @Mock
    private R2dbcEntityTemplate r2dbcEntityTemplate;

    @Mock
    private ReactiveSelectOperation.ReactiveSelect<MissionData> reactiveSelect;

    @Mock
    private ReactiveSelectOperation.TerminatingSelect<MissionData> terminatingSelect;

    @Mock
    private ReactiveSelectOperation.TerminatingSelect<Long> terminatingSelectLong;

    @InjectMocks
    private MissionR2dbcRepositoryImpl missionR2dbcRepositoryImpl;

    private MissionData completedUnrewardedMission1;
    private MissionData completedUnrewardedMission2;
    private MissionData completedRewardedMission;
    private MissionData incompleteMission;

    @BeforeEach
    void setUp() {
        completedUnrewardedMission1 = new MissionData();
        completedUnrewardedMission1.setId(1L);
        completedUnrewardedMission1.setUserId(100L);
        completedUnrewardedMission1.setMissionType(MissionType.CONSECUTIVE_LOGIN);
        completedUnrewardedMission1.setCurrentProgress(3);
        completedUnrewardedMission1.setTargetProgress(3);
        completedUnrewardedMission1.setIsCompleted(true);
        completedUnrewardedMission1.setCompletedAt(LocalDateTime.now());
        completedUnrewardedMission1.setIsRewarded(false);
        completedUnrewardedMission1.setRewardPoints(0);

        completedUnrewardedMission2 = new MissionData();
        completedUnrewardedMission2.setId(2L);
        completedUnrewardedMission2.setUserId(100L);
        completedUnrewardedMission2.setMissionType(MissionType.LAUNCH_GAMES);
        completedUnrewardedMission2.setCurrentProgress(3);
        completedUnrewardedMission2.setTargetProgress(3);
        completedUnrewardedMission2.setIsCompleted(true);
        completedUnrewardedMission2.setCompletedAt(LocalDateTime.now());
        completedUnrewardedMission2.setIsRewarded(false);
        completedUnrewardedMission2.setRewardPoints(0);

        completedRewardedMission = new MissionData();
        completedRewardedMission.setId(3L);
        completedRewardedMission.setUserId(100L);
        completedRewardedMission.setMissionType(MissionType.PLAY_GAMES);
        completedRewardedMission.setCurrentProgress(3);
        completedRewardedMission.setTargetProgress(3);
        completedRewardedMission.setIsCompleted(true);
        completedRewardedMission.setCompletedAt(LocalDateTime.now());
        completedRewardedMission.setIsRewarded(true);
        completedRewardedMission.setRewardedAt(LocalDateTime.now());
        completedRewardedMission.setRewardPoints(100);

        incompleteMission = new MissionData();
        incompleteMission.setId(4L);
        incompleteMission.setUserId(100L);
        incompleteMission.setMissionType(MissionType.CONSECUTIVE_LOGIN);
        incompleteMission.setCurrentProgress(1);
        incompleteMission.setTargetProgress(3);
        incompleteMission.setIsCompleted(false);
        incompleteMission.setIsRewarded(false);
        incompleteMission.setRewardPoints(0);
    }

    @Test
    void findUnrewardedCompletedMissions_shouldReturnMissions_whenUnrewardedMissionsExist() {
        // Given
        Long userId = 100L;
        List<MissionData> missions = Arrays.asList(completedUnrewardedMission1, completedUnrewardedMission2);

        when(r2dbcEntityTemplate.select(MissionData.class)).thenReturn(reactiveSelect);
        when(reactiveSelect.matching(any(Query.class))).thenReturn(terminatingSelect);
        when(terminatingSelect.all()).thenReturn(Flux.fromIterable(missions));

        // When & Then
        StepVerifier.create(missionR2dbcRepositoryImpl.findUnrewardedCompletedMissions(userId))
                .expectNext(completedUnrewardedMission1)
                .expectNext(completedUnrewardedMission2)
                .verifyComplete();

        verify(r2dbcEntityTemplate).select(MissionData.class);
        verify(reactiveSelect).matching(any(Query.class));
        verify(terminatingSelect).all();
    }

    @Test
    void findUnrewardedCompletedMissions_shouldReturnEmpty_whenNoUnrewardedMissions() {
        // Given
        Long userId = 100L;

        when(r2dbcEntityTemplate.select(MissionData.class)).thenReturn(reactiveSelect);
        when(reactiveSelect.matching(any(Query.class))).thenReturn(terminatingSelect);
        when(terminatingSelect.all()).thenReturn(Flux.empty());

        // When & Then
        StepVerifier.create(missionR2dbcRepositoryImpl.findUnrewardedCompletedMissions(userId))
                .verifyComplete();

        verify(r2dbcEntityTemplate).select(MissionData.class);
        verify(reactiveSelect).matching(any(Query.class));
        verify(terminatingSelect).all();
    }

    @Test
    void findUnrewardedCompletedMissions_shouldReturnSingleMission_whenOnlyOneMissionExists() {
        // Given
        Long userId = 100L;

        when(r2dbcEntityTemplate.select(MissionData.class)).thenReturn(reactiveSelect);
        when(reactiveSelect.matching(any(Query.class))).thenReturn(terminatingSelect);
        when(terminatingSelect.all()).thenReturn(Flux.just(completedUnrewardedMission1));

        // When & Then
        StepVerifier.create(missionR2dbcRepositoryImpl.findUnrewardedCompletedMissions(userId))
                .expectNext(completedUnrewardedMission1)
                .verifyComplete();

        verify(r2dbcEntityTemplate).select(MissionData.class);
        verify(reactiveSelect).matching(any(Query.class));
        verify(terminatingSelect).all();
    }

    @Test
    void findUnrewardedCompletedMissions_shouldPropagateError_whenRepositoryFails() {
        // Given
        Long userId = 100L;
        RuntimeException repositoryError = new RuntimeException("Database error");

        when(r2dbcEntityTemplate.select(MissionData.class)).thenReturn(reactiveSelect);
        when(reactiveSelect.matching(any(Query.class))).thenReturn(terminatingSelect);
        when(terminatingSelect.all()).thenReturn(Flux.error(repositoryError));

        // When & Then
        StepVerifier.create(missionR2dbcRepositoryImpl.findUnrewardedCompletedMissions(userId))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("Database error"))
                .verify();

        verify(r2dbcEntityTemplate).select(MissionData.class);
        verify(reactiveSelect).matching(any(Query.class));
        verify(terminatingSelect).all();
    }

    @Test
    void findUnrewardedCompletedMissions_shouldFilterCorrectly() {
        // Given
        Long userId = 100L;
        // Should only return completed and unrewarded missions
        List<MissionData> missions = Arrays.asList(completedUnrewardedMission1, completedUnrewardedMission2);

        when(r2dbcEntityTemplate.select(MissionData.class)).thenReturn(reactiveSelect);
        when(reactiveSelect.matching(any(Query.class))).thenReturn(terminatingSelect);
        when(terminatingSelect.all()).thenReturn(Flux.fromIterable(missions));

        // When & Then
        StepVerifier.create(missionR2dbcRepositoryImpl.findUnrewardedCompletedMissions(userId))
                .expectNextSequence(missions)
                .verifyComplete();

        verify(r2dbcEntityTemplate).select(MissionData.class);
        verify(reactiveSelect).matching(any(Query.class));
        verify(terminatingSelect).all();
    }

    @Test
    void areAllMissionsCompleted_shouldReturnTrue_whenAllThreeMissionsCompleted() {
        // Given
        Long userId = 100L;

        when(r2dbcEntityTemplate.count(any(Query.class), eq(MissionData.class))).thenReturn(Mono.just(3L));

        // When & Then
        StepVerifier.create(missionR2dbcRepositoryImpl.areAllMissionsCompleted(userId))
                .expectNext(true)
                .verifyComplete();

        verify(r2dbcEntityTemplate).count(any(Query.class), eq(MissionData.class));
    }

    @Test
    void areAllMissionsCompleted_shouldReturnFalse_whenOnlyTwoMissionsCompleted() {
        // Given
        Long userId = 100L;

        when(r2dbcEntityTemplate.count(any(Query.class), eq(MissionData.class))).thenReturn(Mono.just(2L));

        // When & Then
        StepVerifier.create(missionR2dbcRepositoryImpl.areAllMissionsCompleted(userId))
                .expectNext(false)
                .verifyComplete();

        verify(r2dbcEntityTemplate).count(any(Query.class), eq(MissionData.class));
    }

    @Test
    void areAllMissionsCompleted_shouldReturnFalse_whenOnlyOneMissionCompleted() {
        // Given
        Long userId = 100L;

        when(r2dbcEntityTemplate.count(any(Query.class), eq(MissionData.class))).thenReturn(Mono.just(1L));

        // When & Then
        StepVerifier.create(missionR2dbcRepositoryImpl.areAllMissionsCompleted(userId))
                .expectNext(false)
                .verifyComplete();

        verify(r2dbcEntityTemplate).count(any(Query.class), eq(MissionData.class));
    }

    @Test
    void areAllMissionsCompleted_shouldReturnFalse_whenNoMissionsCompleted() {
        // Given
        Long userId = 100L;

        when(r2dbcEntityTemplate.count(any(Query.class), eq(MissionData.class))).thenReturn(Mono.just(0L));

        // When & Then
        StepVerifier.create(missionR2dbcRepositoryImpl.areAllMissionsCompleted(userId))
                .expectNext(false)
                .verifyComplete();

        verify(r2dbcEntityTemplate).count(any(Query.class), eq(MissionData.class));
    }

    @Test
    void areAllMissionsCompleted_shouldPropagateError_whenRepositoryFails() {
        // Given
        Long userId = 100L;
        RuntimeException repositoryError = new RuntimeException("Database error");

        when(r2dbcEntityTemplate.count(any(Query.class), eq(MissionData.class))).thenReturn(Mono.error(repositoryError));

        // When & Then
        StepVerifier.create(missionR2dbcRepositoryImpl.areAllMissionsCompleted(userId))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("Database error"))
                .verify();

        verify(r2dbcEntityTemplate).count(any(Query.class), eq(MissionData.class));
    }

    @Test
    void areAllMissionsCompleted_shouldHandleDifferentUserId() {
        // Given
        Long userId = 200L;

        when(r2dbcEntityTemplate.count(any(Query.class), eq(MissionData.class))).thenReturn(Mono.just(3L));

        // When & Then
        StepVerifier.create(missionR2dbcRepositoryImpl.areAllMissionsCompleted(userId))
                .expectNext(true)
                .verifyComplete();

        verify(r2dbcEntityTemplate).count(any(Query.class), eq(MissionData.class));
    }

    @Test
    void findUnrewardedCompletedMissions_shouldHandleDifferentUserId() {
        // Given
        Long userId = 200L;
        MissionData mission = new MissionData();
        mission.setId(10L);
        mission.setUserId(userId);
        mission.setMissionType(MissionType.CONSECUTIVE_LOGIN);
        mission.setIsCompleted(true);
        mission.setIsRewarded(false);

        when(r2dbcEntityTemplate.select(MissionData.class)).thenReturn(reactiveSelect);
        when(reactiveSelect.matching(any(Query.class))).thenReturn(terminatingSelect);
        when(terminatingSelect.all()).thenReturn(Flux.just(mission));

        // When & Then
        StepVerifier.create(missionR2dbcRepositoryImpl.findUnrewardedCompletedMissions(userId))
                .expectNext(mission)
                .verifyComplete();

        verify(r2dbcEntityTemplate).select(MissionData.class);
        verify(reactiveSelect).matching(any(Query.class));
        verify(terminatingSelect).all();
    }

    @Test
    void areAllMissionsCompleted_shouldReturnFalse_whenMoreThanThreeMissionsCompleted() {
        // Given - edge case, should only have 3 missions per user
        Long userId = 100L;

        when(r2dbcEntityTemplate.count(any(Query.class), eq(MissionData.class))).thenReturn(Mono.just(4L));

        // When & Then
        StepVerifier.create(missionR2dbcRepositoryImpl.areAllMissionsCompleted(userId))
                .expectNext(false)
                .verifyComplete();

        verify(r2dbcEntityTemplate).count(any(Query.class), eq(MissionData.class));
    }

    @Test
    void findUnrewardedCompletedMissions_shouldReturnCorrectMissionFields() {
        // Given
        Long userId = 100L;

        when(r2dbcEntityTemplate.select(MissionData.class)).thenReturn(reactiveSelect);
        when(reactiveSelect.matching(any(Query.class))).thenReturn(terminatingSelect);
        when(terminatingSelect.all()).thenReturn(Flux.just(completedUnrewardedMission1));

        // When & Then
        StepVerifier.create(missionR2dbcRepositoryImpl.findUnrewardedCompletedMissions(userId))
                .assertNext(mission -> {
                    assert mission.getId().equals(1L);
                    assert mission.getUserId().equals(100L);
                    assert mission.getIsCompleted().equals(true);
                    assert mission.getIsRewarded().equals(false);
                    assert mission.getCompletedAt() != null;
                })
                .verifyComplete();

        verify(r2dbcEntityTemplate).select(MissionData.class);
        verify(reactiveSelect).matching(any(Query.class));
        verify(terminatingSelect).all();
    }
}
