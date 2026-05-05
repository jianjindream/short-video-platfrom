package fun.witt.favorite.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import fun.witt.constant.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class LikeEventProducer {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean sendLikeEvent(String eventId, long videoId, long userId, long authorId, String action, int delta) {
        try {
            Map<String, Object> event = new HashMap<>(8);
            event.put("eventId", eventId);
            event.put("videoId", videoId);
            event.put("userId", userId);
            event.put("authorId", authorId);
            event.put("action", action);
            event.put("delta", delta);
            event.put("timestamp", System.currentTimeMillis());

            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(Constant.TOPIC_LIKE_EVENTS, String.valueOf(videoId), json)
                    .get(5, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            log.error("send like event failed, videoId={}, userId={}", videoId, userId, e);
            return false;
        }
    }
}
