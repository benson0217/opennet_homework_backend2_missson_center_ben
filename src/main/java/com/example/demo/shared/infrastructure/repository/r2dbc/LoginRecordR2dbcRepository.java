package com.example.demo.shared.infrastructure.repository.r2dbc;

import com.example.demo.shared.infrastructure.repository.data.LoginRecordData;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@Repository
public interface LoginRecordR2dbcRepository extends R2dbcRepository<LoginRecordData, Long>, LoginRecordR2dbcRepositoryCustom {

    Mono<Boolean> existsByUserIdAndLoginDate(Long userId, LocalDate loginDate);

}
