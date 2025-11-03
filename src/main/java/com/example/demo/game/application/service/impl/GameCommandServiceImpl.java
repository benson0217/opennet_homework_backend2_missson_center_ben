package com.example.demo.game.application.service.impl;

import com.example.demo.game.application.service.GameCommandService;
import com.example.demo.game.application.service.GameQueryService;
import com.example.demo.game.domain.model.GamePlayRecord;
import com.example.demo.shared.application.dto.event.GameLaunchEvent;
import com.example.demo.game.domain.model.Game;
import com.example.demo.game.domain.model.GameLaunchRecord;
import com.example.demo.shared.application.dto.event.GamePlayEvent;
import com.example.demo.shared.application.dto.event.UserLoginEvent;
import com.example.demo.user.domain.model.User;
import com.example.demo.game.domain.repository.GameLaunchRecordRepository;
import com.example.demo.game.domain.repository.GamePlayRecordRepository;
import com.example.demo.user.application.service.UserQueryService;
import com.example.demo.shared.infrastructure.message.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameCommandServiceImpl implements GameCommandService {

    private final GameLaunchRecordRepository gameLaunchRecordRepository;
    private final GamePlayRecordRepository gamePlayRecordRepository;
    private final EventPublisher eventPublisher;
    private final GameQueryService gameQueryService;

  /**
   * 處理遊戲啟動事件。 檢查使用者和遊戲是否存在，並記錄遊戲啟動。 如果遊戲曾經啟動過，則跳過處理。
   *
   * @param userId 使用者ID
   * @param gameCode 遊戲代碼
   * @return Mono<Void>
   */
  @Transactional
  @Override
  public Mono<Void> handleGameLaunch(User user, String gameCode) {
    Long userId = user.getId();
    return gameQueryService.findGameByCodeOrThrow(gameCode)
        .flatMap(game ->
                // 檢查是否曾經啟動過這款遊戲
            gameLaunchRecordRepository.existsByUserIdAndGameId(userId, game.getId())
                .flatMap(hasLaunched -> {
                    if (game.isAvailable()) {
                        return Mono.error(new IllegalStateException("遊戲不可用: " + gameCode));
                    }

                    if (Boolean.TRUE.equals(hasLaunched)) {
                        log.debug("使用者 {} 已啟動過遊戲 {}，跳過處理。", userId, gameCode);
                        return Mono.empty(); // 如果已啟動，則不執行任何操作
                    }

                    // 如果從未啟動過，則執行完整流程
                    log.info("處理遊戲啟動 - 使用者: {}, 遊戲: {}", user.getUsername(), gameCode);
                    return recordGameLaunch(user, game)
                        .flatMap(savedGame -> publishGameLaunchEvent(user, savedGame));
                }))
        .then();
  }

    /**
     * 記錄遊戲啟動。
     *
     * @param user 使用者實體
     * @param game 遊戲實體
     * @return 儲存的遊戲啟動紀錄資料 Mono<Game>
     */
    private Mono<Game> recordGameLaunch(User user, Game game) {
        GameLaunchRecord gameLaunchRecord = GameLaunchRecord.create(user.getId(), game.getId());
        return gameLaunchRecordRepository.save(gameLaunchRecord).thenReturn(game);
    }

    /**
     * 發布遊戲啟動事件。
     *
     * @param user 使用者實體
     * @param game 遊戲實體
     * @return Mono<Void>
     */
    private Mono<Void> publishGameLaunchEvent(User user, Game game) {
        if (user.isEligibleForMissions()) {
            GameLaunchEvent event = new GameLaunchEvent(user.getId(), user.getUsername(), game.getId(), game.getGameCode(), LocalDateTime.now());
            return eventPublisher.publishGameLaunchEvent(event);
        } else {
            log.debug("使用者 {} 不符合任務資格（註冊超過30天），跳過發布遊戲啟動事件。", user.getUsername());
            return Mono.empty();
        }
    }

    /**
     * 處理玩遊戲的事件。
     * 檢查使用者和遊戲是否存在，並記錄玩遊戲記錄。
     *
     * @param userId 使用者ID
     * @param gameCode 遊戲代碼
     * @param score 遊戲分數
     * @param playDuration 遊戲時間
     * @return Mono<Void>
     */
    @Transactional
    @Override
    public Mono<Void> handleGamePlay(User user, String gameCode, int score, Integer playDuration) {
        log.info("玩遊戲處理 - 使用者: {}, 遊戲: {}, 分數: {}", user.getUsername(), gameCode, score);
        return gameQueryService.findGameByCodeOrThrow(gameCode)
            .flatMap(game -> {
                if (game.isAvailable()) {
                    return Mono.error(new IllegalStateException("遊戲未啟用: " + gameCode));
                }

                return recordGamePlay(user, game, score, playDuration)
                    .flatMap(savedGame -> publishGamePlayEvent(user, savedGame, score, playDuration));
            }).then();
    }

    /**
     * 記錄遊戲遊玩。
     *
     * @param user 使用者實體
     * @param game 遊戲實體
     * @param score 遊戲分數
     * @param playDuration 遊戲時長
     * @return 儲存的遊戲遊玩紀錄資料Mono<Game>
     */
    private Mono<Game> recordGamePlay(User user, Game game, int score, Integer playDuration) {
        GamePlayRecord gamePlayRecord = GamePlayRecord.create(user.getId(), game.getId(), score, playDuration);
        return gamePlayRecordRepository.save(gamePlayRecord).thenReturn(game);
    }

    /**
     * 發布玩遊戲事件。
     *
     * @param user 使用者實體
     * @param game 遊戲實體
     * @param score 遊戲分數
     * @param playDuration 遊戲時長
     * @return Mono<Void>
     */
    private Mono<Void> publishGamePlayEvent(User user, Game game, int score, Integer playDuration) {
        if (user.isEligibleForMissions()) {
            GamePlayEvent event = new GamePlayEvent(user.getId(), user.getUsername(), game.getId(), game.getGameCode(), score, playDuration, LocalDateTime.now());
            return eventPublisher.publishGamePlayEvent(event);
        } else {
            log.debug("使用者 {} 不符合任務資格（註冊超過30天），跳過發布玩遊戲事件事件。", user.getUsername());
            return Mono.empty();
        }
    }
}
