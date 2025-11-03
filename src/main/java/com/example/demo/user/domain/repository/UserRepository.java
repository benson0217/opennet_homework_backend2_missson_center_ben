package com.example.demo.user.domain.repository;

import com.example.demo.user.domain.model.User;
import reactor.core.publisher.Mono;

/**
 * 使用者聚合根的儲存庫介面
 */
public interface UserRepository {

    /**
     * 根據使用者名稱查詢使用者。
     *
     * @param username 使用者名稱
     * @return 包含使用者實體的 Mono<User>
     */
    Mono<User> findByUsername(String username);

    /**
     * 根據 ID 查詢使用者。
     *
     * @param id 使用者 ID
     * @return 包含使用者實體的 Mono<User>
     */
    Mono<User> findById(Long id);

    /**
     * 儲存（新增或更新）使用者。
     *
     * @param user 要儲存的使用者
     * @return 已儲存使用者的 Mono<User>
     */
    Mono<User> save(User user);

    /**
     * 為使用者增加點數。
     * 這是一個優化過的更新操作，無需先獲取完整的實體。
     *
     * @param userId      使用者ID
     * @param pointsToAdd 要增加的點數
     * @return 表示操作完成的 Mono<Void>
     */
    Mono<Void> addPoints(Long userId, int pointsToAdd);
}
