package com.example.demo.shared.infrastructure.repository.r2dbc;

import com.example.demo.shared.infrastructure.repository.data.UserData;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserR2DbcR2dbcRepository extends R2dbcRepository<UserData, Long>, UserR2dbcRepositoryCustom {

    Mono<UserData> findByUsername(String username);
}
