package com.example.demo.shared.infrastructure.repository.data;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * 遊戲持久化物件
 */
@Data
@Table("games")
public class GameData {

    @Id
    private Long id;

    private String gameCode;

    private String gameName;

    private String description;

    private Boolean isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
