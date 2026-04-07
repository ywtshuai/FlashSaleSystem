package com.flashsale.order.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class SeckillRedisStatusService {

    private static final Duration RESULT_TTL = Duration.ofHours(6);

    private final StringRedisTemplate redisTemplate;

    public void markSuccess(Long userId, Long productId, Long orderId) {
        redisTemplate.opsForValue().set(resultKey(userId, productId), "SUCCESS:" + orderId, RESULT_TTL);
    }

    public void markFailed(Long userId, Long productId, String reason) {
        redisTemplate.opsForValue().set(resultKey(userId, productId), "FAIL:" + reason, RESULT_TTL);
    }

    public void rollbackReservation(Long userId, Long productId) {
        redisTemplate.opsForSet().remove(usersKey(productId), String.valueOf(userId));
        redisTemplate.opsForValue().increment(stockKey(productId));
    }

    private String stockKey(Long productId) {
        return "seckill:stock:" + productId;
    }

    private String usersKey(Long productId) {
        return "seckill:users:" + productId;
    }

    private String resultKey(Long userId, Long productId) {
        return "seckill:result:" + userId + ":" + productId;
    }
}
