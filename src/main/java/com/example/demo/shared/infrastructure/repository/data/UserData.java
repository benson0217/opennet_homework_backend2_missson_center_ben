package com.example.demo.shared.infrastructure.repository.data;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * 使用者持久化物件
 */
@Data
@Table("users")
public class UserData {

    @Id
    private Long id;

    private String username;

    private Integer points;

    private LocalDateTime registrationDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
