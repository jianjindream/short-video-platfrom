package fun.witt.video.service;

import fun.witt.common.service.CounterService;
import fun.witt.constant.Constant;
import fun.witt.mapper.VideoMapper;
import fun.witt.model.Video;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FeedCacheService {
    private static final int HOT_CACHE_SIZE = 1000;
    private static final int HOT_REBUILD_QUERY_SIZE = 1000;
    private static final double LIKE_WEIGHT = 3D;
    private static final double COMMENT_WEIGHT = 4D;
    private static final double COLLECT_WEIGHT = 5D;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private CounterService counterService;

    public HotFeedPage listHotVideos(double maxScore, int count) {
        try {
            if (!ensureHotCache()) {
                return HotFeedPage.cacheMiss();
            }

            double scoreLimit = Double.isInfinite(maxScore) ? Double.MAX_VALUE : Math.nextDown(maxScore);
            Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                    .reverseRangeByScoreWithScores(Constant.REDIS_HOT_FEED_ZSET, 0D, scoreLimit, 0, count);
            if (tuples == null || tuples.isEmpty()) {
                return new HotFeedPage(Collections.<Video>emptyList(), null, true);
            }

            List<Long> videoIds = new ArrayList<Long>(tuples.size());
            Double nextScore = null;
            for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                Long videoId = parseLong(tuple.getValue());
                if (videoId != null) {
                    videoIds.add(videoId);
                    nextScore = tuple.getScore();
                }
            }
            if (videoIds.isEmpty()) {
                return new HotFeedPage(Collections.<Video>emptyList(), null, true);
            }

            List<Video> videos = videoMapper.selectVideoByIdList(videoIds);
            Map<Long, Video> videoDict = new HashMap<Long, Video>(videos.size());
            for (Video video : videos) {
                videoDict.put(video.getId(), video);
            }

            List<Video> orderedVideos = new ArrayList<Video>(videoIds.size());
            for (Long videoId : videoIds) {
                Video video = videoDict.get(videoId);
                if (video != null) {
                    orderedVideos.add(video);
                }
            }
            return new HotFeedPage(orderedVideos, nextScore, true);
        } catch (Exception e) {
            log.warn("query hot feed cache failed, fallback to db", e);
            return HotFeedPage.cacheMiss();
        }
    }

    public boolean rebuildHotFeedCache() {
        try {
            List<Video> candidates = videoMapper.queryVideoOrderByHotScore(new Date(System.currentTimeMillis() + 1000), HOT_REBUILD_QUERY_SIZE);
            redisTemplate.delete(Constant.REDIS_HOT_FEED_ZSET);
            if (candidates == null || candidates.isEmpty()) {
                return true;
            }

            Map<Long, Long> likeCounts = loadLikeCounts(candidates);
            Map<Long, Long> collectCounts = loadCollectCounts(candidates);
            for (Video video : candidates) {
                redisTemplate.opsForZSet().add(Constant.REDIS_HOT_FEED_ZSET,
                        String.valueOf(video.getId()), hotScore(video, likeCounts, collectCounts));
            }
            trimHotCache();
            return true;
        } catch (Exception e) {
            log.warn("rebuild hot feed cache failed", e);
            return false;
        }
    }

    public void addVideo(Video video) {
        if (video == null || video.getId() == null) {
            return;
        }
        try {
            redisTemplate.opsForZSet().add(Constant.REDIS_HOT_FEED_ZSET, String.valueOf(video.getId()), hotScore(video));
            trimHotCache();
        } catch (Exception e) {
            log.warn("add video to hot feed cache failed, videoId={}", video.getId(), e);
        }
    }

    public double hotScore(Video video) {
        return hotScore(video, Collections.<Long, Long>emptyMap(), Collections.<Long, Long>emptyMap());
    }

    private double hotScore(Video video, Map<Long, Long> likeCounts, Map<Long, Long> collectCounts) {
        long videoId = video.getId() == null ? 0L : video.getId();
        long favoriteCount = firstPositive(likeCounts.get(videoId), video.getFavoriteCount());
        long commentCount = video.getCommentCount() == null ? 0L : video.getCommentCount();
        long collectCount = firstPositive(collectCounts.get(videoId), 0L);
        long publishTime = video.getPublishTime() == null ? 0L : video.getPublishTime().getTime();
        return publishTime / 3600000D
                + favoriteCount * LIKE_WEIGHT
                + commentCount * COMMENT_WEIGHT
                + collectCount * COLLECT_WEIGHT
                + videoId / 1000000000D;
    }

    private boolean ensureHotCache() {
        Long size = redisTemplate.opsForZSet().zCard(Constant.REDIS_HOT_FEED_ZSET);
        return size != null && size > 0 || rebuildHotFeedCache();
    }

    private void trimHotCache() {
        Long size = redisTemplate.opsForZSet().zCard(Constant.REDIS_HOT_FEED_ZSET);
        if (size != null && size > HOT_CACHE_SIZE) {
            redisTemplate.opsForZSet().removeRange(Constant.REDIS_HOT_FEED_ZSET, 0, size - HOT_CACHE_SIZE - 1);
        }
    }

    private Map<Long, Long> loadLikeCounts(List<Video> videos) {
        try {
            return counterService.getVideoLikeCounts(videoIds(videos));
        } catch (Exception e) {
            log.warn("load redis like counts for hot feed failed", e);
            return Collections.emptyMap();
        }
    }

    private Map<Long, Long> loadCollectCounts(List<Video> videos) {
        try {
            return counterService.getVideoCollectCounts(videoIds(videos));
        } catch (Exception e) {
            log.warn("load redis collect counts for hot feed failed", e);
            return Collections.emptyMap();
        }
    }

    private List<Long> videoIds(List<Video> videos) {
        return videos.stream().map(Video::getId).collect(Collectors.toList());
    }

    private long firstPositive(Long primary, Long fallback) {
        if (primary != null && primary > 0) {
            return primary;
        }
        return fallback == null ? 0L : fallback;
    }

    private Long parseLong(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static class HotFeedPage {
        private final List<Video> videos;
        private final Double nextScore;
        private final boolean cacheAvailable;

        public HotFeedPage(List<Video> videos, Double nextScore, boolean cacheAvailable) {
            this.videos = videos;
            this.nextScore = nextScore;
            this.cacheAvailable = cacheAvailable;
        }

        public static HotFeedPage cacheMiss() {
            return new HotFeedPage(Collections.<Video>emptyList(), null, false);
        }

        public List<Video> getVideos() {
            return videos;
        }

        public Double getNextScore() {
            return nextScore;
        }

        public boolean isCacheAvailable() {
            return cacheAvailable;
        }
    }
}
