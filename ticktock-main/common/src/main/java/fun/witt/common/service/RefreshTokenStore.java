package fun.witt.common.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RefreshTokenStore {
    private static final String KEY_PREFIX = "auth:refresh:jti:";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public void save(String jti, Long userId, long ttlMs) {
        stringRedisTemplate.opsForValue()
                .set(KEY_PREFIX + jti, String.valueOf(userId), ttlMs, TimeUnit.MILLISECONDS);
    }

    public boolean exists(String jti) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(KEY_PREFIX + jti));
    }

    public Long getUserId(String jti) {
        String value = stringRedisTemplate.opsForValue().get(KEY_PREFIX + jti);
        return value == null ? null : Long.parseLong(value);
    }

    public void delete(String jti) {
        stringRedisTemplate.delete(KEY_PREFIX + jti);
    }
}
