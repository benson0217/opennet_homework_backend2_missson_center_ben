package com.example.demo.user.application.service.impl;

import com.example.demo.user.application.service.UserQueryService;
import com.example.demo.user.domain.model.User;
import com.example.demo.user.domain.repository.LoginRecordRepository;
import com.example.demo.user.domain.repository.UserRepository;
import com.example.demo.user.domain.service.UserDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserQueryServiceImpl implements UserQueryService {

    private final UserRepository userRepository;
    private final LoginRecordRepository loginRecordRepository;
    private final UserDomainService userDomainService;

    @Cacheable(value = "users", key = "#username")
    @Override
    public Mono<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("找不到使用者: " + username)));
    }

    @Override
    public Mono<User> findUserByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("找不到使用者: " + userId)));
    }

    @Override
    public Mono<Integer> getConsecutiveLoginDays(Long userId) {
        return loginRecordRepository.findRecentByUserId(userId, 10)
            .collectList()
            .map(userDomainService::calculateConsecutiveLoginDays);
    }
}
