package fun.witt.favorite.service.impl;

import fun.witt.constant.Constant;
import fun.witt.favorite.kafka.CollectEventProducer;
import fun.witt.favorite.service.CollectCacheService;
import fun.witt.favorite.service.CollectService;
import fun.witt.mapper.CollectMapper;
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
public class CollectServiceImpl implements CollectService {

    @Autowired
    private CollectMapper collectMapper;

    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private CollectCacheService collectCacheService;

    @Autowired
    private CollectEventProducer collectEventProducer;

    @Override
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
        boolean changed = collectCacheService.setCollected(videoId, userId);
        if (!changed) {
            return true;
        }

        int inserted;
        try {
            inserted = collectMapper.insertIgnore(userId, videoId);
        } catch (Exception e) {
            log.error("write collect row failed, videoId={}, userId={}", videoId, userId, e);
            return false;
        }

        if (inserted > 0) {
            collectEventProducer.sendCollectEvent(UUID.randomUUID().toString(), videoId, userId, "COLLECT", 1);
        }
        return true;
    }

    private boolean uncollect(long videoId, long userId) {
        boolean changed = collectCacheService.setUncollected(videoId, userId);
        if (!changed) {
            return true;
        }

        int deleted;
        try {
            deleted = collectMapper.deleteByUserAndVideo(userId, videoId);
        } catch (Exception e) {
            log.error("delete collect row failed, videoId={}, userId={}", videoId, userId, e);
            return false;
        }

        if (deleted > 0) {
            collectEventProducer.sendCollectEvent(UUID.randomUUID().toString(), videoId, userId, "UNCOLLECT", -1);
        }
        return true;
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
