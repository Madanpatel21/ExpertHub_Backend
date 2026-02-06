package service;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class RedisService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String SESSION_PREFIX = "session:";
    private static final Long SESSION_TTL_HOURS = 24L;

    public RedisService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    // ==================== SESSION MANAGEMENT ====================

    public void storeSession(String sessionId, Map<String, Object> sessionData) {
        String key = SESSION_PREFIX + sessionId;
        try {
            String json = objectMapper.writeValueAsString(sessionData);
            redisTemplate.opsForValue().set(key, json, Duration.ofHours(SESSION_TTL_HOURS));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to store session", e);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getSession(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        String json = (String) redisTemplate.opsForValue().get(key);
        if (json == null)
            return null;
        try {
            return objectMapper.readValue(json, HashMap.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public void updateSession(String sessionId, String field, Object value) {
        Map<String, Object> session = getSession(sessionId);
        if (session != null) {
            session.put(field, value);
            storeSession(sessionId, session);
        }
    }

    public void deleteSession(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        redisTemplate.delete(key);
    }

    public boolean sessionExists(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void refreshSession(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        redisTemplate.expire(key, SESSION_TTL_HOURS, TimeUnit.HOURS);
    }
}
