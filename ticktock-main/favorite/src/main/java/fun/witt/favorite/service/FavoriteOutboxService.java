package fun.witt.favorite.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fun.witt.constant.Constant;
import fun.witt.mapper.OutboxMapper;
import fun.witt.model.Outbox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class FavoriteOutboxService {

    @Autowired
    private OutboxMapper outboxMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String addLikeEvent(long videoId, long userId, long authorId, String action, int delta) {
        Map<String, Object> payload = new HashMap<>(8);
        String eventId = UUID.randomUUID().toString();
        payload.put("eventId", eventId);
        payload.put("videoId", videoId);
        payload.put("userId", userId);
        payload.put("authorId", authorId);
        payload.put("action", action);
        payload.put("delta", delta);
        payload.put("timestamp", System.currentTimeMillis());

        insertOutbox(eventId, Constant.AGG_TYPE_LIKE_EVENT, String.valueOf(videoId),
                Constant.TOPIC_LIKE_EVENTS, String.valueOf(videoId), payload);
        return eventId;
    }

    public String addCollectEvent(long videoId, long userId, String action, int delta) {
        Map<String, Object> payload = new HashMap<>(7);
        String eventId = UUID.randomUUID().toString();
        payload.put("eventId", eventId);
        payload.put("videoId", videoId);
        payload.put("userId", userId);
        payload.put("action", action);
        payload.put("delta", delta);
        payload.put("timestamp", System.currentTimeMillis());

        insertOutbox(eventId, Constant.AGG_TYPE_COLLECT_EVENT, String.valueOf(videoId),
                Constant.TOPIC_COLLECT_EVENTS, String.valueOf(videoId), payload);
        return eventId;
    }

    private void insertOutbox(String eventId,
                              String aggregateType,
                              String aggregateId,
                              String topic,
                              String eventKey,
                              Map<String, Object> payload) {
        try {
            Outbox outbox = new Outbox();
            outbox.setAggregateType(aggregateType);
            outbox.setAggregateId(aggregateId);
            outbox.setEventId(eventId);
            outbox.setTopic(topic);
            outbox.setEventKey(eventKey);
            outbox.setPayload(objectMapper.writeValueAsString(payload));
            outbox.setCreatedAt(new Date());
            outbox.setProcessed(false);
            outbox.setStatus("NEW");
            outbox.setRetryCount(0);
            outboxMapper.insert(outbox);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("serialize favorite outbox event failed", e);
        }
    }
}
