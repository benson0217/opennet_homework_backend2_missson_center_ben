package com.example.demo.shared.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 啟動遊戲請求 DTO
 */
public record LaunchGameRequest(
    @NotBlank(message = "使用者名稱不能為空")
    String username,
    @NotBlank(message = "遊戲代碼不能為空")
    String gameCode
) {
}
