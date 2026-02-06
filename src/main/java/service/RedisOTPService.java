package service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class RedisOTPService {

    private final StringRedisTemplate redisTemplate;
    private static final String OTP_KEY_PREFIX = "otp:manual:";
    private static final long OTP_EXPIRY_MINUTES = 5;

    public RedisOTPService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String generateAndStoreOTP(String type, String identifier) {
        String otp = String.valueOf(100000 + new Random().nextInt(900000));
        String key = OTP_KEY_PREFIX + type + ":" + identifier;

        redisTemplate.opsForValue().set(key, otp, OTP_EXPIRY_MINUTES, TimeUnit.MINUTES);
        return otp;
    }

    public boolean validateOTP(String type, String identifier, String otp) {
        String key = OTP_KEY_PREFIX + type + ":" + identifier;
        String cachedOtp = redisTemplate.opsForValue().get(key);

        if (cachedOtp != null && cachedOtp.equals(otp)) {
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }
}
