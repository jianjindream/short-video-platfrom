package fun.witt.favorite.service;

import fun.witt.constant.Constant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CollectCacheService {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> toggleScript;

    @Autowired
    public CollectCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.toggleScript = new DefaultRedisScript<>();
        this.toggleScript.setResultType(Long.class);
        this.toggleScript.setScriptText(TOGGLE_LUA);
    }

    public boolean setCollected(long videoId, long userId) {
        return toggle(videoId, userId, true);
    }

    public boolean setUncollected(long videoId, long userId) {
        return toggle(videoId, userId, false);
    }

    public boolean isCollected(long videoId, long userId) {
        String key = bitmapKey(videoId, userId);
        Boolean bit = redisTemplate.opsForValue().getBit(key, bitOf(userId));
        return Boolean.TRUE.equals(bit);
    }

    public void writeAggregationDelta(long videoId, int delta) {
        long timeSlot = System.currentTimeMillis() / 3600000;
        String aggKey = String.format(Constant.REDIS_AGG_VIDEO_COLLECT, videoId, timeSlot);
        String activeKey = String.format(Constant.REDIS_ACTIVE_VIDEO_SET, timeSlot);
        redisTemplate.opsForHash().increment(aggKey, "collect", delta);
        redisTemplate.opsForSet().add(activeKey, aggKey);
    }

    private boolean toggle(long videoId, long userId, boolean collected) {
        Long changed = redisTemplate.execute(toggleScript,
                Collections.singletonList(bitmapKey(videoId, userId)),
                String.valueOf(bitOf(userId)),
                collected ? "1" : "0");
        return changed != null && changed == 1L;
    }

    private String bitmapKey(long videoId, long userId) {
        return String.format(Constant.REDIS_VIDEO_COLLECT_BITMAP, videoId, chunkOf(userId));
    }

    private long chunkOf(long userId) {
        return userId / Constant.BITMAP_CHUNK_SIZE;
    }

    private long bitOf(long userId) {
        return userId % Constant.BITMAP_CHUNK_SIZE;
    }

    private static final String TOGGLE_LUA =
            "local bmKey = KEYS[1]\n" +
            "local offset = tonumber(ARGV[1])\n" +
            "local desired = tonumber(ARGV[2])\n" +
            "local prev = redis.call('GETBIT', bmKey, offset)\n" +
            "if prev == desired then return 0 end\n" +
            "redis.call('SETBIT', bmKey, offset, desired)\n" +
            "return 1\n";
}
