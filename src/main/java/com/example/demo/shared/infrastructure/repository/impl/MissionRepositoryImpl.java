package com.example.demo.shared.infrastructure.repository.impl;

import com.example.demo.mission.domain.model.MissionType;
import com.example.demo.shared.infrastructure.repository.mapper.MissionPersistenceMapper;
import com.example.demo.shared.infrastructure.repository.r2dbc.MissionR2dbcRepository;
import com.example.demo.mission.domain.model.Mission;
import com.example.demo.mission.domain.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class MissionRepositoryImpl implements MissionRepository {

    private final MissionR2dbcRepository r2dbcRepository;
    private final MissionPersistenceMapper mapper;

    @Override
    public Mono<Mission> save(Mission mission) {
        return Mono.just(mission)
            .map(mapper::toData)
            .flatMap(r2dbcRepository::save)
            .map(mapper::toDomain);
    }

    @Override
    public Flux<Mission> findByUserId(Long userId) {
        return r2dbcRepository.findByUserId(userId).map(mapper::toDomain);
    }

    @Override
    public Mono<Mission> findByUserIdAndMissionType(Long userId, MissionType missionType) {
        return r2dbcRepository.findByUserIdAndMissionType(userId, missionType).map(mapper::toDomain);
    }

    @Override
    public Mono<Boolean> existsByUserIdAndMissionType(Long userId, MissionType missionType) {
        return r2dbcRepository.existsByUserIdAndMissionType(userId, missionType);
    }

    @Override
    public Flux<Mission> findUnrewardedCompletedMissions(Long userId) {
        return r2dbcRepository.findUnrewardedCompletedMissions(userId).map(mapper::toDomain);
    }

    @Override
    public Mono<Boolean> areAllMissionsCompleted(Long userId) {
        return r2dbcRepository.areAllMissionsCompleted(userId);
    }
}
