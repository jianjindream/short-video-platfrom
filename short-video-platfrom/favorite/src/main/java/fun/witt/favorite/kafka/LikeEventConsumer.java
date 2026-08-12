package fun.witt.favorite.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fun.witt.constant.Constant;
import fun.witt.favorite.service.LikeCacheService;
import fun.witt.mapper.FavoriteMapper;
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
public class LikeEventConsumer {

    private static final String ACTION_LIKE = "LIKE";
    private static final String ACTION_UNLIKE = "UNLIKE";
    private static final String CONSUMER_GROUP = "like-aggregation-group";

    @Autowired
    private LikeCacheService likeCacheService;

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private MessageConsumeFailureHandler failureHandler;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = Constant.TOPIC_LIKE_EVENTS, groupId = CONSUMER_GROUP)
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String dedupKey = null;
        boolean dedupAcquired = false;
        String eventId = null;
        try {
            JsonNode node = objectMapper.readTree(record.value());
            eventId = textValue(node, "eventId");
            long videoId = longValue(node, "videoId", longValue(node, "feedId", 0L));
            long userId = longValue(node, "userId", 0L);
            long authorId = longValue(node, "authorId", 0L);
            String action = textValue(node, "action");
            int delta = (int) longValue(node, "delta", 0L);

            if (eventId != null && !eventId.isEmpty()) {
                dedupKey = "dedup:like:" + eventId;
                Boolean first = redisTemplate.opsForValue()
                        .setIfAbsent(dedupKey, "1", Duration.ofHours(24));
                if (!Boolean.TRUE.equals(first)) {
                    ack.acknowledge();
                    return;
                }
                dedupAcquired = true;
            }

            if (videoId <= 0 || userId <= 0 || delta == 0 || action == null || action.isEmpty()) {
                ack.acknowledge();
                return;
            }

            applyFavoriteProjection(action, userId, videoId);
            likeCacheService.writeAggregationDelta(videoId, delta);
            likeCacheService.writeUserCountDelta(userId, authorId, delta);

            ack.acknowledge();
            log.debug("like event processed, videoId={}, userId={}, action={}, delta={}",
                    videoId, userId, action, delta);
        } catch (Exception e) {
            failureHandler.retryOrDead("like", CONSUMER_GROUP, record, eventId, dedupKey, dedupAcquired, e, ack);
        }
    }

    private void applyFavoriteProjection(String action, long userId, long videoId) {
        if (ACTION_LIKE.equalsIgnoreCase(action)) {
            favoriteMapper.insertIgnore(userId, videoId);
            return;
        }
        if (ACTION_UNLIKE.equalsIgnoreCase(action)) {
            favoriteMapper.deleteByUserAndVideo(userId, videoId);
            return;
        }
        throw new IllegalArgumentException("unsupported like action: " + action);
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
