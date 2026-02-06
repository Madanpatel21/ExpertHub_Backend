package service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class tokenBlacklistService {
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    private static final String BLACKLIST_PREFIX = "token:blacklist:";

    public tokenBlacklistService(org.springframework.data.redis.core.StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void blacklistToken(String token, Date expiry) {
        if (token == null || token.isBlank())
            return;

        long diff = expiry.getTime() - System.currentTimeMillis();
        if (diff > 0) {
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "true", diff,
                    java.util.concurrent.TimeUnit.MILLISECONDS);
        }
    }

    public boolean isBlacklisted(String token) {
        if (token == null)
            return false;
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }
}
