package com.example.demo.shared.infrastructure.repository.impl;

import com.example.demo.game.domain.model.Game;
import com.example.demo.game.domain.repository.GameRepository;
import com.example.demo.shared.infrastructure.repository.mapper.GameMapper;
import com.example.demo.shared.infrastructure.repository.r2dbc.GameR2dbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class GameRepositoryImpl implements GameRepository {

    private final GameR2dbcRepository r2dbcRepository;
    private final GameMapper mapper;

    @Override
    public Mono<Game> findByGameCode(String gameCode) {
        return r2dbcRepository.findByGameCode(gameCode).map(mapper::toDomain);
    }

    @Override
    public Mono<Game> save(Game game) {
        return Mono.just(game)
            .map(mapper::toData)
            .flatMap(r2dbcRepository::save)
            .map(mapper::toDomain);
    }

    @Override
    public Flux<Game> findAllActive() { // 恢復此方法
        return r2dbcRepository.findAllActive().map(mapper::toDomain);
    }
}
