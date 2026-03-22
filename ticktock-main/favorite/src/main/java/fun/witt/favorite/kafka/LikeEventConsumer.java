package fun.witt.favorite.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fun.witt.constant.Constant;
import fun.witt.favorite.service.LikeCacheService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka 消费者：消费点赞事件，执行以下操作：
 * 1. HINCRBY agg:like:feed:{feedId}:{timeSlot} like delta
 * 2. SADD active:{timeSlot} aggKey
 * 3. 写入用户级计数增量到聚合桶
 *
 * 读取遇挫时，可基于 Bitmap 状态进行按需重建。
 */
@Slf4j
@Component
public class LikeEventConsumer {

    @Autowired
    private LikeCacheService likeCacheService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = Constant.TOPIC_LIKE_EVENTS, groupId = "like-aggregation-group")
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            JsonNode node = objectMapper.readTree(record.value());
            long feedId = node.get("feedId").asLong();
            long userId = node.get("userId").asLong();
            long authorId = node.get("authorId").asLong();
            int delta = node.get("delta").asInt();

            // 写入视频级聚合桶
            likeCacheService.writeAggregationDelta(feedId, delta);

            // 写入用户级计数聚合桶
            likeCacheService.writeUserCountDelta(userId, authorId, delta);

            ack.acknowledge();
            log.debug("点赞事件已消费并缓冲: feedId={}, userId={}, delta={}", feedId, userId, delta);
        } catch (Exception e) {
            log.error("消费点赞事件失败: {}", record.value(), e);
        }
    }
}
