package com.example.demo.shared.infrastructure.repository.data;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * 遊戲啟動記錄持久化物件
 */
@Data
@Table("game_launch_record")
public class GameLaunchRecordData {

    @Id
    private Long id;

    private Long userId;

    private Long gameId;

    private LocalDateTime launchTime;

    private LocalDateTime createdAt;
}
