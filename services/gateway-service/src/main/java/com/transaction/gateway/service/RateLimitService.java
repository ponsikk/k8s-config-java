package com.transaction.gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${ratelimit.max-requests:1000}")
    private int maxRequests;

    @Value("${ratelimit.window-seconds:60}")
    private int windowSeconds;

    public boolean allowRequest(String userId) {
        try {
            String key = "ratelimit:user:" + userId;
            Long currentCount = redisTemplate.opsForValue().increment(key);

            if (currentCount == null) {
                return true;
            }

            if (currentCount == 1) {
                // Set expiration on first request
                redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
            }

            if (currentCount > maxRequests) {
                log.warn("Rate limit exceeded for user: {} (count: {})", userId, currentCount);
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("Error checking rate limit for user: {}", userId, e);
            // Fail open - allow request if Redis is down
            return true;
        }
    }
}
