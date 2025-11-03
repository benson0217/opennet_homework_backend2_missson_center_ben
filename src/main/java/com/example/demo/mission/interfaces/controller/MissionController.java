package com.example.demo.mission.interfaces.controller;

import com.example.demo.mission.application.service.MissionQueryService;
import com.example.demo.shared.application.dto.ApiResponse;
import com.example.demo.shared.application.dto.MissionResponse;
import com.example.demo.user.application.service.UserQueryService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/missions")
@Validated
@RequiredArgsConstructor
public class MissionController {

    private final UserQueryService userQueryService;
    private final MissionQueryService missionQueryService;

    /**
     * 獲取使用者任務列表。
     * 這是一個純粹的「讀」操作，它會優先從快取中獲取資料。
     *
     * @param username 使用者名稱
     * @return 包含任務列表的 API 回應
     */
    @GetMapping
    public Mono<ApiResponse<List<MissionResponse>>> getMissions(
        @NotBlank(message = "使用者名稱不能為空") @RequestParam String username) {
        log.info("取得使用者任務請求: {}", username);
        return userQueryService.getUserByUsername(username)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("找不到使用者: " + username)))
            .flatMap(user -> missionQueryService.getMissionsForUser(user.getId()))
            .map(missions -> ApiResponse.success("任務取得成功", missions))
            .onErrorResume(e -> {
                log.error("取得任務失敗", e);
                return Mono.just(ApiResponse.error("取得任務失敗: " + e.getMessage()));
            });
    }
}
