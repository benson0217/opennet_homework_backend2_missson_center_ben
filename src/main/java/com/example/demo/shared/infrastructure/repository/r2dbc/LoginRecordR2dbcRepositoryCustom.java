package com.example.demo.shared.infrastructure.repository.r2dbc;

import com.example.demo.shared.infrastructure.repository.data.LoginRecordData;
import reactor.core.publisher.Flux;

public interface LoginRecordR2dbcRepositoryCustom {

    /**
     * 查詢使用者最近的登入記錄。
     *
     * @param userId 使用者ID
     * @param limit  查詢的記錄數量上限
     * @return 包含最近登入記錄的 Flux<LoginRecordData>
     */
    Flux<LoginRecordData> findRecentByUserId(Long userId, int limit);
}
