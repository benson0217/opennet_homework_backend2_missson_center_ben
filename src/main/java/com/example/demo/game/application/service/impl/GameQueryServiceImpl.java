package com.example.demo.game.application.service.impl;

import com.example.demo.game.application.service.GameCacheService;
import com.example.demo.game.application.service.GameQueryService;
import com.example.demo.game.domain.model.Game;
import com.example.demo.game.domain.repository.GameRepository;
import com.example.demo.shared.infrastructure.redis.RedisService; // 修改為 RedisService
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 遊戲查詢服務
 * 負責處理遊戲相關的讀操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameQueryServiceImpl implements GameQueryService {

    private static final String GAME_CACHE_KEY = "games:active_list";

    private final GameRepository gameRepository;
    private final RedisService redisService;
    private final GameCacheService gameCacheService;

    /**
     * 根據遊戲代碼取得遊戲資訊
     */
    @Override
    public Mono<Game> getGameByCode(String gameCode) {
        return redisService.<String, Game>get(GAME_CACHE_KEY, gameCode)
            // 1. 處理快取服務故障
            .onErrorResume(RedisSystemException.class, e -> {
                log.warn("從 Redis 獲取遊戲 '{}' 失敗，降級查詢資料庫。錯誤: {}", gameCode, e.getMessage());
                return gameRepository.findByGameCode(gameCode);
            })
            // 2. 處理快取未命中
            .switchIfEmpty(Mono.defer(() -> {
                log.info("遊戲 '{}' 快取未命中，查詢資料庫並寫回快取。", gameCode);
                return gameRepository.findByGameCode(gameCode)
                    .flatMap(dbGame -> gameCacheService.saveGame(dbGame).thenReturn(dbGame));
            }));
    }

    /**
     * 根據遊戲代碼查找遊戲，如果不存在則拋出 IllegalArgumentException。
     *
     * @param gameCode 遊戲代碼
     * @return 包含找到的遊戲的 Mono<Game>
     */
    @Override
    public Mono<Game> findGameByCodeOrThrow(String gameCode) {
        return getGameByCode(gameCode)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("找不到遊戲: " + gameCode)));
    }
}
