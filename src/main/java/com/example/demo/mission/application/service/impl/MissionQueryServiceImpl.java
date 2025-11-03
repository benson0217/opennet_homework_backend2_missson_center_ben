package com.example.demo.mission.application.service.impl;

import com.example.demo.mission.application.service.MissionQueryService;
import com.example.demo.mission.domain.repository.MissionRepository;
import com.example.demo.shared.application.converter.MissionMapper;
import com.example.demo.shared.application.dto.MissionResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 任務查詢服務
 * 提供所有與任務相關的查詢功能。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MissionQueryServiceImpl implements MissionQueryService {

    private final MissionRepository missionRepository;
    private final MissionMapper missionMapper;

    /**
     * 根據使用者ID獲取任務列表，並將其轉換為 DTO。
     * 此方法的結果會被快取。
     *
     * @param userId 使用者ID
     * @return 包含任務回應 DTO 列表的 Mono
     */
    @Cacheable(value = "missions", key = "#userId")
    @Override
    public Mono<List<MissionResponse>> getMissionsForUser(Long userId) {
        log.debug("從資料庫為使用者 {} ", userId);
        return missionRepository.findByUserId(userId)
            .map(missionMapper::toResponse)
            .collectList();
    }
}
