package com.example.demo.shared.infrastructure.repository.r2dbc;

import reactor.core.publisher.Mono;

public interface UserR2dbcRepositoryCustom {

    /**
     * 為使用者增加點數。
     *
     * @param userId      使用者ID
     * @param pointsToAdd 要增加的點數
     * @return 包含受影響行數的 Mono<Long>
     */
    Mono<Long> addPoints(Long userId, int pointsToAdd);

}
