package com.example.demo.shared.infrastructure.repository.data;

import com.example.demo.mission.domain.model.MissionType;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * 任務持久化物件
 */
@Data
@Table("missions")
public class MissionData {

    @Id
    private Long id;

    private Long userId;

    private MissionType missionType;

    private Integer currentProgress;

    private Integer targetProgress;

    private Boolean isCompleted;

    private LocalDateTime completedAt;

    private Boolean isRewarded;

    private LocalDateTime rewardedAt;

    private Integer rewardPoints;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
