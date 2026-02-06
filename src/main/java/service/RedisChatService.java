package service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisChatService {

    private final StringRedisTemplate redisTemplate;

    private static final String CHAT_MESSAGES = "chat:messages:";
    private static final String TYPING_INDICATOR = "chat:typing:";
    private static final String READ_RECEIPT = "chat:read:";
    private static final int MESSAGE_LIMIT = 100;
    private static final Duration MESSAGE_TTL = Duration.ofDays(7);
    private static final Duration TYPING_TTL = Duration.ofSeconds(5);

    public RedisChatService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ==================== MESSAGE CACHING ====================

    public void cacheMessage(Long appointmentId, String messageJson) {
        String key = CHAT_MESSAGES + appointmentId;
        redisTemplate.opsForList().leftPush(key, messageJson);
        redisTemplate.opsForList().trim(key, 0, MESSAGE_LIMIT - 1);
        redisTemplate.expire(key, MESSAGE_TTL);
    }

    public List<String> getRecentMessages(Long appointmentId) {
        String key = CHAT_MESSAGES + appointmentId;
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    public void deleteChatHistory(Long appointmentId) {
        String key = CHAT_MESSAGES + appointmentId;
        redisTemplate.delete(key);
    }

    public long getMessageCount(Long appointmentId) {
        String key = CHAT_MESSAGES + appointmentId;
        Long count = redisTemplate.opsForList().size(key);
        return count != null ? count : 0;
    }

    // ==================== TYPING INDICATORS ====================

    public void setTyping(Long appointmentId, String userId) {
        String key = TYPING_INDICATOR + appointmentId;
        redisTemplate.opsForSet().add(key, userId);
        redisTemplate.expire(key, TYPING_TTL);
    }

    public void stopTyping(Long appointmentId, String userId) {
        String key = TYPING_INDICATOR + appointmentId;
        redisTemplate.opsForSet().remove(key, userId);
    }

    public Set<String> getTypingUsers(Long appointmentId) {
        String key = TYPING_INDICATOR + appointmentId;
        return redisTemplate.opsForSet().members(key);
    }

    public boolean isTyping(Long appointmentId, String userId) {
        String key = TYPING_INDICATOR + appointmentId;
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, userId));
    }

    // ==================== READ RECEIPTS ====================

    public void markAsRead(Long appointmentId, String userId, long messageIndex) {
        String key = READ_RECEIPT + appointmentId + ":" + userId;
        redisTemplate.opsForValue().set(key, String.valueOf(messageIndex));
        redisTemplate.expire(key, Duration.ofDays(30));
    }

    public long getLastReadIndex(Long appointmentId, String userId) {
        String key = READ_RECEIPT + appointmentId + ":" + userId;
        String value = (String) redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : -1;
    }

    // ==================== ONLINE STATUS ====================

    public void setUserOnline(String userId) {
        String key = "user:online:" + userId;
        redisTemplate.opsForValue().set(key, "online", Duration.ofMinutes(5));
    }

    public void setUserOffline(String userId) {
        String key = "user:online:" + userId;
        redisTemplate.delete(key);
    }

    public boolean isUserOnline(String userId) {
        String key = "user:online:" + userId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public Set<String> getOnlineUsers(Set<String> userIds) {
        return new java.util.HashSet<>();
    }
}
