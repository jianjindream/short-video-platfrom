package fun.witt.common.service;

import fun.witt.constant.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 计数系统核心服务：基于 Redis BITFIELD 的二进制安全极致内存压缩。
 *
 * 底层存储使用 Redis SDS (String)，不使用 JSON。
 * 将 String 视作连续的字节数组，使用 BITFIELD 命令寻址：
 *   offset  0-31: 关注数 (follow_count)     u32
 *   offset 32-63: 粉丝数 (follower_count)   u32
 *   offset 64-95: 获赞数 (total_favorited)  u32
 *
 * Key 格式: ucounter:{userId}
 */
@Slf4j
@Service
public class CounterService {

    private static final BitFieldSubCommands.BitFieldType U32 =
            BitFieldSubCommands.BitFieldType.unsigned(32);

    @Autowired
    private StringRedisTemplate redisTemplate;

    public long getFollowCount(long userId) {
        return getBitfieldValue(userId, Constant.OFFSET_FOLLOW_COUNT);
    }

    public long getFollowerCount(long userId) {
        return getBitfieldValue(userId, Constant.OFFSET_FOLLOWER_COUNT);
    }

    public long getFavoritedCount(long userId) {
        return getBitfieldValue(userId, Constant.OFFSET_FAVORITED_COUNT);
    }

    /**
     * 一次性读取用户的三个计数值：followCount, followerCount, favoritedCount
     */
    public long[] getUserCounters(long userId) {
        String key = String.format(Constant.REDIS_USER_COUNTER, userId);
        BitFieldSubCommands commands = BitFieldSubCommands.create()
                .get(U32).valueAt(BitFieldSubCommands.Offset.offset(Constant.OFFSET_FOLLOW_COUNT))
                .get(U32).valueAt(BitFieldSubCommands.Offset.offset(Constant.OFFSET_FOLLOWER_COUNT))
                .get(U32).valueAt(BitFieldSubCommands.Offset.offset(Constant.OFFSET_FAVORITED_COUNT));

        List<Long> results = redisTemplate.opsForValue().bitField(key, commands);
        if (results == null || results.size() < 3) {
            return new long[]{0, 0, 0};
        }
        return new long[]{results.get(0), results.get(1), results.get(2)};
    }

    /**
     * 设置某个 offset 的绝对值（用于对账覆盖）
     */
    public void setCounter(long userId, int offset, long value) {
        String key = String.format(Constant.REDIS_USER_COUNTER, userId);
        BitFieldSubCommands commands = BitFieldSubCommands.create()
                .set(U32).valueAt(BitFieldSubCommands.Offset.offset(offset)).to(value);
        redisTemplate.opsForValue().bitField(key, commands);
    }

    /**
     * 增量更新某个 offset 的值
     */
    public void incrCounter(long userId, int offset, long delta) {
        String key = String.format(Constant.REDIS_USER_COUNTER, userId);
        BitFieldSubCommands commands = BitFieldSubCommands.create()
                .incr(U32).valueAt(BitFieldSubCommands.Offset.offset(offset)).by(delta);
        redisTemplate.opsForValue().bitField(key, commands);
    }

    private long getBitfieldValue(long userId, int offset) {
        String key = String.format(Constant.REDIS_USER_COUNTER, userId);
        BitFieldSubCommands commands = BitFieldSubCommands.create()
                .get(U32).valueAt(BitFieldSubCommands.Offset.offset(offset));

        List<Long> results = redisTemplate.opsForValue().bitField(key, commands);
        if (results == null || results.isEmpty()) {
            return 0;
        }
        return results.get(0);
    }
}
