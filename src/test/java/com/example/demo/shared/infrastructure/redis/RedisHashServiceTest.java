package com.example.demo.shared.infrastructure.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisHashServiceTest {

    @Mock
    private ReactiveRedisOperations<String, Object> redisOperations;

    @Mock
    private ReactiveHashOperations<String, Object, Object> reactiveHashOperations;

    @InjectMocks
    private RedisService redisService;

    @BeforeEach
    void setUp() {
        lenient().when(redisOperations.opsForHash()).thenReturn(reactiveHashOperations);
    }

    @Test
    void get_shouldReturnValue_whenFieldExists() {
        String cacheKey = "testCache";
        String fieldKey = "field1";
        String expectedValue = "value1";

        when(reactiveHashOperations.get(cacheKey, fieldKey)).thenReturn(Mono.just(expectedValue));

        StepVerifier.create(redisService.get(cacheKey, fieldKey))
                .expectNext(expectedValue)
                .verifyComplete();
    }

    @Test
    void get_shouldReturnEmpty_whenFieldDoesNotExist() {
        String cacheKey = "testCache";
        String fieldKey = "nonExistentField";

        when(reactiveHashOperations.get(cacheKey, fieldKey)).thenReturn(Mono.empty());

        StepVerifier.create(redisService.get(cacheKey, fieldKey))
                .verifyComplete();
    }

    @Test
    void getAll_shouldReturnAllValues_whenCacheKeyExists() {
        String cacheKey = "testCache";
        List<String> expectedValues = Arrays.asList("value1", "value2", "value3");

        when(reactiveHashOperations.values(cacheKey)).thenReturn(Flux.fromIterable(expectedValues));

        StepVerifier.create(redisService.getAll(cacheKey))
                .expectNextSequence(expectedValues)
                .verifyComplete();
    }

    @Test
    void getAll_shouldReturnEmpty_whenCacheKeyDoesNotExist() {
        String cacheKey = "nonExistentCache";

        when(reactiveHashOperations.values(cacheKey)).thenReturn(Flux.empty());

        StepVerifier.create(redisService.getAll(cacheKey))
                .verifyComplete();
    }

    @Test
    void put_shouldReturnTrue_whenSuccessful() {
        String cacheKey = "testCache";
        String fieldKey = "field1";
        String value = "value1";

        when(reactiveHashOperations.put(cacheKey, fieldKey, value)).thenReturn(Mono.just(true));

        StepVerifier.create(redisService.put(cacheKey, fieldKey, value))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void put_shouldReturnFalse_whenUnsuccessful() {
        String cacheKey = "testCache";
        String fieldKey = "field1";
        String value = "value1";

        when(reactiveHashOperations.put(cacheKey, fieldKey, value)).thenReturn(Mono.just(false));

        StepVerifier.create(redisService.put(cacheKey, fieldKey, value))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void putAll_shouldReturnTrue_whenSuccessful() {
        String cacheKey = "testCache";
        Map<String, String> items = new HashMap<>();
        items.put("field1", "value1");
        items.put("field2", "value2");

        when(reactiveHashOperations.putAll(cacheKey, items)).thenReturn(Mono.just(true));

        StepVerifier.create(redisService.putAll(cacheKey, items))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void putAll_shouldReturnFalse_whenUnsuccessful() {
        String cacheKey = "testCache";
        Map<String, String> items = new HashMap<>();
        items.put("field1", "value1");
        items.put("field2", "value2");

        when(reactiveHashOperations.putAll(cacheKey, items)).thenReturn(Mono.just(false));

        StepVerifier.create(redisService.putAll(cacheKey, items))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void remove_shouldReturnNumberOfRemovedFields() {
        String cacheKey = "testCache";
        Object[] fieldKeys = {"field1", "field2"};
        Long expectedRemovedCount = 2L;

        when(reactiveHashOperations.remove(cacheKey, fieldKeys)).thenReturn(Mono.just(expectedRemovedCount));

        StepVerifier.create(redisService.remove(cacheKey, fieldKeys))
                .expectNext(expectedRemovedCount)
                .verifyComplete();
    }

    @Test
    void remove_shouldReturnZero_whenNoFieldsRemoved() {
        String cacheKey = "testCache";
        Object[] fieldKeys = {"nonExistentField"};
        Long expectedRemovedCount = 0L;

        when(reactiveHashOperations.remove(cacheKey, fieldKeys)).thenReturn(Mono.just(expectedRemovedCount));

        StepVerifier.create(redisService.remove(cacheKey, fieldKeys))
                .expectNext(expectedRemovedCount)
                .verifyComplete();
    }

    @Test
    void delete_shouldReturnNumberOfDeletedKeys() {
        String cacheKey = "testCache";
        Long expectedDeletedCount = 1L;

        when(redisOperations.delete(cacheKey)).thenReturn(Mono.just(expectedDeletedCount));

        StepVerifier.create(redisService.delete(cacheKey))
                .expectNext(expectedDeletedCount)
                .verifyComplete();
    }

    @Test
    void delete_shouldReturnZero_whenKeyDoesNotExist() {
        String cacheKey = "nonExistentCache";
        Long expectedDeletedCount = 0L;

        when(redisOperations.delete(cacheKey)).thenReturn(Mono.just(expectedDeletedCount));

        StepVerifier.create(redisService.delete(cacheKey))
                .expectNext(expectedDeletedCount)
                .verifyComplete();
    }
}
