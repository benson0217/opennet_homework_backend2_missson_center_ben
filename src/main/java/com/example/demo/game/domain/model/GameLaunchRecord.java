package com.example.demo.game.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 遊戲啟動記錄實體
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class GameLaunchRecord {
    
    private Long id;
    
    private Long userId;
    
    private Long gameId;
    
    private LocalDateTime launchTime;
    
    private LocalDateTime createdAt;

    /**
     * 建立一個新的遊戲啟動記錄。
     *
     * @param userId 使用者ID
     * @param gameId 遊戲ID
     * @return 新建立的遊戲啟動記錄實體
     * @throws IllegalArgumentException 如果使用者ID或遊戲ID無效
     */
    public static GameLaunchRecord create(Long userId, Long gameId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("無效的使用者ID");
        }
        if (gameId == null || gameId <= 0) {
            throw new IllegalArgumentException("無效的遊戲ID");
        }
        
        GameLaunchRecord gameLaunchRecord = new GameLaunchRecord();
        gameLaunchRecord.userId = userId;
        gameLaunchRecord.gameId = gameId;
        gameLaunchRecord.launchTime = LocalDateTime.now();
        gameLaunchRecord.createdAt = LocalDateTime.now();
        return gameLaunchRecord;
    }
}
