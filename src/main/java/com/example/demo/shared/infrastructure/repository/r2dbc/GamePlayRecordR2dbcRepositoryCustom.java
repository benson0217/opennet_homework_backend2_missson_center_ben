package com.example.demo.shared.infrastructure.repository.r2dbc;

import reactor.core.publisher.Mono;

public interface GamePlayRecordR2dbcRepositoryCustom {

    /**
     * 計算使用者的總分數。
     *
     * @param userId 使用者ID
     * @return 包含總分數的 Mono<Integer>
     */
    Mono<Integer> sumScoreByUserId(Long userId);
}
