package com.example.demo.shared.infrastructure.repository.r2dbc;

import com.example.demo.mission.domain.model.MissionType;
import com.example.demo.shared.infrastructure.repository.data.MissionData;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface MissionR2dbcRepository extends R2dbcRepository<MissionData, Long>, MissionR2dbcRepositoryCustom {

    Flux<MissionData> findByUserId(Long userId);

    Mono<MissionData> findByUserIdAndMissionType(Long userId, MissionType missionType);

    Mono<Boolean> existsByUserIdAndMissionType(Long userId, MissionType missionType);

}
