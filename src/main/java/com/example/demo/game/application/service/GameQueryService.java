package com.example.demo.game.application.service;

import com.example.demo.game.domain.model.Game;
import reactor.core.publisher.Mono;

public interface GameQueryService {

    Mono<Game> getGameByCode(String gameCode);

    Mono<Game> findGameByCodeOrThrow(String gameCode);
}
