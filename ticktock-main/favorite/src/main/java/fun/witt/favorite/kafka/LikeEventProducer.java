package fun.witt.favorite.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import fun.witt.constant.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 点赞事件 Kafka 生产者。
 * 配置 acks=all + 微批处理 (batch.size, linger.ms)。
 * 以 feedId 作为 Hash Key 路由，保证同一视频的事件有序。
 */
@Slf4j
@Component
public class LikeEventProducer {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void sendLikeEvent(long feedId, long userId, long authorId, String action, int delta) {
        try {
            Map<String, Object> event = new HashMap<>(8);
            event.put("feedId", feedId);
            event.put("userId", userId);
            event.put("authorId", authorId);
            event.put("action", action);
            event.put("delta", delta);
            event.put("timestamp", System.currentTimeMillis());

            String json = objectMapper.writeValueAsString(event);
            String key = String.valueOf(feedId);
            kafkaTemplate.send(Constant.TOPIC_LIKE_EVENTS, key, json);
        } catch (Exception e) {
            log.error("发送点赞事件到 Kafka 失败, feedId={}, userId={}", feedId, userId, e);
        }
    }
}
