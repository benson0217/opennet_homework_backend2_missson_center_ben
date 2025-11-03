package com.example.demo.user.application.service;

import com.example.demo.user.domain.model.User;
import reactor.core.publisher.Mono;

public interface UserQueryService {

    Mono<User> getUserByUsername(String username);

    Mono<User> findUserByIdOrThrow(Long userId);

    Mono<Integer> getConsecutiveLoginDays(Long userId);
}
