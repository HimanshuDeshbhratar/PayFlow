package com.payflow.redis;

import com.payflow.config.PayFlowProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;
    private final PayFlowProperties properties;

    public boolean tryConsume(String key, int limitPerMinute) {
        try {
            String redisKey = "ratelimit:" + key;
            Long count = redisTemplate.opsForValue().increment(redisKey);
            if (count != null && count == 1L) {
                redisTemplate.expire(redisKey, Duration.ofMinutes(1));
            }
            return count == null || count <= limitPerMinute;
        } catch (Exception ex) {
            log.warn("Rate limit check failed, allowing request: {}", ex.getMessage());
            return true;
        }
    }

    public boolean tryLogin(String ip) {
        return tryConsume("login:" + ip, properties.getRateLimit().getLoginPerMinute());
    }

    public boolean tryPayment(String key) {
        return tryConsume("payment:" + key, properties.getRateLimit().getPaymentPerMinute());
    }
}
