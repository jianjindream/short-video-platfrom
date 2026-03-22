package fun.witt.relation.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fun.witt.constant.Constant;
import fun.witt.mapper.FollowerMapper;
import fun.witt.model.Follower;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import tk.mybatis.mapper.entity.Example;

/**
 * Kafka 消费者：监听 canal-outbox Topic。
 * 解析消息后，使用 INSERT IGNORE 写入 t_follower 从表，
 * 依赖联合唯一索引 (uk_from_to) 实现天然幂等。
 */
@Slf4j
@Component
public class FollowerSyncConsumer {

    @Autowired
    private FollowerMapper followerMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = Constant.TOPIC_CANAL_OUTBOX, groupId = "relation-follower-sync-group")
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            JsonNode root = objectMapper.readTree(record.value());
            String aggregateType = root.get("aggregateType").asText();
            JsonNode payload = objectMapper.readTree(root.get("payload").asText());
            long fromUserId = payload.get("fromUserId").asLong();
            long toUserId = payload.get("toUserId").asLong();

            if (Constant.AGG_TYPE_FOLLOW.equals(aggregateType)) {
                // INSERT IGNORE 天然幂等，依赖 uk_from_to 联合唯一索引
                followerMapper.insertIgnore(fromUserId, toUserId);
                log.debug("从表同步 FOLLOW: {} -> {}", fromUserId, toUserId);
            } else if (Constant.AGG_TYPE_UNFOLLOW.equals(aggregateType)) {
                Example example = new Example(Follower.class);
                Example.Criteria criteria = example.createCriteria();
                criteria.andEqualTo("fromUserId", fromUserId);
                criteria.andEqualTo("toUserId", toUserId);
                followerMapper.deleteByExample(example);
                log.debug("从表同步 UNFOLLOW: {} -> {}", fromUserId, toUserId);
            }

            ack.acknowledge();
        } catch (Exception e) {
            log.error("处理 canal-outbox 消息失败: {}", record.value(), e);
        }
    }
}
