package service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;
    private static final String RATE_LIMIT_PREFIX = "ratelimit:";

    public RateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isAllowed(String action, String identifier, int limit, int windowMinutes) {
        String key = RATE_LIMIT_PREFIX + action + ":" + identifier;
        Long current = redisTemplate.opsForValue().increment(key);

        if (current == null)
            return false;

        if (current == 1) {
            redisTemplate.expire(key, windowMinutes, TimeUnit.MINUTES);
        }

        return current <= limit;
    }

    public void reset(String action, String identifier) {
        redisTemplate.delete(RATE_LIMIT_PREFIX + action + ":" + identifier);
    }
}
