package com.example.demo.shared.infrastructure.repository.data;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 登入記錄持久化物件
 */
@Data
@Table("login_record")
public class LoginRecordData {

    @Id
    private Long id;

    private Long userId;

    private LocalDate loginDate;

    private LocalDateTime loginTime;

    private LocalDateTime createdAt;
}
