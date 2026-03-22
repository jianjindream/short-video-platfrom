package fun.witt.user.task;

import fun.witt.constant.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Set;

/**
 * 自适应定时刷写任务：
 * 1. 扫描 active:{timeSlot} Set，批量获取待刷新的聚合桶 Hash Key
 * 2. 对每个聚合桶调用 Lua 原子折叠脚本（HGETALL → BITFIELD INCRBY → DEL）
 *
 * 支持处理当前小时和上一个小时的 active set，避免跨小时边界的事件丢失。
 */
@Slf4j
@Component
public class CounterFlushTask {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private DefaultRedisScript<Long> flushScript;

    @PostConstruct
    public void init() {
        flushScript = new DefaultRedisScript<>();
        flushScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/counter_flush.lua")));
        flushScript.setResultType(Long.class);
    }

    @Scheduled(fixedDelay = 5000)
    public void flush() {
        long currentSlot = System.currentTimeMillis() / 3600000;
        flushSlot(currentSlot);
        flushSlot(currentSlot - 1);
    }

    private void flushSlot(long timeSlot) {
        String activeKey = String.format(Constant.REDIS_ACTIVE_SET, timeSlot);
        Set<String> aggKeys = redisTemplate.opsForSet().members(activeKey);
        if (aggKeys == null || aggKeys.isEmpty()) {
            return;
        }

        int flushed = 0;
        for (String aggKey : aggKeys) {
            try {
                String counterKey = extractCounterKey(aggKey);
                if (counterKey == null) {
                    continue;
                }

                Long result = redisTemplate.execute(flushScript, Arrays.asList(aggKey, counterKey));
                if (result != null && result > 0) {
                    flushed++;
                }

                redisTemplate.opsForSet().remove(activeKey, aggKey);
            } catch (Exception e) {
                log.error("刷写聚合桶失败: {}", aggKey, e);
            }
        }

        if (flushed > 0) {
            log.info("已刷写 {} 个聚合桶 (timeSlot={})", flushed, timeSlot);
        }
    }

    /**
     * 从聚合桶 Key 中提取对应的 BITFIELD 计数器 Key
     * agg:ucount:{userId}:{timeSlot} -> ucounter:{userId}
     */
    private String extractCounterKey(String aggKey) {
        if (aggKey.startsWith("agg:ucount:")) {
            String[] parts = aggKey.split(":");
            if (parts.length >= 3) {
                return "ucounter:" + parts[2];
            }
        }
        return null;
    }
}
