package com.example.demo.user.domain.repository;

import com.example.demo.user.domain.model.LoginRecord;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * 登入記錄的儲存庫介面
 */
public interface LoginRecordRepository {

    /**
     * 儲存一筆登入記錄。
     *
     * @param loginRecord 要儲存的登入記錄
     * @return 已儲存記錄的 Mono<LoginRecord>
     */
    Mono<LoginRecord> save(LoginRecord loginRecord);

    /**
     * 檢查使用者是否在特定日期登入過。
     *
     * @param userId    使用者ID
     * @param loginDate 登入日期
     * @return 如果存在記錄，則返回 true 的 Mono<Boolean>
     */
    Mono<Boolean> existsByUserIdAndLoginDate(Long userId, LocalDate loginDate);

    /**
     * 查詢使用者最近的登入記錄，用於計算連續登入。
     *
     * @param userId 使用者ID
     * @param limit  查詢的記錄數量上限
     * @return 包含最近登入記錄的 Flux
     */
    Flux<LoginRecord> findRecentByUserId(Long userId, int limit);
}
