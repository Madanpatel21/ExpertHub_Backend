package service;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisLeaderboardService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String TOP_RATED_LAWYERS = "leaderboard:top-rated";
    private static final String MOST_CONSULTATIONS = "leaderboard:consultations";
    private static final String TOP_REVIEWERS = "leaderboard:reviewers";

    public RedisLeaderboardService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ==================== TOP RATED LAWYERS ====================

    public void updateLawyerRating(Long lawyerId, double rating) {
        String key = TOP_RATED_LAWYERS;
        redisTemplate.opsForZSet().add(key, lawyerId.toString(), rating);
    }

    public List<String> getTopRatedLawyers(int limit) {
        Set<Object> top = redisTemplate.opsForZSet()
                .reverseRange(TOP_RATED_LAWYERS, 0, limit - 1);
        if (top == null)
            return List.of();
        return top.stream()
                .map(Object::toString)
                .toList();
    }

    public void removeLawyerFromRating(Long lawyerId) {
        redisTemplate.opsForZSet().remove(TOP_RATED_LAWYERS, lawyerId.toString());
    }

    // ==================== MOST CONSULTATIONS ====================

    public void incrementConsultationCount(Long lawyerId) {
        redisTemplate.opsForZSet().incrementScore(MOST_CONSULTATIONS, lawyerId.toString(), 1);
    }

    public void setConsultationCount(Long lawyerId, long count) {
        redisTemplate.opsForZSet().add(MOST_CONSULTATIONS, lawyerId.toString(), count);
    }

    public List<String> getMostConsultedLawyers(int limit) {
        Set<Object> top = redisTemplate.opsForZSet()
                .reverseRange(MOST_CONSULTATIONS, 0, limit - 1);
        if (top == null)
            return List.of();
        return top.stream()
                .map(Object::toString)
                .toList();
    }

    public long getConsultationCount(Long lawyerId) {
        Double score = redisTemplate.opsForZSet().score(MOST_CONSULTATIONS, lawyerId.toString());
        return score != null ? score.longValue() : 0;
    }

    // ==================== TOP REVIEWERS ====================

    public void incrementReviewCount(Long userId) {
        redisTemplate.opsForZSet().incrementScore(TOP_REVIEWERS, userId.toString(), 1);
    }

    public List<String> getTopReviewers(int limit) {
        Set<Object> top = redisTemplate.opsForZSet()
                .reverseRange(TOP_REVIEWERS, 0, limit - 1);
        if (top == null)
            return List.of();
        return top.stream()
                .map(Object::toString)
                .toList();
    }

    // ==================== LAWYER STATS ====================

    public void incrementLawyerView(Long lawyerId) {
        String key = "lawyer:views:" + lawyerId;
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, Duration.ofDays(30));
    }

    public long getLawyerViews(Long lawyerId) {
        String key = "lawyer:views:" + lawyerId;
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value.toString()) : 0;
    }

    public void setLawyerReviewCount(Long lawyerId, long count) {
        String key = "lawyer:review-count:" + lawyerId;
        redisTemplate.opsForValue().set(key, count, Duration.ofDays(30));
    }

    public long getLawyerReviewCount(Long lawyerId) {
        String key = "lawyer:review-count:" + lawyerId;
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value.toString()) : 0;
    }
}
