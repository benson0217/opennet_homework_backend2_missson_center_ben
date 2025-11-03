package com.example.demo.shared.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 遊玩遊戲請求 DTO
 */
public record PlayGameRequest(
    @NotBlank(message = "使用者名稱不能為空")
    String username,
    @NotBlank(message = "遊戲代碼不能為空")
    String gameCode,
    @NotNull(message = "分數不能為空")
    @Min(value = 0, message = "分數必須為非負數")
    Integer score,
    @Min(value = 0, message = "遊玩時長必須為非負數")
    Integer playDuration
) {
}
