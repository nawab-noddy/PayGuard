package com.system.PayGuard.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class IdempotencyService {

    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    public IdempotencyService(RedisTemplate<String,String> redisTemplate){
        this.redisTemplate = redisTemplate;
    }

    public boolean lockRequest(String idempotencyKey){
        String redisKey = "idemp:" + idempotencyKey;

        boolean isLocked = redisTemplate.opsForValue().setIfAbsent(
                redisKey,
                "LOCKED",
                Duration.ofHours(24)
        );
        return isLocked;
    }
}
