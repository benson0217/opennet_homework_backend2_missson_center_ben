package com.example.demo.shared.infrastructure.repository.r2dbc.impl;

import com.example.demo.shared.infrastructure.repository.r2dbc.GameLaunchRecordR2dbcRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class GameLaunchRecordR2dbcRepositoryImpl implements GameLaunchRecordR2dbcRepositoryCustom {

    private final R2dbcEntityTemplate r2dbcEntityTemplate;

    @Override
    public Mono<Long> countDistinctGamesLaunchedByUser(Long userId) {
        return r2dbcEntityTemplate.getDatabaseClient()
            .sql("SELECT COUNT(DISTINCT game_id) FROM game_launch_record WHERE user_id = :userId")
            .bind("userId", userId)
            .map(row -> row.get(0, Long.class))
            .one()
            .defaultIfEmpty(0L);
    }
}
