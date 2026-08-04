package fun.witt.favorite.service.impl;

import fun.witt.constant.Constant;
import fun.witt.favorite.service.FavoriteOutboxService;
import fun.witt.favorite.service.FavoriteService;
import fun.witt.favorite.service.LikeCacheService;
import fun.witt.mapper.FavoriteMapper;
import fun.witt.mapper.VideoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class FavoriteServiceImpl implements FavoriteService {

    private static final String ACTION_LIKE = "LIKE";
    private static final String ACTION_UNLIKE = "UNLIKE";

    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private LikeCacheService likeCacheService;

    @Autowired
    private FavoriteOutboxService favoriteOutboxService;

    @Override
    @Transactional(rollbackFor = Exception.class)
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
        int inserted = favoriteMapper.insertIgnore(userId, videoId);
        if (inserted > 0) {
            favoriteOutboxService.addLikeEvent(videoId, userId, authorId, ACTION_LIKE, 1);
        }
        afterCommit(() -> likeCacheService.setLiked(videoId, userId),
                "refresh like bitmap failed, videoId={}, userId={}", videoId, userId);
        return true;
    }

    private boolean unlike(long videoId, long userId, long authorId) {
        int deleted = favoriteMapper.deleteByUserAndVideo(userId, videoId);
        if (deleted > 0) {
            favoriteOutboxService.addLikeEvent(videoId, userId, authorId, ACTION_UNLIKE, -1);
        }
        afterCommit(() -> likeCacheService.setUnliked(videoId, userId),
                "refresh unlike bitmap failed, videoId={}, userId={}", videoId, userId);
        return true;
    }

    private void afterCommit(Runnable runnable, String errorMessage, long videoId, long userId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            runCacheUpdate(runnable, errorMessage, videoId, userId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                runCacheUpdate(runnable, errorMessage, videoId, userId);
            }
        });
    }

    private void runCacheUpdate(Runnable runnable, String errorMessage, long videoId, long userId) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.warn(errorMessage, videoId, userId, e);
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
