package service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.Set;

@Service
public class UserStatusService {

    private final StringRedisTemplate redisTemplate;
    private static final String ONLINE_KEY_PREFIX = "user:online:";
    private static final Duration ONLINE_TIMEOUT = Duration.ofMinutes(5);

    public UserStatusService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void setUserOnline(String userId) {
        redisTemplate.opsForValue().set(ONLINE_KEY_PREFIX + userId, "online", ONLINE_TIMEOUT);
    }

    public void setUserOffline(String userId) {
        redisTemplate.delete(ONLINE_KEY_PREFIX + userId);
    }

    public boolean isUserOnline(String userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(ONLINE_KEY_PREFIX + userId));
    }

    public void heartbeat(String userId) {
        redisTemplate.expire(ONLINE_KEY_PREFIX + userId, ONLINE_TIMEOUT);
    }
}
