package fun.witt.relation.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import fun.witt.constant.Constant;
import fun.witt.mapper.OutboxMapper;
import fun.witt.model.Outbox;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Outbox 轮询器：模拟 Canal 监听 binlog 的行为。
 * 定期扫描 t_outbox 中未处理的事件，将其包装为完整 JSON 投递到 Kafka canal-outbox Topic，
 * 然后标记为已处理。
 *
 * 生产环境可替换为 Canal + Kafka 直连方案，此处保证功能等价。
 */
@Slf4j
@Component
public class OutboxPollerTask {

    private static final int BATCH_SIZE = 100;

    @Autowired
    private OutboxMapper outboxMapper;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Scheduled(fixedDelay = 1000)
    public void pollAndPublish() {
        List<Outbox> events = outboxMapper.selectUnprocessedBatch(BATCH_SIZE);
        if (events.isEmpty()) {
            return;
        }

        for (Outbox event : events) {
            try {
                Map<String, Object> message = new HashMap<>(4);
                message.put("aggregateType", event.getAggregateType());
                message.put("aggregateId", event.getAggregateId());
                message.put("payload", event.getPayload());

                String json = objectMapper.writeValueAsString(message);
                kafkaTemplate.send(Constant.TOPIC_CANAL_OUTBOX, event.getAggregateId(), json).get();
            } catch (Exception e) {
                log.error("发送 Outbox 事件到 Kafka 失败, eventId={}", event.getId(), e);
                return;
            }
        }

        List<Long> processedIds = events.stream().map(Outbox::getId).collect(Collectors.toList());
        outboxMapper.markAsProcessed(processedIds);
        log.info("已投递并标记 {} 条 Outbox 事件", processedIds.size());
    }
}
