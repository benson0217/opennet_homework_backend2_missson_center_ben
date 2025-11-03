package com.example.demo.shared.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * 任務回應 DTO
 * 用於顯示任務進度
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MissionResponse(
    Long id,
    String missionType,
    String description,
    Integer currentProgress,
    Integer targetProgress,
    Double progressPercentage,
    Boolean isCompleted,
    LocalDateTime completedAt,
    Boolean isRewarded,
    LocalDateTime rewardedAt,
    Integer rewardPoints
) {
}
