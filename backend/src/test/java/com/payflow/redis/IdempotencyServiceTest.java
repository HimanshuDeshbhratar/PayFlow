package com.payflow.redis;

import com.payflow.config.PayFlowProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;

    IdempotencyService service;

    @BeforeEach
    void setUp() {
        PayFlowProperties props = new PayFlowProperties();
        props.getIdempotency().setTtlSeconds(3600);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        service = new IdempotencyService(redisTemplate, props);
    }

    @Test
    void storesAndReadsCachedResponse() {
        UUID merchantId = UUID.randomUUID();
        when(valueOps.get(contains("idempotency:"))).thenReturn("{\"id\":\"abc\"}");

        Optional<String> cached = service.getCachedResponse(merchantId, "key-1");
        assertThat(cached).contains("{\"id\":\"abc\"}");

        service.storeResponse(merchantId, "key-1", "{\"id\":\"abc\"}");
        verify(valueOps).set(contains("idempotency:"), eq("{\"id\":\"abc\"}"), any(Duration.class));
    }

    @Test
    void markInProgressUsesSetIfAbsent() {
        UUID merchantId = UUID.randomUUID();
        when(valueOps.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(true);
        assertThat(service.markInProgress(merchantId, "k")).isTrue();
    }
}
