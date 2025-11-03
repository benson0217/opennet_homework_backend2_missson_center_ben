package com.example.demo.game.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 遊戲聚合根
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Game {

    private Long id;

    private String gameCode;

    private String gameName;

    private String description;

    private Boolean isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 建立一個新的遊戲實體。
     *
     * @param gameCode    遊戲的唯一代碼
     * @param gameName    遊戲的名稱
     * @param description 遊戲的描述
     * @return 新建立的遊戲實體
     * @throws IllegalArgumentException 如果遊戲代碼或名稱為空
     */
    public static Game create(String gameCode, String gameName, String description) {
        if (gameCode == null || gameCode.trim().isEmpty()) {
            throw new IllegalArgumentException("遊戲代碼不能為空");
        }
        if (gameName == null || gameName.trim().isEmpty()) {
            throw new IllegalArgumentException("遊戲名稱不能為空");
        }

        Game game = new Game();
        game.gameCode = gameCode.trim();
        game.gameName = gameName.trim();
        game.description = description;
        game.isActive = true;
        game.createdAt = LocalDateTime.now();
        game.updatedAt = LocalDateTime.now();
        return game;
    }

    /**
     * 檢查遊戲是否啟用。
     *
     * @return 如果遊戲為啟用狀態，則返回 true
     */
    public boolean isAvailable() {
        return this.isActive == null || !this.isActive;
    }
}
