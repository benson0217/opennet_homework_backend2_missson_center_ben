package com.example.demo.shared.infrastructure.repository.r2dbc;

import com.example.demo.shared.infrastructure.repository.data.GamePlayRecordData;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface GamePlayRecordR2dbcRepository extends R2dbcRepository<GamePlayRecordData, Long>, GamePlayRecordR2dbcRepositoryCustom {

    Flux<GamePlayRecordData> findByUserId(Long userId);

    /**
     * 使用衍生查詢計算使用者的遊玩記錄總數。
     *
     * @param userId 使用者ID
     * @return 包含遊玩記錄總數的 Mono<Long>
     */
    Mono<Long> countByUserId(Long userId);

}
