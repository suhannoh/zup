package com.noh.zup.domain.collection.scheduler;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class CollectionRedisLock {

    private static final String LOCK_KEY_PREFIX = "collection:source-watch:lock:";

    private final StringRedisTemplate redisTemplate;

    public CollectionRedisLock(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Optional<CollectionLock> tryLock(Long sourceWatchId, Duration ttl) {
        String key = LOCK_KEY_PREFIX + sourceWatchId;
        String token = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, token, ttl);
        if (Boolean.TRUE.equals(acquired)) {
            return Optional.of(new CollectionLock(key, token));
        }
        return Optional.empty();
    }

    public void release(CollectionLock lock) {
        String currentToken = redisTemplate.opsForValue().get(lock.key());
        if (lock.token().equals(currentToken)) {
            redisTemplate.delete(lock.key());
        }
    }
}
