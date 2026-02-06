package service;

import java.time.Duration;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisNotificationQueueService {

    private final StringRedisTemplate redisTemplate;

    private static final String NOTIFICATION_QUEUE = "notification:queue";
    private static final String NOTIFICATION_RETRY = "notification:retry:";
    private static final String PUSH_NOTIFICATION = "notification:push:";
    private static final String EMAIL_NOTIFICATION = "notification:email:";
    private static final String SMS_NOTIFICATION = "notification:sms:";

    public RedisNotificationQueueService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ==================== NOTIFICATION QUEUE ====================

    public void enqueueNotification(String notificationJson) {
        redisTemplate.opsForList().rightPush(NOTIFICATION_QUEUE, notificationJson);
    }

    public String dequeueNotification() {
        return redisTemplate.opsForList().leftPop(NOTIFICATION_QUEUE);
    }

    public long getQueueSize() {
        Long size = redisTemplate.opsForList().size(NOTIFICATION_QUEUE);
        return size != null ? size : 0;
    }

    // ==================== PUSH NOTIFICATIONS ====================

    public void queuePushNotification(String userId, String title, String body) {
        String key = PUSH_NOTIFICATION + userId;
        String notification = title + "|" + body + "|" + System.currentTimeMillis();
        redisTemplate.opsForList().rightPush(key, notification);
        redisTemplate.expire(key, Duration.ofHours(24));
    }

    public List<String> getPushNotifications(String userId) {
        String key = PUSH_NOTIFICATION + userId;
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    public void clearPushNotifications(String userId) {
        String key = PUSH_NOTIFICATION + userId;
        redisTemplate.delete(key);
    }

    // ==================== EMAIL QUEUE ====================

    public void queueEmail(String to, String subject, String body) {
        String key = EMAIL_NOTIFICATION + to;
        String emailData = subject + "|" + body + "|" + System.currentTimeMillis();
        redisTemplate.opsForList().rightPush(key, emailData);
        redisTemplate.expire(key, Duration.ofHours(1));
    }

    public List<String> getPendingEmails(String to) {
        String key = EMAIL_NOTIFICATION + to;
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    // ==================== SMS QUEUE ====================

    public void queueSMS(String phoneNumber, String message) {
        String key = SMS_NOTIFICATION + phoneNumber;
        redisTemplate.opsForList().rightPush(key, message);
        redisTemplate.expire(key, Duration.ofHours(1));
    }

    public List<String> getPendingSMS(String phoneNumber) {
        String key = SMS_NOTIFICATION + phoneNumber;
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    // ==================== RETRY MECHANISM ====================

    public void markForRetry(String notificationId, int attempt) {
        String key = NOTIFICATION_RETRY + notificationId;
        redisTemplate.opsForValue().set(key, String.valueOf(attempt), Duration.ofHours(24));
    }

    public int getRetryCount(String notificationId) {
        String key = NOTIFICATION_RETRY + notificationId;
        String value = (String) redisTemplate.opsForValue().get(key);
        return value != null ? Integer.parseInt(value) : 0;
    }

    public void removeRetryMarker(String notificationId) {
        String key = NOTIFICATION_RETRY + notificationId;
        redisTemplate.delete(key);
    }
}
