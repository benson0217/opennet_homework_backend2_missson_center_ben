package com.example.demo.shared.infrastructure.repository.r2dbc.impl;

import com.example.demo.shared.infrastructure.repository.data.MissionData;
import com.example.demo.shared.infrastructure.repository.r2dbc.MissionR2dbcRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.springframework.data.relational.core.query.Criteria.where;

@Repository
@RequiredArgsConstructor
public class MissionR2dbcRepositoryImpl implements MissionR2dbcRepositoryCustom {

    private final R2dbcEntityTemplate r2dbcEntityTemplate;

    @Override
    public Flux<MissionData> findUnrewardedCompletedMissions(Long userId) {
        Query query = Query.query(
            where("user_id").is(userId)
                .and("is_completed").is(true)
                .and("is_rewarded").is(false)
        );
        return r2dbcEntityTemplate.select(MissionData.class)
            .matching(query)
            .all();
    }

    @Override
    public Mono<Boolean> areAllMissionsCompleted(Long userId) {
        Query query = Query.query(
            where("user_id").is(userId)
                .and("is_completed").is(true)
        );
        return r2dbcEntityTemplate.count(query, MissionData.class)
            .map(count -> count == 3);
    }
}
