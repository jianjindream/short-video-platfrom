package fun.witt.favorite.kafka;

import fun.witt.mapper.MessageDeadLetterMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
public class MessageConsumeFailureHandler {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private MessageDeadLetterMapper deadLetterMapper;

    @Value("${favorite.consumer.max-retry:3}")
    private int maxRetry;

    public void retryOrDead(String eventType,
                            String consumerGroup,
                            ConsumerRecord<String, String> record,
                            String eventId,
                            String dedupKey,
                            boolean dedupAcquired,
                            Exception cause,
                            Acknowledgment ack) {
        if (dedupAcquired && dedupKey != null) {
            redisTemplate.delete(dedupKey);
        }

        String retryKey = retryKey(eventType, record, eventId);
        Long retry = redisTemplate.opsForValue().increment(retryKey);
        if (retry != null && retry == 1L) {
            redisTemplate.expire(retryKey, Duration.ofHours(24));
        }

        int retryCount = retry == null ? 1 : retry.intValue();
        if (retryCount >= maxRetry) {
            deadLetterMapper.insertDeadLetter(
                    fallbackEventId(record, eventId),
                    record.topic(),
                    consumerGroup,
                    record.value(),
                    truncate(cause.getMessage()),
                    retryCount);
            redisTemplate.delete(retryKey);
            ack.acknowledge();
            log.error("message moved to dead letter, topic={}, eventId={}, retry={}",
                    record.topic(), fallbackEventId(record, eventId), retryCount, cause);
            return;
        }

        log.warn("message consume failed, will retry, topic={}, eventId={}, retry={}",
                record.topic(), fallbackEventId(record, eventId), retryCount, cause);
        throw new IllegalStateException("message consume failed, retry=" + retryCount, cause);
    }

    private String retryKey(String eventType, ConsumerRecord<String, String> record, String eventId) {
        return "retry:consumer:" + eventType + ":" + fallbackEventId(record, eventId);
    }

    private String fallbackEventId(ConsumerRecord<String, String> record, String eventId) {
        if (eventId != null && !eventId.isEmpty()) {
            return eventId;
        }
        return record.topic() + ":" + record.partition() + ":" + record.offset();
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
