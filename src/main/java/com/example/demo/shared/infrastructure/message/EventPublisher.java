package com.example.demo.shared.infrastructure.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * RocketMQ 的事件發布器
 */
@Slf4j
@Service
public class EventPublisher {

    private static final String TOPIC_PREFIX = "task-center-";
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final Duration RETRY_BACKOFF = Duration.ofSeconds(1);

    private final RocketMQTemplate rocketMQTemplate;
    private final ObjectMapper objectMapper;

    public EventPublisher(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * 發布事件
     *
     * @param topic 主題名稱（不含前綴）
     * @param event 要發布的事件物件
     * @param <T>   事件的類型
     * @return 表示發布完成的 Mono<Void>
     */
    public <T> Mono<Void> publishEvent(String topic, T event) {
        try {
            String message = objectMapper.writeValueAsString(event);
            String fullTopic = TOPIC_PREFIX + topic;

            CompletableFuture<SendResult> future = new CompletableFuture<>();

            // 使用 RocketMQ 的 asyncSend 配合 SendCallback
            rocketMQTemplate.asyncSend(
                fullTopic,
                MessageBuilder.withPayload(message).build(),
                new SendCallback() {
                    @Override
                    public void onSuccess(SendResult sendResult) {
                        future.complete(sendResult);
                    }

                    @Override
                    public void onException(Throwable throwable) {
                        future.completeExceptionally(throwable);
                    }
                }
            );

            return Mono.fromFuture(future)
                .doOnSuccess(result ->
                    log.debug("已發布事件到主題 {}: {}, SendResult: {}", fullTopic, event, result)
                )
                .doOnError(e ->
                    log.error("發布事件失敗到主題 {}: {}", fullTopic, event, e)
                )
                .then();
        } catch (JsonProcessingException e) {
            log.error("序列化事件失敗: {}", event, e);
            return Mono.error(new RuntimeException("發布事件失敗", e));
        }
    }

    /**
     * 發布事件
     * 使用指數退避策略重試，如果最終失敗會拋出異常
     * 適用於需要保證訊息發送成功的場景
     *
     * @param topic 主題名稱（不含前綴）
     * @param event 要發布的事件物件
     * @param <T>   事件的類型
     * @return 表示發布完成的 Mono<Void>，如果失敗會拋出異常
     */
    public <T> Mono<Void> publishEventWithRetry(String topic, T event) {
        return publishEvent(topic, event)
            .retryWhen(
                Retry.backoff(MAX_RETRY_ATTEMPTS, RETRY_BACKOFF)
                    .jitter(0.5) // 添加50%的抖動，避免重試風暴
                    .doBeforeRetry(retrySignal -> 
                        log.warn("重試發布事件 (第 {} 次): topic={}, event={}, 原因: {}", 
                            retrySignal.totalRetries() + 1, 
                            topic, 
                            event, 
                            retrySignal.failure().getMessage())
                    )
                    .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                        log.error("發布事件失敗，已達最大重試次數 {}: topic={}, event={}", 
                            MAX_RETRY_ATTEMPTS, topic, event);
                        return new EventPublishException(
                            String.format("發布事件失敗，已重試 %d 次: %s", 
                                MAX_RETRY_ATTEMPTS, 
                                retrySignal.failure().getMessage()),
                            retrySignal.failure()
                        );
                    })
            );
    }

    /**
     * 事件發布異常
     * 用於標識事件發布失敗，觸發事務回滾
     */
    public static class EventPublishException extends RuntimeException {
        public EventPublishException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * 發布使用者登入事件（帶重試）
     *
     * @param event 登入事件物件
     * @param <T>   事件的類型
     * @return 表示發布完成的 Mono<Void>
     */
    public <T> Mono<Void> publishLoginEvent(T event) {
        return publishEventWithRetry("user-login", event);
    }

    /**
     * 發布遊戲啟動事件（帶重試）
     *
     * @param event 遊戲啟動事件物件
     * @param <T>   事件的類型
     * @return 表示發布完成的 Mono<Void>
     */
    public <T> Mono<Void> publishGameLaunchEvent(T event) {
        return publishEventWithRetry("game-launch", event);
    }

    /**
     * 發布遊戲遊玩事件（帶重試）
     *
     * @param event 遊戲遊玩事件物件
     * @param <T>   事件的類型
     * @return 表示發布完成的 Mono<Void>
     */
    public <T> Mono<Void> publishGamePlayEvent(T event) {
        return publishEventWithRetry("game-play", event);
    }

    /**
     * 發布任務完成事件（帶重試）
     *
     * @param event 任務完成事件物件
     * @param <T>   事件的類型
     * @return 表示發布完成的 Mono<Void>
     */
    public <T> Mono<Void> publishMissionCompletedEvent(T event) {
        return publishEventWithRetry("mission-completed", event);
    }
}
