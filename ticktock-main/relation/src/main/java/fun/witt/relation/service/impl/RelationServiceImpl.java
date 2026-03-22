package fun.witt.relation.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fun.witt.api.feign.UserFeignClient;
import fun.witt.api.vo.ResultVO;
import fun.witt.api.vo.UserExt;
import fun.witt.api.vo.UserListVO;
import fun.witt.constant.Constant;
import fun.witt.mapper.RelationMapper;
import fun.witt.model.Outbox;
import fun.witt.model.Relation;
import fun.witt.mapper.OutboxMapper;
import fun.witt.relation.service.RelationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RelationServiceImpl implements RelationService {

    @Autowired
    private RelationMapper relationMapper;

    @Autowired
    private OutboxMapper outboxMapper;

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 同步写：在一个事务中同时 INSERT following + INSERT outbox
     * 保证核心关系链绝对不丢
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResultVO followAction(String actionType, long toUserId, long fromUserId) {
        switch (actionType) {
            case Constant.RELATION_FOLLOW: {
                Relation relation = new Relation();
                relation.setFollowId(toUserId);
                relation.setFollowerId(fromUserId);
                if (relationMapper.insert(relation) <= 0) {
                    return ResultVO.fail("关注失败");
                }
                insertOutboxEvent(Constant.AGG_TYPE_FOLLOW, fromUserId, toUserId);
                writeFollowCountDelta(fromUserId, toUserId, 1);
                return ResultVO.ok();
            }
            case Constant.RELATION_UNFOLLOW: {
                Example example = new Example(Relation.class);
                Example.Criteria criteria = example.createCriteria();
                criteria.andEqualTo("followId", toUserId);
                criteria.andEqualTo("followerId", fromUserId);
                if (relationMapper.deleteByExample(example) <= 0) {
                    return ResultVO.fail("取消关注失败");
                }
                insertOutboxEvent(Constant.AGG_TYPE_UNFOLLOW, fromUserId, toUserId);
                writeFollowCountDelta(fromUserId, toUserId, -1);
                return ResultVO.ok();
            }
            default:
                return ResultVO.fail("无效的操作类型");
        }
    }

    private void insertOutboxEvent(String aggregateType, long fromUserId, long toUserId) {
        try {
            Map<String, Long> payload = new HashMap<>(4);
            payload.put("fromUserId", fromUserId);
            payload.put("toUserId", toUserId);

            Outbox outbox = new Outbox();
            outbox.setAggregateType(aggregateType);
            outbox.setAggregateId(fromUserId + ":" + toUserId);
            outbox.setPayload(objectMapper.writeValueAsString(payload));
            outbox.setCreatedAt(new Date());
            outbox.setProcessed(false);
            outboxMapper.insert(outbox);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化 Outbox payload 失败", e);
        }
    }

    /**
     * 写入关注/粉丝计数增量到 Redis 聚合桶
     * 计数系统(Module 3)的定时任务会读取这些增量并刷入 BITFIELD
     */
    private void writeFollowCountDelta(long fromUserId, long toUserId, int delta) {
        long timeSlot = System.currentTimeMillis() / 3600000;

        String fromAggKey = String.format("agg:ucount:%d:%d", fromUserId, timeSlot);
        String toAggKey = String.format("agg:ucount:%d:%d", toUserId, timeSlot);
        String activeKey = String.format(Constant.REDIS_ACTIVE_SET, timeSlot);

        // from_user 的关注数 +/- delta
        redisTemplate.opsForHash().increment(fromAggKey, "follow", delta);
        redisTemplate.opsForSet().add(activeKey, fromAggKey);

        // to_user 的粉丝数 +/- delta
        redisTemplate.opsForHash().increment(toAggKey, "follower", delta);
        redisTemplate.opsForSet().add(activeKey, toAggKey);
    }

    @Override
    public boolean followState(long userID, long loginUserID) {
        Example example = new Example(Relation.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("followId", userID);
        criteria.andEqualTo("followerId", loginUserID);
        Relation relation = relationMapper.selectOneByExample(example);
        return relation != null;
    }

    @Override
    public ResultVO followList(long userID, long loginUserID) {
        Example example = new Example(Relation.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("followerId", userID);
        List<Relation> relationList = relationMapper.selectByExample(example);

        UserListVO vo = new UserListVO();
        List<Long> followUserIDList = relationList.stream().map(Relation::getFollowId).collect(Collectors.toList());
        if (followUserIDList.isEmpty()) {
            return vo;
        }

        Map<Long, Boolean> followStateDict = relationList.stream()
                .collect(Collectors.toMap(Relation::getFollowId,
                        relation -> userID == loginUserID || relation.getFollowId() == loginUserID));

        List<UserExt> userExtList = userFeignClient.batchUserInfo(followUserIDList, 0);
        userExtList = userExtList.stream()
                .peek(userExt -> userExt.setFollow(followStateDict.getOrDefault(userExt.getId(), false)))
                .collect(Collectors.toList());
        vo.setUserList(userExtList);
        return vo;
    }

    @Override
    public ResultVO followerList(long userID, long loginUserID) {
        Example example = new Example(Relation.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("followId", userID);
        List<Relation> relationList = relationMapper.selectByExample(example);

        UserListVO vo = new UserListVO();
        List<Long> followerUserIDList = relationList.stream().map(Relation::getFollowerId).collect(Collectors.toList());
        if (followerUserIDList.isEmpty()) {
            return vo;
        }

        Map<Long, Boolean> followerStateDict = relationList.stream()
                .collect(Collectors.toMap(Relation::getFollowerId,
                        relation -> relation.getFollowerId() == loginUserID));

        List<UserExt> userExtList = userFeignClient.batchUserInfo(followerUserIDList, 0);
        userExtList = userExtList.stream()
                .peek(userExt -> userExt.setFollow(followerStateDict.getOrDefault(userExt.getId(), false)))
                .collect(Collectors.toList());
        vo.setUserList(userExtList);
        return vo;
    }
}
