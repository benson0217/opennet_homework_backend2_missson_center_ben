package com.example.demo.mission.domain.model;

import lombok.Getter;

/**
 * 任務類型枚舉
 * 代表任務中心的三種任務類型。
 */
@Getter
public enum MissionType {
    /**
     * 連續登入3天
     */
    CONSECUTIVE_LOGIN("連續登入", 3),

    /**
     * 啟動至少3款不同的遊戲
     */
    LAUNCH_GAMES("遊戲啟動", 3),

    /**
     * 遊玩至少3局遊戲，且總分超過1000分
     */
    PLAY_GAMES("遊戲遊玩", 3);

    private final String description;
    private final int defaultTarget;

    /**
     * 建構子
     *
     * @param description   任務的中文描述
     * @param defaultTarget 任務的預設目標值
     */
    MissionType(String description, int defaultTarget) {
        this.description = description;
        this.defaultTarget = defaultTarget;
    }
}
