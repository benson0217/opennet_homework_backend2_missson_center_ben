package com.example.demo.shared.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * 通用的 Redis 操作服務
 * 封裝了 ReactiveRedisOperations，提供與業務無關的、可重用的 Redis 操作方法。
 */
@Service
@RequiredArgsConstructor
public class RedisService {

    private final ReactiveRedisOperations<String, Object> redisOperations;

    // Hash Operations
    public <K, V> Mono<V> get(String cacheKey, K fieldKey) {
        return redisOperations.<K, V>opsForHash().get(cacheKey, fieldKey);
    }

    public <V> Flux<V> getAll(String cacheKey) {
        return redisOperations.<String, V>opsForHash().values(cacheKey);
    }

    public <K, V> Mono<Boolean> put(String cacheKey, K fieldKey, V value) {
        return redisOperations.opsForHash().put(cacheKey, fieldKey, value);
    }

    public <K, V> Mono<Boolean> putAll(String cacheKey, Map<K, V> items) {
        return redisOperations.opsForHash().putAll(cacheKey, items);
    }

    public Mono<Long> remove(String cacheKey, Object... fieldKeys) {
        return redisOperations.opsForHash().remove(cacheKey, fieldKeys);
    }

    // General Key Operations
    public Mono<Long> delete(String cacheKey) {
        return redisOperations.delete(cacheKey);
    }

    /**
     * 嘗試設定一個鍵值對，只有當鍵不存在時才成功 (set if absent)。
     * 用於實現幂等性或分散式鎖。
     *
     * @param key 鍵
     * @param value 值
     * @param ttl 過期時間
     * @return 如果鍵被成功設定則返回 true，否則返回 false。
     */
    public Mono<Boolean> setIfAbsent(String key, String value, Duration ttl) {
        return redisOperations.opsForValue().setIfAbsent(key, value, ttl);
    }
}
