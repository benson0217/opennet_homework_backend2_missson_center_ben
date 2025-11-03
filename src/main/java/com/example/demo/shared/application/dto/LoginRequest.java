package com.example.demo.shared.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登入請求 DTO
 */
public record LoginRequest(
    @NotBlank(message = "使用者名稱不能為空")
    String username
) {
}
