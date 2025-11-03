package com.example.demo.shared.infrastructure.repository.r2dbc;

import com.example.demo.shared.infrastructure.repository.data.GameData;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface GameR2dbcRepository extends R2dbcRepository<GameData, Long> {

    Mono<GameData> findByGameCode(String gameCode);

    /**
     * 查詢所有已啟用的遊戲。
     *
     * @return 所有已啟用遊戲 Flux<GameData>
     */
    @Query("SELECT * FROM games WHERE is_active = true")
    Flux<GameData> findAllActive();
}
