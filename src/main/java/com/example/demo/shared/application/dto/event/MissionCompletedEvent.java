package com.example.demo.shared.application.dto.event;

import java.time.LocalDateTime;

public record MissionCompletedEvent(
    Long userId,
    String username,
    Long missionId,
    String missionType,
    Integer rewardPoints,
    LocalDateTime completedAt
) {
}
