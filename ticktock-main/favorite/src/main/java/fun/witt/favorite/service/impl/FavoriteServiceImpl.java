package fun.witt.favorite.service.impl;

import fun.witt.constant.Constant;
import fun.witt.favorite.kafka.LikeEventProducer;
import fun.witt.favorite.service.FavoriteService;
import fun.witt.favorite.service.LikeCacheService;
import fun.witt.mapper.FavoriteMapper;
import fun.witt.model.Favorite;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 点赞服务实现：极速响应 + 异步写聚合 + 容忍极端丢失换取高吞吐。
 *
 * 同步极速路径：
 *   1. Redis SETBIT feed:{feedId}:likes {userId} 1/0
 *   2. 返回 HTTP 200
 *
 * 异步事件生产：
 *   业务线程将增量事件封装，通过 Kafka 投递（acks=all, 微批处理）
 *
 * DB 写入保留作为最终一致性兜底
 */
@Slf4j
@Service
public class FavoriteServiceImpl implements FavoriteService {

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private LikeCacheService likeCacheService;

    @Autowired
    private LikeEventProducer likeEventProducer;

    @Override
    public boolean likeAction(String actionType, long videoID, long userID, long authorId) {
        switch (actionType) {
            case Constant.FAVORITE_LIKE: {
                // 同步极速路径：Redis SETBIT，立即返回成功
                likeCacheService.setLiked(videoID, userID);

                // 异步事件生产：Kafka 投递增量事件
                likeEventProducer.sendLikeEvent(videoID, userID, authorId, "LIKE", 1);

                // DB 写入作为兜底（异步或同步均可，此处保持同步以兼容现有逻辑）
                try {
                    Favorite favorite = new Favorite();
                    favorite.setUserId(userID);
                    favorite.setVideoId(videoID);
                    favoriteMapper.insert(favorite);
                } catch (Exception e) {
                    log.warn("点赞 DB 写入失败(可能重复), videoId={}, userId={}", videoID, userID);
                }
                return true;
            }
            case Constant.FAVORITE_UNLIKE: {
                likeCacheService.setUnliked(videoID, userID);

                likeEventProducer.sendLikeEvent(videoID, userID, authorId, "UNLIKE", -1);

                try {
                    Example example = new Example(Favorite.class);
                    Example.Criteria criteria = example.createCriteria();
                    criteria.andEqualTo("videoId", videoID);
                    criteria.andEqualTo("userId", userID);
                    favoriteMapper.deleteByExample(example);
                } catch (Exception e) {
                    log.warn("取消点赞 DB 删除失败, videoId={}, userId={}", videoID, userID);
                }
                return true;
            }
            default:
                return false;
        }
    }

    @Override
    public boolean isLiked(long videoID, long userID) {
        return likeCacheService.isLiked(videoID, userID);
    }

    @Override
    public Map<Long, Boolean> batchLikeState(List<Long> videoIDList, long userID) {
        Map<Long, Boolean> result = new HashMap<>(videoIDList.size());
        for (Long videoId : videoIDList) {
            result.put(videoId, likeCacheService.isLiked(videoId, userID));
        }
        return result;
    }
}
