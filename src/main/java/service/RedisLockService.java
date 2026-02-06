package service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisLockService {

    private final StringRedisTemplate redisTemplate;

    private static final String LOCK_PREFIX = "lock:";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    public RedisLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ==================== DISTRIBUTED LOCKING ====================

    /**
     * Acquire a lock
     * 
     * @param lockName Unique lock identifier
     * @param ownerId  Owner of the lock (e.g., userId)
     * @param timeout  Lock timeout duration
     * @return true if lock acquired, false otherwise
     */
    public boolean acquireLock(String lockName, String ownerId, Duration timeout) {
        String key = LOCK_PREFIX + lockName;
        Boolean result = redisTemplate.opsForValue()
                .setIfAbsent(key, ownerId, timeout);
        return Boolean.TRUE.equals(result);
    }

    /**
     * Acquire a lock with default timeout (30 seconds)
     */
    public boolean acquireLock(String lockName, String ownerId) {
        return acquireLock(lockName, ownerId, DEFAULT_TIMEOUT);
    }

    /**
     * Try to acquire lock with wait time
     * 
     * @param lockName     Unique lock identifier
     * @param ownerId      Owner of the lock
     * @param waitTime     Maximum time to wait for lock
     * @param lockDuration How long to hold the lock
     * @return true if lock acquired
     */
    public boolean tryLock(String lockName, String ownerId, Duration waitTime, Duration lockDuration) {
        String key = LOCK_PREFIX + lockName;
        try {
            Boolean result = redisTemplate.opsForValue()
                    .setIfAbsent(key, ownerId, lockDuration);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Release a lock (only if owner matches)
     */
    public boolean releaseLock(String lockName, String ownerId) {
        String key = LOCK_PREFIX + lockName;
        String currentOwner = (String) redisTemplate.opsForValue().get(key);
        if (ownerId.equals(currentOwner)) {
            Boolean deleted = redisTemplate.delete(key);
            return Boolean.TRUE.equals(deleted);
        }
        return false;
    }

    /**
     * Force release a lock (use with caution)
     */
    public boolean forceReleaseLock(String lockName) {
        String key = LOCK_PREFIX + lockName;
        Boolean deleted = redisTemplate.delete(key);
        return Boolean.TRUE.equals(deleted);
    }

    /**
     * Check if lock is held
     */
    public boolean isLocked(String lockName) {
        String key = LOCK_PREFIX + lockName;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Get lock owner
     */
    public String getLockOwner(String lockName) {
        String key = LOCK_PREFIX + lockName;
        return (String) redisTemplate.opsForValue().get(key);
    }

    /**
     * Extend lock timeout
     */
    public boolean extendLock(String lockName, String ownerId, Duration extension) {
        String key = LOCK_PREFIX + lockName;
        String currentOwner = (String) redisTemplate.opsForValue().get(key);
        if (ownerId.equals(currentOwner)) {
            return Boolean.TRUE.equals(
                    redisTemplate.expire(key, extension));
        }
        return false;
    }

    // ==================== SPECIFIC LOCKS ====================

    /**
     * Prevent double booking of appointment slots
     */
    public boolean acquireAppointmentLock(Long appointmentId, String userId) {
        return acquireLock("appointment:" + appointmentId, userId, Duration.ofMinutes(5));
    }

    /**
     * Prevent concurrent payment processing
     */
    public boolean acquirePaymentLock(Long appointmentId, String userId) {
        return acquireLock("payment:" + appointmentId, userId, Duration.ofMinutes(10));
    }

    /**
     * Prevent duplicate reviews
     */
    public boolean acquireReviewLock(Long lawyerId, String userId) {
        return acquireLock("review:" + lawyerId + ":" + userId, userId, Duration.ofMinutes(2));
    }

    /**
     * Exclusive profile update
     */
    public boolean acquireProfileLock(Long userId) {
        return acquireLock("profile:" + userId, userId.toString(), Duration.ofMinutes(2));
    }
}
