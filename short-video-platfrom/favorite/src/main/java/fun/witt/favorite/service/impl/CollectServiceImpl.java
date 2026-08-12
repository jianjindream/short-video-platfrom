package fun.witt.favorite.service.impl;

import fun.witt.constant.Constant;
import fun.witt.favorite.service.CollectCacheService;
import fun.witt.favorite.service.CollectService;
import fun.witt.favorite.service.FavoriteOutboxService;
import fun.witt.mapper.CollectMapper;
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
public class CollectServiceImpl implements CollectService {

    private static final String ACTION_COLLECT = "COLLECT";
    private static final String ACTION_UNCOLLECT = "UNCOLLECT";

    @Autowired
    private CollectMapper collectMapper;

    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private CollectCacheService collectCacheService;

    @Autowired
    private FavoriteOutboxService favoriteOutboxService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean collectAction(String actionType, long videoId, long userId) {
        Long authorId = videoMapper.selectAuthorId(videoId);
        if (authorId == null) {
            log.warn("collect action ignored because video not found, videoId={}, userId={}", videoId, userId);
            return false;
        }

        if (Constant.COLLECT_COLLECT.equals(actionType)) {
            return collect(videoId, userId);
        }
        if (Constant.COLLECT_UNCOLLECT.equals(actionType)) {
            return uncollect(videoId, userId);
        }
        return false;
    }

    private boolean collect(long videoId, long userId) {
        int inserted = collectMapper.insertIgnore(userId, videoId);
        if (inserted > 0) {
            favoriteOutboxService.addCollectEvent(videoId, userId, ACTION_COLLECT, 1);
        }
        afterCommit(() -> collectCacheService.setCollected(videoId, userId),
                "refresh collect bitmap failed, videoId={}, userId={}", videoId, userId);
        return true;
    }

    private boolean uncollect(long videoId, long userId) {
        int deleted = collectMapper.deleteByUserAndVideo(userId, videoId);
        if (deleted > 0) {
            favoriteOutboxService.addCollectEvent(videoId, userId, ACTION_UNCOLLECT, -1);
        }
        afterCommit(() -> collectCacheService.setUncollected(videoId, userId),
                "refresh uncollect bitmap failed, videoId={}, userId={}", videoId, userId);
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
    public boolean isCollected(long videoId, long userId) {
        return collectCacheService.isCollected(videoId, userId);
    }

    @Override
    public Map<Long, Boolean> batchCollectState(List<Long> videoIdList, long userId) {
        Map<Long, Boolean> result = new HashMap<>(videoIdList.size());
        for (Long videoId : videoIdList) {
            result.put(videoId, collectCacheService.isCollected(videoId, userId));
        }
        return result;
    }
}
