package com.example.demo.shared.infrastructure.repository.impl;

import com.example.demo.shared.infrastructure.repository.mapper.LoginRecordMapper;
import com.example.demo.shared.infrastructure.repository.r2dbc.LoginRecordR2dbcRepository;
import com.example.demo.user.domain.model.LoginRecord;
import com.example.demo.user.domain.repository.LoginRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@Repository
@RequiredArgsConstructor
public class LoginRecordRepositoryImpl implements LoginRecordRepository {

    private final LoginRecordR2dbcRepository r2dbcRepository;
    private final LoginRecordMapper mapper;

    @Override
    public Mono<LoginRecord> save(LoginRecord loginRecord) {
        return Mono.just(loginRecord)
            .map(mapper::toData)
            .flatMap(r2dbcRepository::save)
            .map(mapper::toDomain);
    }

    @Override
    public Mono<Boolean> existsByUserIdAndLoginDate(Long userId, LocalDate loginDate) {
        return r2dbcRepository.existsByUserIdAndLoginDate(userId, loginDate);
    }

    @Override
    public Flux<LoginRecord> findRecentByUserId(Long userId, int limit) {
        return r2dbcRepository.findRecentByUserId(userId, limit).map(mapper::toDomain);
    }
}
