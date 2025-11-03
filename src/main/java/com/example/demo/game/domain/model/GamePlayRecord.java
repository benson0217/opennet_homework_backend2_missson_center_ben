package com.example.demo.game.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 遊戲遊玩記錄實體
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class GamePlayRecord {
    
    private Long id;
    
    private Long userId;
    
    private Long gameId;
    
    private Integer score;
    
    private Integer playDuration;
    
    private LocalDateTime playTime;
    
    private LocalDateTime createdAt;

    /**
     * 建立一個新的遊戲遊玩記錄。
     *
     * @param userId       使用者ID
     * @param gameId       遊戲ID
     * @param score        玩遊戲的分數
     * @param playDuration 玩遊戲的時間（秒）
     * @return 新建立的遊戲遊玩記錄實體
     * @throws IllegalArgumentException 如果使用者ID、遊戲ID無效或分數為負數
     */
    public static GamePlayRecord create(Long userId, Long gameId, int score, Integer playDuration) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("無效的使用者ID");
        }
        if (gameId == null || gameId <= 0) {
            throw new IllegalArgumentException("無效的遊戲ID");
        }
        if (score < 0) {
            throw new IllegalArgumentException("分數不能為負數");
        }
        
        GamePlayRecord gamePlayRecord = new GamePlayRecord();
        gamePlayRecord.userId = userId;
        gamePlayRecord.gameId = gameId;
        gamePlayRecord.score = score;
        gamePlayRecord.playDuration = playDuration;
        gamePlayRecord.playTime = LocalDateTime.now();
        gamePlayRecord.createdAt = LocalDateTime.now();
        return gamePlayRecord;
    }
}
