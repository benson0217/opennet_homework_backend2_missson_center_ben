package com.example.demo.shared.infrastructure.repository.data;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * 遊戲遊玩記錄持久化物件
 */
@Data
@Table("games_play_record")
public class GamePlayRecordData {

    @Id
    private Long id;

    private Long userId;

    private Long gameId;

    private Integer score;

    private Integer playDuration;

    private LocalDateTime playTime;

    private LocalDateTime createdAt;
}
