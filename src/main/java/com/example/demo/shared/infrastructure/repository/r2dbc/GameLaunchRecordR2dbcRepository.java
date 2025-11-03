package com.example.demo.shared.infrastructure.repository.r2dbc;

import com.example.demo.shared.infrastructure.repository.data.GameLaunchRecordData;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface GameLaunchRecordR2dbcRepository extends R2dbcRepository<GameLaunchRecordData, Long>, GameLaunchRecordR2dbcRepositoryCustom {

    /**
     * 檢查使用者是否曾經啟動過指定遊戲。
     */
    Mono<Boolean> existsByUserIdAndGameId(Long userId, Long gameId);
}
