package com.example.demo.game.application.service;

import com.example.demo.user.domain.model.User;
import reactor.core.publisher.Mono;

public interface GameCommandService {

    Mono<Void> handleGameLaunch(User user, String gameCode);

    Mono<Void> handleGamePlay(User user, String gameCode, int score, Integer playDuration);
}
