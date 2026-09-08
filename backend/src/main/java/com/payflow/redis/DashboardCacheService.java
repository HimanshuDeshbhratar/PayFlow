package com.payflow.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public <T> T getOrCompute(String key, Class<T> type, Duration ttl, Supplier<T> supplier) {
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey(key));
            if (cached != null) {
                return objectMapper.readValue(cached, type);
            }
        } catch (Exception ex) {
            log.debug("Dashboard cache miss/error: {}", ex.getMessage());
        }

        T value = supplier.get();
        try {
            redisTemplate.opsForValue().set(cacheKey(key), objectMapper.writeValueAsString(value), ttl);
        } catch (JsonProcessingException ex) {
            log.debug("Dashboard cache write failed: {}", ex.getMessage());
        }
        return value;
    }

    public void evict(String key) {
        try {
            redisTemplate.delete(cacheKey(key));
        } catch (Exception ignored) {
            // best-effort
        }
    }

    private String cacheKey(String key) {
        return "dashboard:" + key;
    }
}
