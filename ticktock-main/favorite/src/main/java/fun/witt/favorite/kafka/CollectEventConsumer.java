package fun.witt.favorite.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fun.witt.constant.Constant;
import fun.witt.favorite.service.CollectCacheService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
public class CollectEventConsumer {

    @Autowired
    private CollectCacheService collectCacheService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = Constant.TOPIC_COLLECT_EVENTS, groupId = "collect-aggregation-group")
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            JsonNode node = objectMapper.readTree(record.value());
            String eventId = textValue(node, "eventId");
            long videoId = longValue(node, "videoId", 0L);
            long userId = longValue(node, "userId", 0L);
            int delta = (int) longValue(node, "delta", 0L);

            if (eventId != null && !eventId.isEmpty()) {
                Boolean first = redisTemplate.opsForValue()
                        .setIfAbsent("dedup:collect:" + eventId, "1", Duration.ofHours(24));
                if (!Boolean.TRUE.equals(first)) {
                    ack.acknowledge();
                    return;
                }
            }

            if (videoId <= 0 || userId <= 0 || delta == 0) {
                ack.acknowledge();
                return;
            }

            collectCacheService.writeAggregationDelta(videoId, delta);

            ack.acknowledge();
            log.debug("collect event aggregated, videoId={}, userId={}, delta={}", videoId, userId, delta);
        } catch (Exception e) {
            log.error("consume collect event failed: {}", record.value(), e);
        }
    }

    private String textValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private long longValue(JsonNode node, String field, long defaultValue) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? defaultValue : value.asLong(defaultValue);
    }
}
