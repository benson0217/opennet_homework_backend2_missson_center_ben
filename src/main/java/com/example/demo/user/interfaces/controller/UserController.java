package com.example.demo.user.interfaces.controller;

import com.example.demo.shared.application.dto.ApiResponse;
import com.example.demo.shared.application.dto.LoginRequest;
import com.example.demo.mission.application.service.MissionCommandService;
import com.example.demo.user.application.service.UserCommandService;
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
@RequestMapping("/api/users")
@Validated
@RequiredArgsConstructor
public class UserController {

    private final UserCommandService userCommandService;
    private final MissionCommandService missionCommandService;

    @PostMapping("/login")
    public Mono<ApiResponse<Void>> login(@Valid @RequestBody LoginRequest request) {
        log.info("使用者登入請求: {}", request.username());

        return userCommandService.handleLogin(request.username())
            .flatMap(user -> missionCommandService.initializeMissions(user.getId()))
            .then(Mono.just(ApiResponse.<Void>success("登入成功")))
            .onErrorResume(e -> {
                log.error("登入失敗", e);
                return Mono.just(ApiResponse.error("登入失敗: " + e.getMessage()));
            });
    }
}
