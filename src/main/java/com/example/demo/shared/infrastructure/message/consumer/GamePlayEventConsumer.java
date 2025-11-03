package com.example.demo.shared.infrastructure.message.consumer;

import com.example.demo.mission.application.service.MissionCommandService;
import com.example.demo.shared.application.dto.event.GamePlayEvent;
import com.example.demo.shared.infrastructure.redis.RedisService; // 導入 RedisService
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 遊戲遊玩事件消費者
 * 監聽 task-center-game-play 主題的消息
 * 處理遊戲遊玩事件並更新相關任務進度
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
    topic = "task-center-game-play",
    consumerGroup = "task-center-game-play-consumer-group"
)
public class GamePlayEventConsumer implements RocketMQListener<String> {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final MissionCommandService missionCommandService;
    private final RedisService redisService;

    private static final String IDEMPOTENCY_KEY_PREFIX = "game_play_event:idempotency:";
    private static final Duration IDEMPOTENCY_KEY_TTL = Duration.ofDays(1);

    @Override
    public void onMessage(String message) {
        try {
            GamePlayEvent event = objectMapper.readValue(message, GamePlayEvent.class);

            String idempotencyKey = IDEMPOTENCY_KEY_PREFIX + event.userId() + ":" + event.gameId() + ":" + event.score() + ":" + event.playTime().toEpochSecond(java.time.ZoneOffset.UTC);

            redisService.setIfAbsent(idempotencyKey, "processed", IDEMPOTENCY_KEY_TTL)
                .flatMap(isNew -> {
                    if (Boolean.TRUE.equals(isNew)) {
                        log.info("接收到遊戲遊玩事件 (首次處理): userId={}, username={}, gameId={}, gameCode={}, score={}, playDuration={}秒, playTime={}",
                            event.userId(), event.username(), event.gameId(), event.gameCode(),
                            event.score(), event.playDuration(), event.playTime());

                        // 呼叫 MissionCommandService 更新任務進度
                        return missionCommandService.updateMissionProgress(event.userId(), event.username())
                            .doOnSuccess(v -> log.debug("成功觸發使用者 {} 的任務進度更新（遊戲遊玩事件）", event.userId()))
                            .doOnError(e -> log.error("觸發使用者 {} 的任務進度更新失敗（遊戲遊玩事件）", event.userId(), e));
                    } else {
                        log.warn("接收到重複的遊戲遊玩事件: idempotencyKey={}, userId={}, gameId={}. 跳過處理.",
                            idempotencyKey, event.userId(), event.gameId());
                        return Mono.empty();
                    }
                })
                .subscribe();

        } catch (Exception e) {
            log.error("處理遊戲遊玩事件失敗: {}", message, e);
        }
    }
}
