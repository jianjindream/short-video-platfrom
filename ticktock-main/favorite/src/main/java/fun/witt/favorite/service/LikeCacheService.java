package fun.witt.favorite.service;

import fun.witt.constant.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 点赞状态缓存服务：基于 Redis Bitmap 实现极速读写。
 * Key 格式: feed:{feedId}:likes，使用 userId 作为 offset。
 */
@Slf4j
@Service
public class LikeCacheService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    public void setLiked(long feedId, long userId) {
        String key = String.format(Constant.REDIS_FEED_LIKES, feedId);
        redisTemplate.opsForValue().setBit(key, userId, true);
    }

    public void setUnliked(long feedId, long userId) {
        String key = String.format(Constant.REDIS_FEED_LIKES, feedId);
        redisTemplate.opsForValue().setBit(key, userId, false);
    }

    public boolean isLiked(long feedId, long userId) {
        String key = String.format(Constant.REDIS_FEED_LIKES, feedId);
        Boolean bit = redisTemplate.opsForValue().getBit(key, userId);
        return Boolean.TRUE.equals(bit);
    }

    /**
     * 写入点赞增量到 Redis 聚合桶
     * agg:like:feed:{feedId}:{timeSlot} HINCRBY like delta
     * active:{timeSlot} SADD aggKey
     */
    public void writeAggregationDelta(long feedId, int delta) {
        long timeSlot = System.currentTimeMillis() / 3600000;
        String aggKey = String.format(Constant.REDIS_AGG_LIKE, feedId, timeSlot);
        String activeKey = String.format(Constant.REDIS_ACTIVE_SET, timeSlot);

        redisTemplate.opsForHash().increment(aggKey, "like", delta);
        redisTemplate.opsForSet().add(activeKey, aggKey);
    }

    /**
     * 写入用户级计数增量到聚合桶（用于 BITFIELD 计数器）
     * - liker 的 favorite_count +/- delta
     * - author 的 total_favorited +/- delta
     */
    public void writeUserCountDelta(long likerId, long authorId, int delta) {
        long timeSlot = System.currentTimeMillis() / 3600000;
        String activeKey = String.format(Constant.REDIS_ACTIVE_SET, timeSlot);

        // 点赞用户的"我喜欢"计数
        String likerAggKey = String.format("agg:ucount:%d:%d", likerId, timeSlot);
        redisTemplate.opsForHash().increment(likerAggKey, "favorited_count", delta);
        redisTemplate.opsForSet().add(activeKey, likerAggKey);

        // 视频作者的"获赞"计数
        String authorAggKey = String.format("agg:ucount:%d:%d", authorId, timeSlot);
        redisTemplate.opsForHash().increment(authorAggKey, "total_favorited", delta);
        redisTemplate.opsForSet().add(activeKey, authorAggKey);
    }
}
