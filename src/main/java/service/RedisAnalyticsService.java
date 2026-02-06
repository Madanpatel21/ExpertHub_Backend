package service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisAnalyticsService {

    private final StringRedisTemplate redisTemplate;

    private static final String DAILY_ACTIVE_USERS = "analytics:dau:";
    private static final String MONTHLY_ACTIVE_USERS = "analytics:mau:";
    private static final String CALL_DURATION = "analytics:call:";
    private static final String PAYMENT_STATS = "analytics:payment:";
    private static final String APPOINTMENT_STATS = "analytics:appointment:";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public RedisAnalyticsService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ==================== DAILY ACTIVE USERS (DAU) ====================

    public void recordUserActivity(String userId) {
        String today = LocalDate.now().format(DATE_FORMATTER);
        String key = DAILY_ACTIVE_USERS + today;
        redisTemplate.opsForHyperLogLog().add(key, userId);
        redisTemplate.expire(key, Duration.ofDays(30));
    }

    public long getDailyActiveUsers() {
        String today = LocalDate.now().format(DATE_FORMATTER);
        String key = DAILY_ACTIVE_USERS + today;
        Long count = redisTemplate.opsForHyperLogLog().size(key);
        return count != null ? count : 0;
    }

    public long getDailyActiveUsers(String date) {
        String key = DAILY_ACTIVE_USERS + date;
        Long count = redisTemplate.opsForHyperLogLog().size(key);
        return count != null ? count : 0;
    }

    // ==================== MONTHLY ACTIVE USERS (MAU) ====================

    public void recordMonthlyActivity(String userId) {
        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String key = MONTHLY_ACTIVE_USERS + month;
        redisTemplate.opsForHyperLogLog().add(key, userId);
        redisTemplate.expire(key, Duration.ofDays(90));
    }

    public long getMonthlyActiveUsers() {
        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String key = MONTHLY_ACTIVE_USERS + month;
        Long count = redisTemplate.opsForHyperLogLog().size(key);
        return count != null ? count : 0;
    }

    // ==================== CALL STATISTICS ====================

    public void recordCallDuration(String userId, long seconds) {
        String today = LocalDate.now().format(DATE_FORMATTER);
        String key = CALL_DURATION + today + ":" + userId;
        redisTemplate.opsForValue().increment(key, seconds);
        redisTemplate.expire(key, Duration.ofDays(30));
    }

    public long getUserCallDurationToday(String userId) {
        String today = LocalDate.now().format(DATE_FORMATTER);
        String key = CALL_DURATION + today + ":" + userId;
        String value = (String) redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0;
    }

    public long getTotalCallDurationToday() {
        String today = LocalDate.now().format(DATE_FORMATTER);
        String pattern = CALL_DURATION + today + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys == null || keys.isEmpty())
            return 0;

        long total = 0;
        for (String key : keys) {
            String value = (String) redisTemplate.opsForValue().get(key);
            if (value != null)
                total += Long.parseLong(value);
        }
        return total;
    }

    // ==================== PAYMENT STATISTICS ====================

    public void recordPaymentSuccess() {
        String today = LocalDate.now().format(DATE_FORMATTER);
        String key = PAYMENT_STATS + today + ":success";
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, Duration.ofDays(90));
    }

    public void recordPaymentFailure() {
        String today = LocalDate.now().format(DATE_FORMATTER);
        String key = PAYMENT_STATS + today + ":failure";
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, Duration.ofDays(90));
    }

    public long getSuccessfulPaymentsToday() {
        String today = LocalDate.now().format(DATE_FORMATTER);
        String key = PAYMENT_STATS + today + ":success";
        String value = (String) redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0;
    }

    public long getFailedPaymentsToday() {
        String today = LocalDate.now().format(DATE_FORMATTER);
        String key = PAYMENT_STATS + today + ":failure";
        String value = (String) redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0;
    }

    // ==================== APPOINTMENT STATISTICS ====================

    public void recordNewAppointment() {
        String today = LocalDate.now().format(DATE_FORMATTER);
        String key = APPOINTMENT_STATS + today + ":new";
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, Duration.ofDays(90));
    }

    public void recordCompletedAppointment() {
        String today = LocalDate.now().format(DATE_FORMATTER);
        String key = APPOINTMENT_STATS + today + ":completed";
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, Duration.ofDays(90));
    }

    public void recordCancelledAppointment() {
        String today = LocalDate.now().format(DATE_FORMATTER);
        String key = APPOINTMENT_STATS + today + ":cancelled";
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, Duration.ofDays(90));
    }

    public long getNewAppointmentsToday() {
        String today = LocalDate.now().format(DATE_FORMATTER);
        String key = APPOINTMENT_STATS + today + ":new";
        String value = (String) redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0;
    }

    public long getCompletedAppointmentsToday() {
        String today = LocalDate.now().format(DATE_FORMATTER);
        String key = APPOINTMENT_STATS + today + ":completed";
        String value = (String) redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0;
    }
}
