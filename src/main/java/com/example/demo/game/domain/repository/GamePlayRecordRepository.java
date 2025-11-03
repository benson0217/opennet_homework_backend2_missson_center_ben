package com.example.demo.game.domain.repository;

import com.example.demo.game.domain.model.GamePlayRecord;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 遊戲遊玩記錄儲存庫介面
 */
public interface GamePlayRecordRepository {

    Mono<GamePlayRecord> save(GamePlayRecord gamePlayRecord);

    /**
     * 計算使用者的玩遊戲記錄總數。
     *
     * @param userId 使用者ID
     * @return 包含玩遊戲記錄總數的 Mono<Long>
     */
    Mono<Long> countByUserId(Long userId); // 修改

    /**
     * 計算使用者的總分數。
     *
     * @param userId 使用者ID
     * @return 包含總分數的 Mono<Integer>
     */
    Mono<Integer> sumScoreByUserId(Long userId);
}
