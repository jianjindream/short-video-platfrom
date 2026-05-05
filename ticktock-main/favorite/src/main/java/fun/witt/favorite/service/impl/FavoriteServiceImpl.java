package fun.witt.favorite.service.impl;

import fun.witt.constant.Constant;
import fun.witt.favorite.kafka.LikeEventProducer;
import fun.witt.favorite.service.FavoriteService;
import fun.witt.favorite.service.LikeCacheService;
import fun.witt.mapper.VideoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class FavoriteServiceImpl implements FavoriteService {

    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private LikeCacheService likeCacheService;

    @Autowired
    private LikeEventProducer likeEventProducer;

    @Override
    public boolean likeAction(String actionType, long videoID, long userID, long authorIdFromRequest) {
        Long authorId = videoMapper.selectAuthorId(videoID);
        if (authorId == null) {
            log.warn("like action ignored because video not found, videoId={}, userId={}", videoID, userID);
            return false;
        }

        if (Constant.FAVORITE_LIKE.equals(actionType)) {
            return like(videoID, userID, authorId);
        }
        if (Constant.FAVORITE_UNLIKE.equals(actionType)) {
            return unlike(videoID, userID, authorId);
        }
        return false;
    }

    private boolean like(long videoId, long userId, long authorId) {
        boolean changed = likeCacheService.setLiked(videoId, userId);
        if (!changed) {
            return true;
        }

        return likeEventProducer.sendLikeEvent(
                UUID.randomUUID().toString(), videoId, userId, authorId, "LIKE", 1);
    }

    private boolean unlike(long videoId, long userId, long authorId) {
        boolean changed = likeCacheService.setUnliked(videoId, userId);
        if (!changed) {
            return true;
        }

        return likeEventProducer.sendLikeEvent(
                UUID.randomUUID().toString(), videoId, userId, authorId, "UNLIKE", -1);
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
