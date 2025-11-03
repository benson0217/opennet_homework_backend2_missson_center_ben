package com.example.demo.game.domain.repository;

import com.example.demo.game.domain.model.Game;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

/**
 * 遊戲儲存庫介面
 */
public interface GameRepository {

    Mono<Game> findByGameCode(String gameCode);

    Mono<Game> save(Game game);

    Flux<Game> findAllActive();
}
