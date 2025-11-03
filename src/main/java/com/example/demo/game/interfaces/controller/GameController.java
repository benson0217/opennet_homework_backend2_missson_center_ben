package com.example.demo.game.interfaces.controller;

import com.example.demo.game.application.service.GameCommandService;
import com.example.demo.shared.application.dto.ApiResponse;
import com.example.demo.shared.application.dto.LaunchGameRequest;
import com.example.demo.shared.application.dto.PlayGameRequest;
import com.example.demo.user.application.service.UserQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/games")
@Validated
@RequiredArgsConstructor
public class GameController {

    private final UserQueryService userQueryService;
    private final GameCommandService gameCommandService;

    @PostMapping("/launchGame")
    public Mono<ApiResponse<Void>> launchGame(@Valid @RequestBody LaunchGameRequest request) {
        log.info("啟動遊戲請求 - 使用者: {}, 遊戲: {}", request.username(), request.gameCode());

        return userQueryService.getUserByUsername(request.username())
            .flatMap(user -> gameCommandService.handleGameLaunch(user, request.gameCode()))
            .then(Mono.just(ApiResponse.<Void>success("遊戲啟動成功")))
            .onErrorResume(e -> {
                log.error("啟動遊戲失敗", e);
                return Mono.just(ApiResponse.error("啟動遊戲失敗: " + e.getMessage()));
            });
    }

    @PostMapping("/play")
    public Mono<ApiResponse<Void>> playGame(@Valid @RequestBody PlayGameRequest request) {
        log.info("遊玩遊戲請求 - 使用者: {}, 遊戲: {}, 分數: {}",
            request.username(), request.gameCode(), request.score());

        return userQueryService.getUserByUsername(request.username())
            .flatMap(user -> gameCommandService.handleGamePlay(
                    user,
                    request.gameCode(),
                    request.score(),
                    request.playDuration()
                )
            )
            .then(Mono.just(ApiResponse.<Void>success("遊戲記錄成功")))
            .onErrorResume(e -> {
                log.error("遊玩遊戲失敗", e);
                return Mono.just(ApiResponse.error("遊玩遊戲失敗: " + e.getMessage()));
            });
    }
}
