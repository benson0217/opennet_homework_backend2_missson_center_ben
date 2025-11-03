package com.example.demo.shared.infrastructure.repository.r2dbc.impl;

import com.example.demo.shared.infrastructure.repository.data.LoginRecordData;
import com.example.demo.shared.infrastructure.repository.r2dbc.LoginRecordR2dbcRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import static org.springframework.data.relational.core.query.Criteria.where;

@Repository
@RequiredArgsConstructor
public class LoginRecordR2dbcRepositoryImpl implements LoginRecordR2dbcRepositoryCustom {

    private final R2dbcEntityTemplate r2dbcEntityTemplate;

    @Override
    public Flux<LoginRecordData> findRecentByUserId(Long userId, int limit) {
        Query query = Query.query(where("user_id").is(userId))
            .sort(Sort.by(Sort.Direction.DESC, "login_date"))
            .limit(limit);

        return r2dbcEntityTemplate.select(LoginRecordData.class)
            .matching(query)
            .all();
    }
}
