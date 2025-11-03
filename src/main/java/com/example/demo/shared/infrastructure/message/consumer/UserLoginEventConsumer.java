package com.example.demo.shared.infrastructure.message.consumer;

import com.example.demo.mission.application.service.MissionCommandService;
import com.example.demo.shared.application.dto.event.UserLoginEvent;
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
 * 使用者登入事件消費者
 * 監聽 task-center-user-login 主題的消息
 * 處理登入事件並更新相關任務進度
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
    topic = "task-center-user-login",
    consumerGroup = "task-center-user-login-consumer-group"
)
public class UserLoginEventConsumer implements RocketMQListener<String> {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final MissionCommandService missionCommandService;
    private final RedisService redisService;

    private static final String IDEMPOTENCY_KEY_PREFIX = "user_login_event:idempotency:";
    private static final Duration IDEMPOTENCY_KEY_TTL = Duration.ofDays(1);

    @Override
    public void onMessage(String message) {
        try {
            UserLoginEvent event = objectMapper.readValue(message, UserLoginEvent.class);

            String idempotencyKey = IDEMPOTENCY_KEY_PREFIX + event.userId();

            redisService.setIfAbsent(idempotencyKey, "processed", IDEMPOTENCY_KEY_TTL)
                .flatMap(isNew -> {
                    if (Boolean.TRUE.equals(isNew)) {
                        log.info("接收到使用者登入事件 (首次處理): userId={}, username={}, loginTime={}",
                            event.userId(), event.username(), event.loginTime());

                        // 呼叫 MissionCommandService 更新任務進度
                        return missionCommandService.updateMissionProgress(event.userId(), event.username())
                            .doOnSuccess(v -> log.debug("成功觸發使用者 {} 的任務進度更新（登入事件）", event.userId()))
                            .doOnError(e -> log.error("觸發使用者 {} 的任務進度更新失敗（登入事件）", event.userId(), e));
                    } else {
                        log.warn("接收到重複的使用者登入事件: idempotencyKey={}, userId={}. 跳過處理.",
                            idempotencyKey, event.userId());
                        return Mono.empty();
                    }
                })
                .subscribe();

        } catch (Exception e) {
            log.error("處理使用者登入事件失敗: {}", message, e);
        }
    }
}
