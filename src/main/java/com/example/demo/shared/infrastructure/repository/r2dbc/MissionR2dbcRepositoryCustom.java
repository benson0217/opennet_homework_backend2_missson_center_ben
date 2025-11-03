package com.example.demo.shared.infrastructure.repository.r2dbc;

import com.example.demo.shared.infrastructure.repository.data.MissionData;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MissionR2dbcRepositoryCustom {

    /**
     * 查詢使用者已完成但尚未領取獎勵的任務。
     *
     * @param userId 使用者ID
     * @return 包含未領取獎勵的已完成任務的 Flux<MissionData>
     */
    Flux<MissionData> findUnrewardedCompletedMissions(Long userId);

    /**
     * 檢查使用者是否已完成所有任務。
     *
     * @param userId 使用者ID
     * @return 如果所有任務都已完成，則返回 Mono<Boolean> of true
     */
    Mono<Boolean> areAllMissionsCompleted(Long userId);
}
