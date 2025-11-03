package com.example.demo.shared.infrastructure.repository.impl;

import com.example.demo.shared.infrastructure.repository.mapper.UserMapper;
import com.example.demo.shared.infrastructure.repository.r2dbc.UserR2DbcR2dbcRepository;
import com.example.demo.user.domain.model.User;
import com.example.demo.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * UserRepository 的基礎設施層實作。
 */
@Component
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserR2DbcR2dbcRepository r2dbcRepository;
    private final UserMapper mapper;

    @Override
    public Mono<User> findByUsername(String username) {
        return r2dbcRepository.findByUsername(username).map(mapper::toDomain);
    }

    @Override
    public Mono<User> findById(Long id) {
        return r2dbcRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Mono<User> save(User user) {
        return Mono.just(user)
            .map(mapper::toData)
            .flatMap(r2dbcRepository::save)
            .map(mapper::toDomain);
    }

    @Override
    public Mono<Void> addPoints(Long userId, int pointsToAdd) {
        return r2dbcRepository.addPoints(userId, pointsToAdd).then();
    }
}
