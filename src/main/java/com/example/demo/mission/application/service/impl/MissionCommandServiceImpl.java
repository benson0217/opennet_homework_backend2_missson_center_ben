package com.example.demo.mission.application.service.impl;

import com.example.demo.game.domain.repository.GameLaunchRecordRepository;
import com.example.demo.game.domain.repository.GamePlayRecordRepository;
import com.example.demo.mission.application.service.MissionCommandService;
import com.example.demo.mission.domain.model.Mission;
import com.example.demo.mission.domain.model.MissionType;
import com.example.demo.mission.domain.repository.MissionRepository;
import com.example.demo.shared.application.dto.event.MissionCompletedEvent;
import com.example.demo.shared.infrastructure.message.EventPublisher;
import com.example.demo.user.application.service.UserQueryService;
import com.example.demo.user.domain.repository.UserRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 任務命令服務
 * 負責處理所有與任務狀態變更相關的寫操作，例如初始化、進度更新和獎勵發放。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MissionCommandServiceImpl implements MissionCommandService {

    private final MissionRepository missionRepository;
    private final UserRepository userRepository;
    private final GameLaunchRecordRepository gameLaunchRecordRepository;
    private final GamePlayRecordRepository gamePlayRecordRepository;
    private final UserQueryService userQueryService;
    private final EventPublisher eventPublisher;

    @Value("${app.mission.consecutive-login-days:3}")
    private int consecutiveLoginDays;
    @Value("${app.mission.launch-games-count:3}")
    private int launchGamesCount;
    @Value("${app.mission.play-games-count:3}")
    private int playGamesCount;
    @Value("${app.mission.play-games-min-score:1000}")
    private int playGamesMinScore;
    @Value("${app.mission.completion-reward-points:777}")
    private int completionRewardPoints;

    /**
     * 如果使用者尚未初始化任務時則初始化所有類型任務。
     * 如果任務已存在則不執行任何操作。
     *
     * @param userId 使用者ID
     * @return 表示操作完成的 Mono<Void>
     */
    @Transactional
    @Override
    public Mono<Void> initializeMissions(Long userId) {
        return missionRepository.existsByUserIdAndMissionType(userId, MissionType.CONSECUTIVE_LOGIN)
            .flatMap(exists -> {
                if (Boolean.TRUE.equals(exists)) {
                    return Mono.empty();
                }
                log.info("為使用者 {} 初始化任務", userId);
                return Flux.fromArray(MissionType.values())
                    .flatMap(type -> createMission(userId, type))
                    .then();
            });
    }

    /**
     * 根據任務類型建立一個新的、未完成的任務。
     *
     * @param userId 使用者ID
     * @param type   任務類型
     * @return 包含已儲存任務的 Mono<Mission>
     */
    private Mono<Mission> createMission(Long userId, MissionType type) {
        int target = switch (type) {
            case CONSECUTIVE_LOGIN -> consecutiveLoginDays;
            case LAUNCH_GAMES -> launchGamesCount;
            case PLAY_GAMES -> playGamesCount;
        };
        Mission mission = Mission.create(userId, type, target, 0);
        return missionRepository.save(mission);
    }

    /**
     * 更新指定使用者的所有任務進度。
     * 觸發所有任務的進度檢查和更新。
     * 成功後會清除 "missions" 快取，並觸發獎勵檢查。
     *
     * @param userId 使用者ID
     * @return 表示操作完成的 Mono<Void>
     */
    @Transactional
    @CacheEvict(value = "missions", key = "#userId")
    @Override
    public Mono<Void> updateMissionProgress(Long userId, String userName) {
        log.info("為使用者 {} 更新任務進度並清除快取", userId);
        return Mono.when(updateConsecutiveLoginMission(userId), updateLaunchGamesMission(userId), updatePlayGamesMission(userId))
            .then(Mono.defer(() -> checkAndDistributeRewards(userId, userName)));
    }

    /**
     * 更新連續登入任務的進度。
     * 它會呼叫 UserQueryService 來獲取最新的連續登入天數。
     *
     * @param userId 使用者ID
     * @return 表示操作完成的 Mono<Void>
     */
    private Mono<Void> updateConsecutiveLoginMission(Long userId) {
        return Mono.zip(
            missionRepository.findByUserIdAndMissionType(userId, MissionType.CONSECUTIVE_LOGIN),
            userQueryService.getConsecutiveLoginDays(userId)
        ).flatMap(tuple -> {
            Mission mission = tuple.getT1();
            int consecutiveDays = tuple.getT2();
            boolean justCompleted = mission.updateProgress(consecutiveDays);
            return missionRepository.save(mission)
                .flatMap(savedMission -> justCompleted ? publishMissionCompletedEvent(userId, savedMission) : Mono.empty());
        }).then();
    }

    /**
     * 更新啟動不同遊戲任務的進度。
     *
     * @param userId 使用者ID
     * @return 表示操作完成的 Mono<Void>
     */
    private Mono<Void> updateLaunchGamesMission(Long userId) {
        return missionRepository.findByUserIdAndMissionType(userId, MissionType.LAUNCH_GAMES)
            .flatMap(mission ->
                gameLaunchRecordRepository.countDistinctGamesLaunchedByUser(userId)
                    .flatMap(count -> {
                        boolean justCompleted = mission.updateProgress(count.intValue());
                        return missionRepository.save(mission)
                            .flatMap(savedMission -> justCompleted ? publishMissionCompletedEvent(userId, savedMission) : Mono.empty());
                    })
            ).then();
    }

    /**
     * 更新遊玩遊戲任務的進度。
     * 綜合考慮遊玩次數和總分數。
     *
     * @param userId 使用者ID
     * @return 表示操作完成的 Mono<Void>
     */
    private Mono<Void> updatePlayGamesMission(Long userId) {
        return missionRepository.findByUserIdAndMissionType(userId, MissionType.PLAY_GAMES)
            .flatMap(mission ->
                Mono.zip(gamePlayRecordRepository.countByUserId(userId), gamePlayRecordRepository.sumScoreByUserId(userId))
                    .flatMap(tuple -> {
                        long playCount = tuple.getT1();
                        int totalScore = tuple.getT2();
                        int progress = calculatePlayGameProgress(playCount, totalScore);
                        boolean justCompleted = mission.updateProgress(progress);
                        return missionRepository.save(mission)
                            .flatMap(savedMission -> justCompleted ? publishMissionCompletedEvent(userId, savedMission) : Mono.empty());
                    })
            ).then();
    }

    /**
     * 根據遊玩次數和總分數，計算遊玩遊戲任務的進度。
     * @param playCount  遊玩次數
     * @param totalScore 總分數
     * @return 計算出的進度值
     */
    private int calculatePlayGameProgress(long playCount, int totalScore) {
        if (playCount >= playGamesCount && totalScore >= playGamesMinScore) {
            return playGamesCount; // 條件全部滿足，任務完成
        } else if (playCount >= playGamesCount) {
            return 2; // 次數達標但分數未達標，給予一個中間進度
        } else {
            return (int) playCount; // 次數未達標，進度等於當前次數
        }
    }

    /**
     * 檢查使用者是否完成了所有任務，如果是，則發放獎勵。
     * 成功後會清除 "users" 快取
     *
     * @param userId 使用者ID
     * @return 表示操作完成的 Mono<Void>
     */
    @CacheEvict(value = "users", key = "#userName")
    @Override
    public Mono<Void> checkAndDistributeRewards(Long userId, String userName) {
        return missionRepository.areAllMissionsCompleted(userId)
            .flatMap(allCompleted -> {
                if (Boolean.FALSE.equals(allCompleted)) {
                    return Mono.empty();
                }
                log.info("使用者 {} 的所有任務已完成，正在發放獎勵並清除使用者快取", userId);
                return userRepository.addPoints(userId, completionRewardPoints)
                    .then(markAllMissionsAsRewarded(userId));
            });
    }

    /**
     * 將指定使用者的所有已完成但未領取獎勵的任務，標記為已領取。
     *
     * @param userId 使用者ID
     * @return 表示操作完成的 Mono<Void>
     */
    private Mono<Void> markAllMissionsAsRewarded(Long userId) {
        return missionRepository.findByUserId(userId)
            .filter(mission -> mission.getIsCompleted() && !mission.getIsRewarded())
            .flatMap(mission -> {
                mission.markAsRewarded();
                return missionRepository.save(mission);
            })
            .then();
    }

    /**
     * 當單個任務完成時，發布一個 MissionCompletedEvent 事件。
     *
     * @param userId  使用者ID
     * @param mission 已完成的任務實體
     * @return 表示操作完成的 Mono<Void>
     */
    private Mono<Void> publishMissionCompletedEvent(Long userId, Mission mission) {
        return userRepository.findById(userId)
            .flatMap(user -> {
                MissionCompletedEvent event = new MissionCompletedEvent(user.getId(), user.getUsername(), mission.getId(),
                    mission.getMissionType().name(), mission.getRewardPoints(), LocalDateTime.now());
                return eventPublisher.publishMissionCompletedEvent(event)
                    .doOnSuccess(v -> log.info("已為使用者 {} 發布任務完成事件: {}", user.getUsername(), mission.getMissionType()))
                    .onErrorResume(e -> {
                        log.error("發布任務完成事件失敗", e);
                        return Mono.empty();
                    });
            });
    }
}
