package fun.witt.relation.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fun.witt.constant.Constant;
import fun.witt.relation.event.RelationEvent;
import fun.witt.relation.processor.RelationEventProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CanalOutboxConsumer {

    @Autowired
    private RelationEventProcessor processor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = Constant.TOPIC_CANAL_OUTBOX, groupId = "relation-outbox-consumer")
    public void onMessage(String message, Acknowledgment ack) {
        try {
            JsonNode root = objectMapper.readTree(message);
            if (root.has("data") && root.get("data").isArray()) {
                for (JsonNode row : root.get("data")) {
                    consumePayload(row.get("payload"));
                }
            } else if (root.has("payload")) {
                consumePayload(root.get("payload"));
            } else {
                RelationEvent event = objectMapper.treeToValue(root, RelationEvent.class);
                processor.process(event);
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("consume canal outbox message failed: {}", message, e);
        }
    }

    private void consumePayload(JsonNode payloadNode) throws Exception {
        if (payloadNode == null || payloadNode.isNull()) {
            return;
        }
        RelationEvent event;
        if (payloadNode.isTextual()) {
            event = objectMapper.readValue(payloadNode.asText(), RelationEvent.class);
        } else {
            event = objectMapper.treeToValue(payloadNode, RelationEvent.class);
        }
        processor.process(event);
    }
}
