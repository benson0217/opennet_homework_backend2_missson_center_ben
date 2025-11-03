package com.example.demo.shared.infrastructure.message.consumer;

import com.example.demo.shared.application.dto.event.MissionCompletedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 任務完成事件消費者
 * 監聽 task-center-mission-completed 主題的消息
 * 
 * 此消費者主要用於記錄任務完成的審計日誌。
 * 獎勵分發已由 MissionApplicationService.checkAndDistributeRewards() 處理。
 * 可在此擴展其他業務邏輯，例如：發送通知、觸發其他系統流程等。
 */
@Slf4j
@Component
@RocketMQMessageListener(
    topic = "task-center-mission-completed",
    consumerGroup = "task-center-mission-completed-consumer-group"
)
public class MissionCompletedEventConsumer implements RocketMQListener<String> {

    private final ObjectMapper objectMapper;

    public MissionCompletedEventConsumer() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void onMessage(String message) {
        try {
            MissionCompletedEvent event = objectMapper.readValue(message, MissionCompletedEvent.class);
            log.info("接收到任務完成事件: userId={}, username={}, missionId={}, missionType={}, rewardPoints={}, completedAt={}", 
                event.userId(), event.username(), event.missionId(), event.missionType(), 
                event.rewardPoints(), event.completedAt());
            
            // 在這裡處理業務邏輯
            // 例如：發送通知、更新積分、記錄成就等
            
        } catch (Exception e) {
            log.error("處理任務完成事件失敗: {}", message, e);
        }
    }
}
