package com.example.demo.user.application.service;

import com.example.demo.user.domain.model.User;
import reactor.core.publisher.Mono;

public interface UserCommandService {

    Mono<User> handleLogin(String username);
}
