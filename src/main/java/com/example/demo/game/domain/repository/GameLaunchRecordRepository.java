package com.example.demo.game.domain.repository;

import com.example.demo.game.domain.model.GameLaunchRecord;
import reactor.core.publisher.Mono;

/**
 * 遊戲啟動記錄儲存庫介面
 */
public interface GameLaunchRecordRepository {

    Mono<GameLaunchRecord> save(GameLaunchRecord gameLaunchRecord);

    Mono<Long> countDistinctGamesLaunchedByUser(Long userId);

    /**
     * 檢查使用者是否曾經啟動過指定的遊戲。
     *
     * @param userId 使用者ID
     * @param gameId 遊戲ID
     * @return 如果曾經啟動過，則返回 Mono<Boolean> of true
     */
    Mono<Boolean> existsByUserIdAndGameId(Long userId, Long gameId);
}
