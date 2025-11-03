package com.example.demo.shared.infrastructure.repository.impl;

import com.example.demo.game.domain.model.GamePlayRecord;
import com.example.demo.game.domain.repository.GamePlayRecordRepository;
import com.example.demo.shared.infrastructure.repository.mapper.GamePlayRecordMapper;
import com.example.demo.shared.infrastructure.repository.r2dbc.GamePlayRecordR2dbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class GamePlayRecordRepositoryImpl implements GamePlayRecordRepository {

    private final GamePlayRecordR2dbcRepository r2dbcRepository;
    private final GamePlayRecordMapper mapper;

    @Override
    public Mono<GamePlayRecord> save(GamePlayRecord gamePlayRecord) {
        return Mono.just(gamePlayRecord)
            .map(mapper::toData)
            .flatMap(r2dbcRepository::save)
            .map(mapper::toDomain);
    }

    @Override
    public Mono<Long> countByUserId(Long userId) { // 修改
        return r2dbcRepository.countByUserId(userId);
    }

    @Override
    public Mono<Integer> sumScoreByUserId(Long userId) {
        return r2dbcRepository.sumScoreByUserId(userId);
    }
}
