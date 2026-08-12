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

    private static final String CONSUMER_GROUP = "collect-aggregation-group";

    @Autowired
    private CollectCacheService collectCacheService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private MessageConsumeFailureHandler failureHandler;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = Constant.TOPIC_COLLECT_EVENTS, groupId = CONSUMER_GROUP)
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String dedupKey = null;
        boolean dedupAcquired = false;
        String eventId = null;
        try {
            JsonNode node = objectMapper.readTree(record.value());
            eventId = textValue(node, "eventId");
            long videoId = longValue(node, "videoId", 0L);
            long userId = longValue(node, "userId", 0L);
            int delta = (int) longValue(node, "delta", 0L);

            if (eventId != null && !eventId.isEmpty()) {
                dedupKey = "dedup:collect:" + eventId;
                Boolean first = redisTemplate.opsForValue()
                        .setIfAbsent(dedupKey, "1", Duration.ofHours(24));
                if (!Boolean.TRUE.equals(first)) {
                    ack.acknowledge();
                    return;
                }
                dedupAcquired = true;
            }

            if (videoId <= 0 || userId <= 0 || delta == 0) {
                ack.acknowledge();
                return;
            }

            collectCacheService.writeAggregationDelta(videoId, delta);

            ack.acknowledge();
            log.debug("collect event aggregated, videoId={}, userId={}, delta={}", videoId, userId, delta);
        } catch (Exception e) {
            failureHandler.retryOrDead("collect", CONSUMER_GROUP, record, eventId, dedupKey, dedupAcquired, e, ack);
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
