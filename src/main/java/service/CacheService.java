package service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import entity.user;

@Service
public class CacheService {

    private final org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;
    private static final String SEARCH_CACHE_PREFIX = "manual:search:";

    public CacheService(org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void storeSearchResults(String userId, List<entity.user> searchResults) {
        redisTemplate.opsForValue().set(SEARCH_CACHE_PREFIX + userId, searchResults, java.time.Duration.ofMinutes(10));
    }

    public List<entity.user> getSearchResults(String userId) {
        return (List<entity.user>) redisTemplate.opsForValue().get(SEARCH_CACHE_PREFIX + userId);
    }

    public void clearSearchResults(String userId) {
        redisTemplate.delete(SEARCH_CACHE_PREFIX + userId);
    }
}
