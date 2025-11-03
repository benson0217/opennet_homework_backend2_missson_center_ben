package com.example.demo.shared.infrastructure.message.consumer;

import com.example.demo.mission.application.service.MissionCommandService;
import com.example.demo.shared.application.dto.event.GameLaunchEvent;
import com.example.demo.shared.infrastructure.redis.RedisService; // 導入新的 RedisService
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
 * 遊戲啟動事件消費者
 * 監聽 task-center-game-launch 主題的消息
 * 處理遊戲啟動事件並更新相關任務進度
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
    topic = "task-center-game-launch",
    consumerGroup = "task-center-game-launch-consumer-group"
)
public class GameLaunchEventConsumer implements RocketMQListener<String> {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final MissionCommandService missionCommandService;
    private final RedisService redisService;

    private static final String IDEMPOTENCY_KEY_PREFIX = "game_launch_event:idempotency:";
    private static final Duration IDEMPOTENCY_KEY_TTL = Duration.ofDays(1);

    @Override
    public void onMessage(String message) {
        try {
            GameLaunchEvent event = objectMapper.readValue(message, GameLaunchEvent.class);

            String idempotencyKey = IDEMPOTENCY_KEY_PREFIX + event.userId() + ":" + event.gameId();

            redisService.setIfAbsent(idempotencyKey, "processed", IDEMPOTENCY_KEY_TTL)
                .flatMap(isNew -> {
                    if (Boolean.TRUE.equals(isNew)) {
                        log.info("接收到遊戲啟動事件 (首次處理): userId={}, username={}, gameId={}, gameCode={}, launchTime={}",
                            event.userId(), event.username(), event.gameId(), event.gameCode(), event.launchTime());

                        // 呼叫 MissionCommandService 更新任務進度
                        return missionCommandService.updateMissionProgress(event.userId(), event.username())
                            .doOnSuccess(v -> log.debug("成功觸發使用者 {} 的任務進度更新（遊戲啟動事件）", event.userId()))
                            .doOnError(e -> log.error("觸發使用者 {} 的任務進度更新失敗（遊戲啟動事件）", event.userId(), e));
                    } else {
                        log.warn("接收到重複的遊戲啟動事件: idempotencyKey={}, userId={}, gameId={}. 跳過處理.",
                            idempotencyKey, event.userId(), event.gameId());
                        return Mono.empty();
                    }
                })
                .subscribe(); // 訂閱以觸發響應式鏈的執行

        } catch (Exception e) {
            log.error("處理遊戲啟動事件失敗: {}", message, e);
        }
    }
}
