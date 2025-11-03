package com.example.demo.mission.domain.repository;

import com.example.demo.mission.domain.model.MissionType;
import com.example.demo.mission.domain.model.Mission;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 任務儲存庫介面
 */
public interface MissionRepository {

    Mono<Mission> save(Mission mission);

    Flux<Mission> findByUserId(Long userId);

    Mono<Mission> findByUserIdAndMissionType(Long userId, MissionType missionType);

    Mono<Boolean> existsByUserIdAndMissionType(Long userId, MissionType missionType);

    Flux<Mission> findUnrewardedCompletedMissions(Long userId);

    Mono<Boolean> areAllMissionsCompleted(Long userId);
}
