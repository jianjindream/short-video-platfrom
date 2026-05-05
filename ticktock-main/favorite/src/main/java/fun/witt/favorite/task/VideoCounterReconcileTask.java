package fun.witt.favorite.task;

import fun.witt.common.service.CounterService;
import fun.witt.mapper.CollectMapper;
import fun.witt.mapper.FavoriteMapper;
import fun.witt.mapper.VideoMapper;
import fun.witt.model.Video;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Slf4j
@Component
public class VideoCounterReconcileTask {

    private static final int BATCH_SIZE = 50;
    private long lastVideoId = 0;

    @Autowired
    private CounterService counterService;

    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private CollectMapper collectMapper;

    @Scheduled(fixedDelay = 600000)
    public void reconcile() {
        Example example = new Example(Video.class);
        example.createCriteria().andGreaterThan("id", lastVideoId);
        example.setOrderByClause("id ASC");
        List<Video> videos = videoMapper.selectByExample(example);
        if (videos.isEmpty()) {
            lastVideoId = 0;
            return;
        }

        int checked = 0;
        int fixed = 0;
        for (Video video : videos) {
            if (checked >= BATCH_SIZE) {
                break;
            }
            checked++;
            long videoId = video.getId();
            long redisCount = counterService.getVideoLikeCount(videoId);
            long redisCollectCount = counterService.getVideoCollectCount(videoId);
            long dbLikeCount = favoriteMapper.countByVideo(videoId);
            long dbCollectCount = collectMapper.countByVideo(videoId);
            if (redisCount != dbLikeCount) {
                counterService.setVideoLikeCount(videoId, dbLikeCount);
                fixed++;
            }
            if (redisCollectCount != dbCollectCount) {
                counterService.setVideoCollectCount(videoId, dbCollectCount);
                fixed++;
            }
            lastVideoId = videoId;
        }

        if (fixed > 0) {
            log.info("video counter reconcile fixed {} videos", fixed);
        }
    }
}
