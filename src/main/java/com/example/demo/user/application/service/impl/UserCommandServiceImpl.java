package com.example.demo.user.application.service.impl;

import com.example.demo.shared.application.dto.event.UserLoginEvent;
import com.example.demo.shared.infrastructure.message.EventPublisher;
import com.example.demo.user.application.service.UserCommandService;
import com.example.demo.user.domain.model.LoginRecord;
import com.example.demo.user.domain.model.User;
import com.example.demo.user.domain.repository.LoginRecordRepository;
import com.example.demo.user.domain.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCommandServiceImpl implements UserCommandService {

    private final UserRepository userRepository;
    private final LoginRecordRepository loginRecordRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    @CachePut(value = "users", key = "#username")
    @Override
    public Mono<User> handleLogin(String username) {
        log.info("處理使用者登入: {}", username);
        return userRepository.findByUsername(username)
            .switchIfEmpty(createNewUser(username))
            .flatMap(user -> recordLogin(user).thenReturn(user))
            .flatMap(this::publishLoginEvent)
            .doOnError(e -> log.error("登入處理失敗，事務將回滾: username={}, 錯誤: {}", 
                username, e.getMessage()));
    }

    private Mono<User> createNewUser(String username) {
        log.info("建立新使用者: {}", username);
        User newUser = User.create(username);
        return userRepository.save(newUser);
    }

    private Mono<Void> recordLogin(User user) {
        LocalDate today = LocalDate.now();
        return loginRecordRepository.existsByUserIdAndLoginDate(user.getId(), today)
            .flatMap(exists -> {
                if (Boolean.TRUE.equals(exists)) {
                    log.debug("使用者 {} 今日已登入過", user.getId());
                    return Mono.empty();
                }
                log.debug("記錄使用者 {} 的登入記錄", user.getId());
                LoginRecord loginRecord = LoginRecord.createForToday(user.getId());
                return loginRecordRepository.save(loginRecord).then();
            });
    }

    /**
     * 發布登入事件
     * 如果使用者符合任務資格（註冊30天內），則發布事件。
     */
    private Mono<User> publishLoginEvent(User user) {
        if (user.isEligibleForMissions()) {
            log.debug("準備發布登入事件: userId={}, username={}", user.getId(), user.getUsername());
            UserLoginEvent event = new UserLoginEvent(user.getId(), user.getUsername(), LocalDateTime.now());

            return eventPublisher.publishLoginEvent(event)
                .doOnSuccess(v -> log.info("成功發布登入事件: userId={}, username={}", 
                    user.getId(), user.getUsername()))
                .doOnError(e -> log.error("發布登入事件失敗，將觸發回滾: userId={}, username={}, 錯誤: {}",
                    user.getId(), user.getUsername(), e.getMessage()))
                .thenReturn(user);
        } else {
            log.debug("使用者 {} 不符合任務資格（註冊超過30天），跳過發布登入事件。", user.getUsername());
            return Mono.just(user);
        }
    }
}
