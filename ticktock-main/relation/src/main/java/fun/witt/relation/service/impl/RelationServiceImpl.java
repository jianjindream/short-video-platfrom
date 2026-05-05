package fun.witt.relation.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fun.witt.api.feign.UserFeignClient;
import fun.witt.api.vo.ResultVO;
import fun.witt.api.vo.UserExt;
import fun.witt.api.vo.UserListVO;
import fun.witt.constant.Constant;
import fun.witt.mapper.OutboxMapper;
import fun.witt.mapper.RelationMapper;
import fun.witt.model.Outbox;
import fun.witt.model.Relation;
import fun.witt.relation.event.RelationEvent;
import fun.witt.relation.service.RelationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RelationServiceImpl implements RelationService {

    private static final int CACHE_BACKFILL_LIMIT = 1000;

    @Autowired
    private RelationMapper relationMapper;

    @Autowired
    private OutboxMapper outboxMapper;

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResultVO followAction(String actionType, long toUserId, long fromUserId) {
        if (toUserId <= 0 || fromUserId <= 0) {
            return ResultVO.fail("invalid user id");
        }
        if (toUserId == fromUserId) {
            return ResultVO.fail("can not follow yourself");
        }

        if (Constant.RELATION_FOLLOW.equals(actionType)) {
            int inserted = relationMapper.insertIgnore(fromUserId, toUserId);
            if (inserted > 0) {
                insertOutboxEvent(Constant.RELATION_EVENT_FOLLOW_CREATED, fromUserId, toUserId);
            }
            return ResultVO.ok();
        }

        if (Constant.RELATION_UNFOLLOW.equals(actionType)) {
            int deleted = relationMapper.deleteByUsers(fromUserId, toUserId);
            if (deleted > 0) {
                insertOutboxEvent(Constant.RELATION_EVENT_FOLLOW_CANCELED, fromUserId, toUserId);
            }
            return ResultVO.ok();
        }

        return ResultVO.fail("invalid action type");
    }

    private void insertOutboxEvent(String eventType, long fromUserId, long toUserId) {
        try {
            RelationEvent event = new RelationEvent(
                    UUID.randomUUID().toString(),
                    eventType,
                    fromUserId,
                    toUserId,
                    System.currentTimeMillis());

            Outbox outbox = new Outbox();
            outbox.setAggregateType("following");
            outbox.setAggregateId(fromUserId + ":" + toUserId);
            outbox.setPayload(objectMapper.writeValueAsString(event));
            outbox.setCreatedAt(new Date());
            outbox.setProcessed(false);
            outboxMapper.insert(outbox);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("serialize relation outbox event failed", e);
        }
    }

    @Override
    public boolean followState(long userID, long loginUserID) {
        return relationMapper.existsByUsers(loginUserID, userID) > 0;
    }

    @Override
    public ResultVO followList(long userID, long loginUserID) {
        List<Long> followUserIds = readFollowingIds(userID);
        UserListVO vo = new UserListVO();
        if (followUserIds.isEmpty()) {
            vo.setUserList(new ArrayList<>());
            return vo;
        }

        List<UserExt> userExtList = userFeignClient.batchUserInfo(followUserIds, 0);
        for (UserExt userExt : userExtList) {
            userExt.setFollow(userID == loginUserID || relationMapper.existsByUsers(loginUserID, userExt.getId()) > 0);
        }
        vo.setUserList(userExtList);
        return vo;
    }

    @Override
    public ResultVO followerList(long userID, long loginUserID) {
        List<Long> followerUserIds = readFollowerIds(userID);
        UserListVO vo = new UserListVO();
        if (followerUserIds.isEmpty()) {
            vo.setUserList(new ArrayList<>());
            return vo;
        }

        List<UserExt> userExtList = userFeignClient.batchUserInfo(followerUserIds, 0);
        for (UserExt userExt : userExtList) {
            userExt.setFollow(relationMapper.existsByUsers(loginUserID, userExt.getId()) > 0);
        }
        vo.setUserList(userExtList);
        return vo;
    }

    private List<Long> readFollowingIds(long userId) {
        String key = String.format(Constant.REDIS_RELATION_FOLLOWING, userId);
        List<Long> cached = readZSetIds(key);
        if (!cached.isEmpty()) {
            return cached;
        }

        List<Relation> rows = relationMapper.listFollowingRows(userId, CACHE_BACKFILL_LIMIT);
        for (Relation row : rows) {
            redisTemplate.opsForZSet().add(key, String.valueOf(row.getFollowId()), score(row));
        }
        redisTemplate.expire(key, Duration.ofHours(2));
        return rows.stream().map(Relation::getFollowId).collect(Collectors.toList());
    }

    private List<Long> readFollowerIds(long userId) {
        String key = String.format(Constant.REDIS_RELATION_FOLLOWER, userId);
        List<Long> cached = readZSetIds(key);
        if (!cached.isEmpty()) {
            return cached;
        }

        List<Relation> rows = relationMapper.listFollowerRows(userId, CACHE_BACKFILL_LIMIT);
        for (Relation row : rows) {
            redisTemplate.opsForZSet().add(key, String.valueOf(row.getFollowerId()), score(row));
        }
        redisTemplate.expire(key, Duration.ofHours(2));
        return rows.stream().map(Relation::getFollowerId).collect(Collectors.toList());
    }

    private List<Long> readZSetIds(String key) {
        Set<String> values = redisTemplate.opsForZSet().reverseRange(key, 0, CACHE_BACKFILL_LIMIT - 1);
        List<Long> ids = new ArrayList<>();
        if (values == null || values.isEmpty()) {
            return ids;
        }
        for (String value : values) {
            try {
                ids.add(Long.parseLong(value));
            } catch (NumberFormatException ignored) {
            }
        }
        return ids;
    }

    private long score(Relation relation) {
        if (relation.getCreatedAt() != null) {
            return relation.getCreatedAt().getTime();
        }
        if (relation.getId() != null) {
            return relation.getId();
        }
        return System.currentTimeMillis();
    }
}
