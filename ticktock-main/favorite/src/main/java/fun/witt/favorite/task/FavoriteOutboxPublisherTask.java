package fun.witt.favorite.task;

import fun.witt.constant.Constant;
import fun.witt.mapper.OutboxMapper;
import fun.witt.model.Outbox;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class FavoriteOutboxPublisherTask {

    private static final List<String> TOPICS = Arrays.asList(
            Constant.TOPIC_LIKE_EVENTS,
            Constant.TOPIC_COLLECT_EVENTS);

    @Autowired
    private OutboxMapper outboxMapper;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value("${favorite.outbox.publisher.batch-size:100}")
    private int batchSize;

    @Value("${favorite.outbox.publisher.max-retry:6}")
    private int maxRetry;

    @Value("${favorite.outbox.publisher.send-timeout-ms:5000}")
    private long sendTimeoutMs;

    @Value("${favorite.outbox.publisher.inflight-timeout-ms:300000}")
    private long inflightTimeoutMs;

    @Scheduled(fixedDelayString = "${favorite.outbox.publisher.fixed-delay-ms:1000}")
    public void publish() {
        List<Outbox> events = outboxMapper.selectPublishableByTopics(TOPICS, batchSize);
        if (events.isEmpty()) {
            return;
        }

        for (Outbox event : events) {
            publishOne(event);
        }
    }

    private void publishOne(Outbox event) {
        Date publishingDeadline = new Date(System.currentTimeMillis() + inflightTimeoutMs);
        if (outboxMapper.markAsPublishing(event.getId(), publishingDeadline) == 0) {
            return;
        }

        try {
            String topic = event.getTopic();
            String key = event.getEventKey() == null ? event.getAggregateId() : event.getEventKey();
            kafkaTemplate.send(topic, key, event.getPayload()).get(sendTimeoutMs, TimeUnit.MILLISECONDS);
            outboxMapper.markOneAsProcessed(event.getId());
            log.debug("favorite outbox event published, id={}, eventId={}, topic={}",
                    event.getId(), event.getEventId(), topic);
        } catch (Exception e) {
            int nextRetry = event.getRetryCount() == null ? 1 : event.getRetryCount() + 1;
            String message = truncate(e.getMessage());
            if (nextRetry >= maxRetry) {
                outboxMapper.markAsDead(event.getId(), message);
                log.error("favorite outbox event moved to dead state, id={}, eventId={}",
                        event.getId(), event.getEventId(), e);
                return;
            }

            outboxMapper.markForRetry(event.getId(), nextRetry,
                    new Date(System.currentTimeMillis() + retryDelayMs(nextRetry)), message);
            log.warn("favorite outbox publish failed, will retry, id={}, eventId={}, retry={}",
                    event.getId(), event.getEventId(), nextRetry, e);
        }
    }

    private long retryDelayMs(int retry) {
        int shift = Math.min(retry, 6);
        return Math.min(300000L, (1L << shift) * 1000L);
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
