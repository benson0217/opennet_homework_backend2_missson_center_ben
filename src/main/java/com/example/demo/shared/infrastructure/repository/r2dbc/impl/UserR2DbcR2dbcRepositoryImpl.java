package com.example.demo.shared.infrastructure.repository.r2dbc.impl;

import com.example.demo.shared.infrastructure.repository.r2dbc.UserR2dbcRepositoryCustom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Slf4j
@Repository
@RequiredArgsConstructor
public class UserR2DbcR2dbcRepositoryImpl implements UserR2dbcRepositoryCustom {

    private final DatabaseClient databaseClient;

    @Override
    public Mono<Long> addPoints(Long userId, int pointsToAdd) {
        String sql = "UPDATE users SET points = points + :pointsToAdd, updated_at = NOW() WHERE id = :userId";

        return databaseClient.sql(sql)
            .bind("userId", userId)
            .bind("pointsToAdd", pointsToAdd)
            .fetch()
            .rowsUpdated()
            .doOnSuccess(count -> log.debug("成功為使用者 {} 增加 {} 點數，更新了 {} 筆記錄", 
                userId, pointsToAdd, count))
            .doOnError(e -> log.error("為使用者 {} 增加點數失敗", userId, e));
    }
}
