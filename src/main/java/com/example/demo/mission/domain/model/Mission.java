package com.example.demo.mission.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 任務聚合根
 * 代表使用者的任務，包含進度追蹤和獎勵邏輯。
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Mission {

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

    /**
     *
     * @param userId         使用者ID
     * @param missionType    任務類型
     * @param targetProgress 任務目標進度
     * @param rewardPoints   任務獎勵點數
     * @return 新建立的任務實體
     * @throws IllegalArgumentException 如果參數無效
     */
    public static Mission create(Long userId, MissionType missionType, int targetProgress, int rewardPoints) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("無效的使用者ID");
        }
        if (missionType == null) {
            throw new IllegalArgumentException("任務類型不能為空");
        }
        if (targetProgress <= 0) {
            throw new IllegalArgumentException("目標進度必須為正數");
        }

        Mission mission = new Mission();
        mission.userId = userId;
        mission.missionType = missionType;
        mission.currentProgress = 0;
        mission.targetProgress = targetProgress;
        mission.isCompleted = false;
        mission.isRewarded = false;
        mission.rewardPoints = rewardPoints;
        mission.createdAt = LocalDateTime.now();
        mission.updatedAt = LocalDateTime.now();
        return mission;
    }

    /**
     * 更新任務進度。
     *
     * @param newProgress 新的進度值
     * @return 如果任務更新時剛好完成，則返回 true，否則返回 false
     */
    public boolean updateProgress(int newProgress) {
        if (newProgress < 0) {
            throw new IllegalArgumentException("進度不能為負數");
        }

        boolean wasCompleted = this.isCompleted;
        this.currentProgress = newProgress;
        this.updatedAt = LocalDateTime.now();

        if (!wasCompleted && this.currentProgress >= this.targetProgress) {
            this.complete();
            return true;
        }

        return false;
    }

    /**
     * 將任務標記為已完成。
     */
    private void complete() {
    if (Boolean.FALSE.equals(this.isCompleted)) {
            this.isCompleted = true;
            this.completedAt = LocalDateTime.now();
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 將任務標記為已領取獎勵。
     *
     * @throws IllegalStateException 如果任務尚未完成或已經領取過獎勵
     */
    public void markAsRewarded() {
    if (Boolean.FALSE.equals(this.isCompleted)) {
            throw new IllegalStateException("不能為未完成的任務發放獎勵");
        }
    if (Boolean.TRUE.equals(this.isRewarded)) {
            throw new IllegalStateException("任務獎勵已被領取");
        }

        this.isRewarded = true;
        this.rewardedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 獲取任務進度的百分比。
     *
     * @return 進度百分比 (0.0 到 100.0)
     */
    public double getProgressPercentage() {
        if (targetProgress == 0) {
            return 100.0; // 如果目標是0，視為已完成
        }
        double progress = (double) this.currentProgress / this.targetProgress;
        return Math.min(100.0, progress * 100.0);
    }
}
