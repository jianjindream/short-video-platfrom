package fun.witt.gateway.ratelimit;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

@Component
public class RedisSlidingWindowRateLimiter {

    private final ReactiveStringRedisTemplate redisTemplate;

    private final DefaultRedisScript<Long> script;

    public RedisSlidingWindowRateLimiter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.script = new DefaultRedisScript<>();
        this.script.setResultType(Long.class);
        this.script.setScriptText(
                "local key = KEYS[1]\n" +
                        "local now = tonumber(ARGV[1])\n" +
                        "local window = tonumber(ARGV[2])\n" +
                        "local limit = tonumber(ARGV[3])\n" +
                        "local member = ARGV[4]\n" +
                        "redis.call('ZREMRANGEBYSCORE', key, 0, now - window)\n" +
                        "local current = redis.call('ZCARD', key)\n" +
                        "if current >= limit then\n" +
                        "  return 0\n" +
                        "end\n" +
                        "redis.call('ZADD', key, now, member)\n" +
                        "redis.call('PEXPIRE', key, window)\n" +
                        "return 1\n"
        );
    }

    public Mono<Boolean> isAllowed(String key, int limit, long windowMillis) {
        long now = System.currentTimeMillis();
        String member = now + "-" + UUID.randomUUID();
        return redisTemplate.execute(script,
                        Collections.singletonList(key),
                        Arrays.asList(String.valueOf(now),
                                String.valueOf(windowMillis),
                                String.valueOf(limit),
                                member))
                .next()
                .map(result -> Long.valueOf(1L).equals(result))
                .defaultIfEmpty(true);
    }
}
