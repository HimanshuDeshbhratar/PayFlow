package com.payflow.redis;

import com.payflow.config.PayFlowProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;
    private final PayFlowProperties properties;

    public Optional<String> getCachedResponse(UUID merchantId, String idempotencyKey) {
        try {
            return Optional.ofNullable(redisTemplate.opsForValue().get(redisKey(merchantId, idempotencyKey)));
        } catch (Exception ex) {
            log.warn("Idempotency get failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public void storeResponse(UUID merchantId, String idempotencyKey, String responseJson) {
        try {
            redisTemplate.opsForValue().set(
                    redisKey(merchantId, idempotencyKey),
                    responseJson,
                    Duration.ofSeconds(properties.getIdempotency().getTtlSeconds())
            );
        } catch (Exception ex) {
            log.warn("Idempotency store failed: {}", ex.getMessage());
        }
    }

    public boolean markInProgress(UUID merchantId, String idempotencyKey) {
        try {
            Boolean set = redisTemplate.opsForValue().setIfAbsent(
                    redisKey(merchantId, idempotencyKey) + ":lock",
                    "1",
                    Duration.ofSeconds(30)
            );
            return Boolean.TRUE.equals(set);
        } catch (Exception ex) {
            log.warn("Idempotency lock failed: {}", ex.getMessage());
            return true;
        }
    }

    private String redisKey(UUID merchantId, String idempotencyKey) {
        return "idempotency:" + merchantId + ":" + idempotencyKey;
    }
}
