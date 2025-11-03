package com.example.demo.shared.infrastructure.repository.r2dbc.impl;

import com.example.demo.shared.infrastructure.repository.r2dbc.GamePlayRecordR2dbcRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class GamePlayRecordR2dbcRepositoryImpl implements GamePlayRecordR2dbcRepositoryCustom {

    private final R2dbcEntityTemplate r2dbcEntityTemplate;

    @Override
    public Mono<Integer> sumScoreByUserId(Long userId) {
        return r2dbcEntityTemplate.getDatabaseClient()
            .sql("SELECT COALESCE(SUM(score), 0) FROM games_play_record WHERE user_id = :userId")
            .bind("userId", userId)
            .map(row -> row.get(0, Integer.class))
            .one()
            .defaultIfEmpty(0);
    }
}
