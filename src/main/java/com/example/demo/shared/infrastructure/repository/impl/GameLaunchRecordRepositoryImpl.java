package com.example.demo.shared.infrastructure.repository.impl;

import com.example.demo.game.domain.model.GameLaunchRecord;
import com.example.demo.game.domain.repository.GameLaunchRecordRepository;
import com.example.demo.shared.infrastructure.repository.mapper.GameLaunchRecordMapper;
import com.example.demo.shared.infrastructure.repository.r2dbc.GameLaunchRecordR2dbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class GameLaunchRecordRepositoryImpl implements GameLaunchRecordRepository {

    private final GameLaunchRecordR2dbcRepository r2dbcRepository;
    private final GameLaunchRecordMapper mapper;

    @Override
    public Mono<GameLaunchRecord> save(GameLaunchRecord gameLaunchRecord) {
        return Mono.just(gameLaunchRecord)
            .map(mapper::toData)
            .flatMap(r2dbcRepository::save)
            .map(mapper::toDomain);
    }

    @Override
    public Mono<Long> countDistinctGamesLaunchedByUser(Long userId) {
        return r2dbcRepository.countDistinctGamesLaunchedByUser(userId);
    }

    @Override
    public Mono<Boolean> existsByUserIdAndGameId(Long userId, Long gameId) {
        return r2dbcRepository.existsByUserIdAndGameId(userId, gameId);
    }
}
