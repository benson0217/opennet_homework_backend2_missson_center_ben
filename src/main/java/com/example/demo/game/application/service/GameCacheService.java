package com.example.demo.game.application.service;

import com.example.demo.game.domain.model.Game;
import com.example.demo.game.domain.repository.GameRepository;
import com.example.demo.shared.infrastructure.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameCacheService implements InitializingBean {

    private static final String GAME_CACHE_KEY = "games:active_list";

    private final GameRepository gameDbRepository;
    private final RedisService redisService;

    /**
     * 在所有屬性設置完成後調用，用於初始化遊戲列表快取。
     * 該方法會在應用啟動時預熱遊戲列表快取，將所有活躍遊戲從資料庫載入到 Redis 中。
     */
    @Override
    public void afterPropertiesSet() {
        log.info("正在預熱遊戲列表快取...");
        rebuildGameCache().subscribe(
            count -> log.info("成功快取 {} 個遊戲到 Redis.", count),
            error -> log.error("預熱遊戲列表快取失敗！", error)
        );
    }

    /**
     * 重建遊戲列表的快取。
     * 從資料庫中查詢所有啟用遊戲儲存到 Redis 的 Hash
     * @return 快取遊戲數量的 Mono<Long>。
     */
    public Mono<Long> rebuildGameCache() {
        return gameDbRepository.findAllActive()
            .collect(Collectors.toMap(Game::getGameCode, game -> game))
            .flatMap(gamesMap ->
                redisService.delete(GAME_CACHE_KEY)
                    .then(redisService.putAll(GAME_CACHE_KEY, gamesMap))
                    .thenReturn((long) gamesMap.size())
            );
    }

    /**
     * 將單個遊戲儲存到快取。
     * 如果遊戲已存在，則更新；如果不存在，則新增。
     * @param game 要儲存的遊戲對象。
     * @return 操作結果的 Mono<Boolean>。
     */
    public Mono<Boolean> saveGame(Game game) {
        return redisService.put(GAME_CACHE_KEY, game.getGameCode(), game);
    }
}
