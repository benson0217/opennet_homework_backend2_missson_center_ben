package com.example.demo.shared.application.dto.event;

import java.time.LocalDateTime;

public record GamePlayEvent(
    Long userId,
    String username,
    Long gameId,
    String gameCode,
    Integer score,
    Integer playDuration,
    LocalDateTime playTime
) {
}
