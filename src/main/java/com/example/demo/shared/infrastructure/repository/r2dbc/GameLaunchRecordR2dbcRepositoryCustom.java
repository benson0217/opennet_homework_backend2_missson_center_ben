package com.example.demo.shared.infrastructure.repository.r2dbc;

import reactor.core.publisher.Mono;

public interface GameLaunchRecordR2dbcRepositoryCustom {

    /**
     * 計算使用者啟動過的不同遊戲的數量。
     *
     * @param userId 使用者ID
     * @return 包含不同遊戲數量的 Mono<Long>
     */
    Mono<Long> countDistinctGamesLaunchedByUser(Long userId);
}
