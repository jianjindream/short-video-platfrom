package fun.witt.relation.processor;

import fun.witt.common.service.CounterService;
import fun.witt.constant.Constant;
import fun.witt.mapper.FollowerMapper;
import fun.witt.relation.event.RelationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
public class RelationEventProcessor {

    @Autowired
    private FollowerMapper followerMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private CounterService counterService;

    public void process(RelationEvent event) {
        if (event == null || event.getFromUserId() == null || event.getToUserId() == null) {
            return;
        }

        String eventId = event.getEventId();
        if (eventId != null && !eventId.isEmpty()) {
            Boolean first = redisTemplate.opsForValue()
                    .setIfAbsent("dedup:rel:" + eventId, "1", Duration.ofHours(24));
            if (!Boolean.TRUE.equals(first)) {
                return;
            }
        }

        long fromUserId = event.getFromUserId();
        long toUserId = event.getToUserId();
        long score = event.getTimestamp() == null ? System.currentTimeMillis() : event.getTimestamp();

        if (Constant.RELATION_EVENT_FOLLOW_CREATED.equals(event.getType())) {
            followerMapper.insertIgnore(fromUserId, toUserId);
            redisTemplate.opsForZSet().add(String.format(Constant.REDIS_RELATION_FOLLOWING, fromUserId), String.valueOf(toUserId), score);
            redisTemplate.opsForZSet().add(String.format(Constant.REDIS_RELATION_FOLLOWER, toUserId), String.valueOf(fromUserId), score);
            expireRelationKeys(fromUserId, toUserId);
            counterService.incrCounter(fromUserId, Constant.OFFSET_FOLLOW_COUNT, 1);
            counterService.incrCounter(toUserId, Constant.OFFSET_FOLLOWER_COUNT, 1);
        } else if (Constant.RELATION_EVENT_FOLLOW_CANCELED.equals(event.getType())) {
            followerMapper.deleteByUsers(fromUserId, toUserId);
            redisTemplate.opsForZSet().remove(String.format(Constant.REDIS_RELATION_FOLLOWING, fromUserId), String.valueOf(toUserId));
            redisTemplate.opsForZSet().remove(String.format(Constant.REDIS_RELATION_FOLLOWER, toUserId), String.valueOf(fromUserId));
            expireRelationKeys(fromUserId, toUserId);
            counterService.incrCounter(fromUserId, Constant.OFFSET_FOLLOW_COUNT, -1);
            counterService.incrCounter(toUserId, Constant.OFFSET_FOLLOWER_COUNT, -1);
        } else {
            log.warn("unknown relation event type: {}", event.getType());
        }
    }

    private void expireRelationKeys(long fromUserId, long toUserId) {
        redisTemplate.expire(String.format(Constant.REDIS_RELATION_FOLLOWING, fromUserId), Duration.ofHours(2));
        redisTemplate.expire(String.format(Constant.REDIS_RELATION_FOLLOWER, toUserId), Duration.ofHours(2));
    }
}
