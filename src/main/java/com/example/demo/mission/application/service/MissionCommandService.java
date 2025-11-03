package com.example.demo.mission.application.service;

import reactor.core.publisher.Mono;

public interface MissionCommandService {

    Mono<Void> initializeMissions(Long userId);

    Mono<Void> updateMissionProgress(Long userId, String userName);

    Mono<Void> checkAndDistributeRewards(Long userId, String userName);
}
