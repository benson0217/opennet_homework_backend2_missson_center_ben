package com.example.demo.mission.application.service;

import com.example.demo.shared.application.dto.MissionResponse;
import java.util.List;
import reactor.core.publisher.Mono;

public interface MissionQueryService {

    Mono<List<MissionResponse>> getMissionsForUser(Long userId);
}
