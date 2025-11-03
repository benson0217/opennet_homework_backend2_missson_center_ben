package com.example.demo.shared.application.dto.event;

import java.time.LocalDateTime;

public record UserLoginEvent(
    Long userId,
    String username,
    LocalDateTime loginTime
) {
}
