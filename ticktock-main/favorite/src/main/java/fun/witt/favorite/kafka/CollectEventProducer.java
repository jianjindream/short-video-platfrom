package fun.witt.favorite.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import fun.witt.constant.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class CollectEventProducer {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void sendCollectEvent(String eventId, long videoId, long userId, String action, int delta) {
        try {
            Map<String, Object> event = new HashMap<>(6);
            event.put("eventId", eventId);
            event.put("videoId", videoId);
            event.put("userId", userId);
            event.put("action", action);
            event.put("delta", delta);
            event.put("timestamp", System.currentTimeMillis());

            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(Constant.TOPIC_COLLECT_EVENTS, String.valueOf(videoId), json);
        } catch (Exception e) {
            log.error("send collect event failed, videoId={}, userId={}", videoId, userId, e);
        }
    }
}
