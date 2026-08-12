package fun.witt.favorite.task;

import fun.witt.constant.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Set;

@Slf4j
@Component
public class VideoCounterFlushTask {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private DefaultRedisScript<Long> flushScript;

    @PostConstruct
    public void init() {
        flushScript = new DefaultRedisScript<>();
        flushScript.setResultType(Long.class);
        flushScript.setScriptText(FLUSH_LUA);
    }

    @Scheduled(fixedDelay = 5000)
    public void flush() {
        long currentSlot = System.currentTimeMillis() / 3600000;
        flushSlot(currentSlot);
        flushSlot(currentSlot - 1);
    }

    private void flushSlot(long timeSlot) {
        String activeKey = String.format(Constant.REDIS_ACTIVE_VIDEO_SET, timeSlot);
        Set<String> aggKeys = redisTemplate.opsForSet().members(activeKey);
        if (aggKeys == null || aggKeys.isEmpty()) {
            return;
        }

        int flushed = 0;
        for (String aggKey : aggKeys) {
            String counterKey = extractCounterKey(aggKey);
            if (counterKey == null) {
                redisTemplate.opsForSet().remove(activeKey, aggKey);
                continue;
            }
            try {
                Long left = redisTemplate.execute(flushScript, Arrays.asList(aggKey, counterKey));
                if (left != null) {
                    flushed++;
                }
                if (left != null && left == 0) {
                    redisTemplate.opsForSet().remove(activeKey, aggKey);
                }
            } catch (Exception e) {
                log.error("flush video counter agg failed, aggKey={}", aggKey, e);
            }
        }

        if (flushed > 0) {
            log.info("flushed {} video counter buckets, timeSlot={}", flushed, timeSlot);
        }
    }

    private String extractCounterKey(String aggKey) {
        if (aggKey.startsWith("agg:v1:video:collect:")) {
            String[] parts = aggKey.split(":");
            if (parts.length >= 6) {
                try {
                    return String.format(Constant.REDIS_VIDEO_COUNTER, Long.parseLong(parts[4]));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        }
        if (!aggKey.startsWith("agg:v1:video:")) {
            return null;
        }
        String[] parts = aggKey.split(":");
        if (parts.length < 5) {
            return null;
        }
        try {
            return String.format(Constant.REDIS_VIDEO_COUNTER, Long.parseLong(parts[3]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static final String FLUSH_LUA =
            "local aggKey = KEYS[1]\n" +
            "local counterKey = KEYS[2]\n" +
            "local fields = redis.call('HGETALL', aggKey)\n" +
            "if #fields == 0 then return 0 end\n" +
            "local schemaLen = 5\n" +
            "local fieldSize = 4\n" +
            "local targetLen = schemaLen * fieldSize\n" +
            "local max = 4294967295\n" +
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
            "local cnt = normalize(redis.call('GET', counterKey))\n" +
            "for i = 1, #fields, 2 do\n" +
            "  local field = fields[i]\n" +
            "  local delta = tonumber(fields[i + 1])\n" +
            "  local idx = -1\n" +
            "  if field == 'like' then idx = 1 end\n" +
            "  if field == 'collect' then idx = 2 end\n" +
            "  if idx >= 0 and delta and delta ~= 0 then\n" +
            "    local off = idx * fieldSize\n" +
            "    local v = read32be(cnt, off) + delta\n" +
            "    if v < 0 then v = 0 end\n" +
            "    if v > max then v = max end\n" +
            "    cnt = string.sub(cnt, 1, off) .. write32be(v) .. string.sub(cnt, off + fieldSize + 1)\n" +
            "    local left = redis.call('HINCRBY', aggKey, field, -delta)\n" +
            "    if left == 0 then redis.call('HDEL', aggKey, field) end\n" +
            "  end\n" +
            "end\n" +
            "redis.call('SET', counterKey, cnt)\n" +
            "return redis.call('HLEN', aggKey)\n";
}
