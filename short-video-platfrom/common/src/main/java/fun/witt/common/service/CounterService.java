package fun.witt.common.service;

import fun.witt.constant.Constant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CounterService {

    private static final long UINT32_MAX = 0xFFFF_FFFFL;

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> incrScript;
    private final DefaultRedisScript<Long> setScript;

    @Autowired
    public CounterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.incrScript = new DefaultRedisScript<>();
        this.incrScript.setResultType(Long.class);
        this.incrScript.setScriptText(INCR_FIELD_LUA);

        this.setScript = new DefaultRedisScript<>();
        this.setScript.setResultType(Long.class);
        this.setScript.setScriptText(SET_FIELD_LUA);
    }

    public long getFollowCount(long userId) {
        return getUserCounterValue(userId, 1);
    }

    public long getFollowerCount(long userId) {
        return getUserCounterValue(userId, 2);
    }

    public long getFavoritedCount(long userId) {
        return getUserCounterValue(userId, 3);
    }

    public long getFavoriteCount(long userId) {
        return getUserCounterValue(userId, 4);
    }

    public long[] getUserCounters(long userId) {
        byte[] raw = getRaw(String.format(Constant.REDIS_USER_COUNTER, userId));
        int len = Constant.USER_COUNTER_FIELDS * Constant.COUNTER_FIELD_SIZE;
        if (raw == null || raw.length < len) {
            return new long[]{0, 0, 0, 0};
        }
        return new long[]{
                readUInt32BE(raw, 0),
                readUInt32BE(raw, 4),
                readUInt32BE(raw, 8),
                readUInt32BE(raw, 12)
        };
    }

    public void setCounter(long userId, int offset, long value) {
        setField(String.format(Constant.REDIS_USER_COUNTER, userId),
                Constant.USER_COUNTER_FIELDS, offsetToIndex(offset), value);
    }

    public void incrCounter(long userId, int offset, long delta) {
        incrField(String.format(Constant.REDIS_USER_COUNTER, userId),
                Constant.USER_COUNTER_FIELDS, offsetToIndex(offset), delta);
    }

    public long getVideoLikeCount(long videoId) {
        return getVideoCounterValue(videoId, Constant.VIDEO_COUNTER_IDX_LIKE);
    }

    public long getVideoCollectCount(long videoId) {
        return getVideoCounterValue(videoId, Constant.VIDEO_COUNTER_IDX_COLLECT);
    }

    public Map<Long, Long> getVideoLikeCounts(List<Long> videoIds) {
        return getVideoCounterValues(videoIds, Constant.VIDEO_COUNTER_IDX_LIKE);
    }

    public Map<Long, Long> getVideoCollectCounts(List<Long> videoIds) {
        return getVideoCounterValues(videoIds, Constant.VIDEO_COUNTER_IDX_COLLECT);
    }

    private Map<Long, Long> getVideoCounterValues(List<Long> videoIds, int zeroBasedIndex) {
        Map<Long, Long> result = new LinkedHashMap<>();
        if (videoIds == null || videoIds.isEmpty()) {
            return result;
        }

        final List<String> keys = new ArrayList<>(videoIds.size());
        for (Long videoId : videoIds) {
            keys.add(String.format(Constant.REDIS_VIDEO_COUNTER, videoId));
        }

        List<Object> raws = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (String key : keys) {
                connection.stringCommands().get(key.getBytes(StandardCharsets.UTF_8));
            }
            return null;
        });

        int expectedLen = Constant.VIDEO_COUNTER_SCHEMA_LEN * Constant.COUNTER_FIELD_SIZE;
        int offset = zeroBasedIndex * Constant.COUNTER_FIELD_SIZE;
        for (int i = 0; i < videoIds.size(); i++) {
            byte[] raw = null;
            if (raws != null && i < raws.size() && raws.get(i) instanceof byte[]) {
                raw = (byte[]) raws.get(i);
            }
            long value = (raw == null || raw.length < expectedLen) ? 0 : readUInt32BE(raw, offset);
            result.put(videoIds.get(i), value);
        }
        return result;
    }

    public void incrementVideoLikeCount(long videoId, long delta) {
        incrField(String.format(Constant.REDIS_VIDEO_COUNTER, videoId),
                Constant.VIDEO_COUNTER_SCHEMA_LEN, Constant.VIDEO_COUNTER_IDX_LIKE, delta);
    }

    public void incrementVideoCollectCount(long videoId, long delta) {
        incrField(String.format(Constant.REDIS_VIDEO_COUNTER, videoId),
                Constant.VIDEO_COUNTER_SCHEMA_LEN, Constant.VIDEO_COUNTER_IDX_COLLECT, delta);
    }

    public void setVideoLikeCount(long videoId, long value) {
        setField(String.format(Constant.REDIS_VIDEO_COUNTER, videoId),
                Constant.VIDEO_COUNTER_SCHEMA_LEN, Constant.VIDEO_COUNTER_IDX_LIKE, value);
    }

    public void setVideoCollectCount(long videoId, long value) {
        setField(String.format(Constant.REDIS_VIDEO_COUNTER, videoId),
                Constant.VIDEO_COUNTER_SCHEMA_LEN, Constant.VIDEO_COUNTER_IDX_COLLECT, value);
    }

    private long getUserCounterValue(long userId, int index) {
        return getSdsCounterValue(String.format(Constant.REDIS_USER_COUNTER, userId),
                Constant.USER_COUNTER_FIELDS, index - 1);
    }

    private long getSdsCounterValue(String key, int schemaLen, int zeroBasedIndex) {
        byte[] raw = getRaw(key);
        int expectedLen = schemaLen * Constant.COUNTER_FIELD_SIZE;
        int offset = zeroBasedIndex * Constant.COUNTER_FIELD_SIZE;
        if (raw == null || raw.length < expectedLen || offset < 0 || offset + 4 > raw.length) {
            return 0;
        }
        return readUInt32BE(raw, offset);
    }

    private long getVideoCounterValue(long videoId, int zeroBasedIndex) {
        return getSdsCounterValue(String.format(Constant.REDIS_VIDEO_COUNTER, videoId),
                Constant.VIDEO_COUNTER_SCHEMA_LEN, zeroBasedIndex);
    }

    private void incrField(String key, int schemaLen, int zeroBasedIndex, long delta) {
        redisTemplate.execute(incrScript, Collections.singletonList(key),
                String.valueOf(schemaLen),
                String.valueOf(Constant.COUNTER_FIELD_SIZE),
                String.valueOf(zeroBasedIndex),
                String.valueOf(delta));
    }

    private void setField(String key, int schemaLen, int zeroBasedIndex, long value) {
        long normalized = Math.max(0, Math.min(value, UINT32_MAX));
        redisTemplate.execute(setScript, Collections.singletonList(key),
                String.valueOf(schemaLen),
                String.valueOf(Constant.COUNTER_FIELD_SIZE),
                String.valueOf(zeroBasedIndex),
                String.valueOf(normalized));
    }

    private byte[] getRaw(String key) {
        return redisTemplate.execute((RedisCallback<byte[]>) connection ->
                connection.stringCommands().get(key.getBytes(StandardCharsets.UTF_8)));
    }

    private int offsetToIndex(int offset) {
        return offset / 32;
    }

    private static long readUInt32BE(byte[] buf, int off) {
        if (buf == null || off < 0 || buf.length < off + 4) {
            return 0;
        }
        long n = 0L;
        for (int i = 0; i < 4; i++) {
            n = (n << 8) | (buf[off + i] & 0xFFL);
        }
        return n;
    }

    private static final String INCR_FIELD_LUA =
            "local cntKey = KEYS[1]\n" +
            "local schemaLen = tonumber(ARGV[1])\n" +
            "local fieldSize = tonumber(ARGV[2])\n" +
            "local idx = tonumber(ARGV[3])\n" +
            "local delta = tonumber(ARGV[4])\n" +
            "local max = 4294967295\n" +
            "local targetLen = schemaLen * fieldSize\n" +
            "local function normalize(s)\n" +
            "  if not s then return string.rep(string.char(0), targetLen) end\n" +
            "  local len = string.len(s)\n" +
            "  if len < targetLen then return s .. string.rep(string.char(0), targetLen - len) end\n" +
            "  return s\n" +
            "end\n" +
            "local function read32be(s, off)\n" +
            "  local b = {string.byte(s, off + 1, off + 4)}\n" +
            "  local n = 0\n" +
            "  for i = 1, 4 do n = n * 256 + (b[i] or 0) end\n" +
            "  return n\n" +
            "end\n" +
            "local function write32be(n)\n" +
            "  local t = {}\n" +
            "  for i = 4, 1, -1 do t[i] = n % 256; n = math.floor(n / 256) end\n" +
            "  return string.char(unpack(t))\n" +
            "end\n" +
            "local cnt = normalize(redis.call('GET', cntKey))\n" +
            "local off = idx * fieldSize\n" +
            "local v = read32be(cnt, off) + delta\n" +
            "if v < 0 then v = 0 end\n" +
            "if v > max then v = max end\n" +
            "local seg = write32be(v)\n" +
            "cnt = string.sub(cnt, 1, off) .. seg .. string.sub(cnt, off + fieldSize + 1)\n" +
            "redis.call('SET', cntKey, cnt)\n" +
            "return v\n";

    private static final String SET_FIELD_LUA =
            "local cntKey = KEYS[1]\n" +
            "local schemaLen = tonumber(ARGV[1])\n" +
            "local fieldSize = tonumber(ARGV[2])\n" +
            "local idx = tonumber(ARGV[3])\n" +
            "local value = tonumber(ARGV[4])\n" +
            "local targetLen = schemaLen * fieldSize\n" +
            "local function normalize(s)\n" +
            "  if not s then return string.rep(string.char(0), targetLen) end\n" +
            "  local len = string.len(s)\n" +
            "  if len < targetLen then return s .. string.rep(string.char(0), targetLen - len) end\n" +
            "  return s\n" +
            "end\n" +
            "local function write32be(n)\n" +
            "  local t = {}\n" +
            "  for i = 4, 1, -1 do t[i] = n % 256; n = math.floor(n / 256) end\n" +
            "  return string.char(unpack(t))\n" +
            "end\n" +
            "local cnt = normalize(redis.call('GET', cntKey))\n" +
            "local off = idx * fieldSize\n" +
            "local seg = write32be(value)\n" +
            "cnt = string.sub(cnt, 1, off) .. seg .. string.sub(cnt, off + fieldSize + 1)\n" +
            "redis.call('SET', cntKey, cnt)\n" +
            "return value\n";
}
