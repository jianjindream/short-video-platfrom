package fun.witt.video.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import fun.witt.api.feign.CollectFeignClient;
import fun.witt.api.feign.FavoriteFeignClient;
import fun.witt.api.feign.UserFeignClient;
import fun.witt.api.utils.ConvertUtil;
import fun.witt.api.vo.ResultVO;
import fun.witt.api.vo.VideoExt;
import fun.witt.api.vo.VideoListVO;
import fun.witt.common.service.CounterService;
import fun.witt.common.template.MinioTemplate;
import fun.witt.mapper.VideoMapper;
import fun.witt.model.Video;
import fun.witt.video.service.FeedCacheService;
import fun.witt.video.service.VideoService;
import fun.witt.video.utils.FfmpegUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tk.mybatis.mapper.entity.Example;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class VideoServiceImpl implements VideoService {
    private static final String NORM_DAY_PATTERN = "yyyy-MM-dd";

    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private MinioTemplate minioTemplate;

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private FavoriteFeignClient favoriteFeignClient;

    @Autowired
    private CollectFeignClient collectFeignClient;

    @Autowired
    private CounterService counterService;

    @Autowired
    private FeedCacheService feedCacheService;

    @Override
    public ResultVO publish(long userID, String title, MultipartFile file) {
        // fixme Exception & filename & upload & frame content type
        String filePath = DateUtil.format(new Date(), NORM_DAY_PATTERN) + "/" + IdUtil.simpleUUID();
        String fileSuffix = Objects.requireNonNull(file.getOriginalFilename())
                .substring(file.getOriginalFilename().lastIndexOf("."));

        String videoURL;
        try {
            videoURL = filePath + fileSuffix;
            minioTemplate.uploadFile(file.getBytes(), videoURL, file.getContentType());
        } catch (IOException e) {
            e.printStackTrace();
            return ResultVO.fail("publish fail");
        }

        String coverURL;
        try {
            coverURL = filePath + ".jpg";
            byte[] videoFrame = FfmpegUtils.videoFrame(file);
            minioTemplate.uploadFile(videoFrame, coverURL, "image/jpeg");
        } catch (IOException e) {
            e.printStackTrace();
            return ResultVO.fail("cut video frame fail");
        }

        Video video = new Video();
        video.setFavoriteCount(0L);
        video.setCommentCount(0L);
        video.setAuthorId(userID);
        video.setTitle(title);
        video.setPlayUrl(videoURL);
        video.setCoverUrl(coverURL);
        video.setPublishTime(new Date());
        if (videoMapper.insert(video) <= 0) {
            return ResultVO.fail("publish fail");
        }
        feedCacheService.addVideo(video);
        return ResultVO.ok();
    }

    @Override
    public VideoListVO listVideo(long userID, long loginUserID) {
        Example example = new Example(Video.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("authorId", userID);
        List<Video> videoList = videoMapper.selectByExample(example);
        List<VideoExt> videoExtList = convertVideoExtList(videoList, loginUserID);
        VideoListVO vo = new VideoListVO();
        vo.setVideoList(videoExtList);
        return vo;
    }

    @Override
    public VideoListVO feedVideo(long loginUserID, long latestTime, int count) {
        DateTime date = DateUtil.date(latestTime);
        List<Video> videoList = videoMapper.queryVideoOrderByLatestTime(date.toJdkDate(), count);
        List<VideoExt> videoExtList = convertVideoExtList(videoList, loginUserID);

        VideoListVO vo = new VideoListVO();
        vo.setFeedType("latest");
        vo.setVideoList(videoExtList);
        vo.setNextTime(nextLatestTime(videoList, latestTime));
        return vo;
    }

    @Override
    public VideoListVO hotFeedVideo(long loginUserID, double maxScore, int count) {
        FeedCacheService.HotFeedPage page = feedCacheService.listHotVideos(maxScore, count);
        List<Video> videoList = page.isCacheAvailable()
                ? page.getVideos()
                : queryHotFeedFromDb(maxScore, count);
        List<VideoExt> videoExtList = convertVideoExtList(videoList, loginUserID);

        VideoListVO vo = new VideoListVO();
        vo.setFeedType("hot");
        vo.setVideoList(videoExtList);
        vo.setNextScore(page.isCacheAvailable() ? page.getNextScore() : nextHotScore(videoList));
        vo.setNextTime(nextLatestTime(videoList, System.currentTimeMillis()));
        return vo;
    }

    @Override
    public VideoListVO listFavoriteVideo(long userID, long loginUserID) {
        List<Long> videoIDList = favoriteFeignClient.listUserFavoriteVideo(userID);
        if (videoIDList.isEmpty()) {
            return new VideoListVO();
        }

        Example example = new Example(Video.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andIn("id", videoIDList);
        List<Video> videoList = videoMapper.selectByExample(example);
        List<VideoExt> videoExtList = convertVideoExtList(videoList, loginUserID);
        VideoListVO vo = new VideoListVO();
        vo.setVideoList(videoExtList);
        return vo;
    }

    @Override
    public VideoListVO listCollectVideo(long userID, long loginUserID) {
        List<Long> videoIDList = collectFeignClient.listUserCollectVideo(userID);
        if (videoIDList.isEmpty()) {
            return new VideoListVO();
        }

        Example example = new Example(Video.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andIn("id", videoIDList);
        List<Video> videoList = videoMapper.selectByExample(example);
        List<VideoExt> videoExtList = convertVideoExtList(videoList, loginUserID);
        VideoListVO vo = new VideoListVO();
        vo.setVideoList(videoExtList);
        return vo;
    }

    private List<Video> queryHotFeedFromDb(double maxScore, int count) {
        int queryCount = Double.isInfinite(maxScore) ? count : Math.min(count * 5, 200);
        List<Video> candidates = videoMapper.queryVideoOrderByHotScore(new Date(System.currentTimeMillis() + 1000), queryCount);
        if (Double.isInfinite(maxScore)) {
            return candidates;
        }
        return candidates.stream()
                .filter(video -> feedCacheService.hotScore(video) < maxScore)
                .limit(count)
                .collect(Collectors.toList());
    }

    private long nextLatestTime(List<Video> videoList, long defaultTime) {
        return videoList.stream()
                .min(Comparator.comparing(Video::getPublishTime))
                .map(video -> video.getPublishTime().getTime() / 1000)
                .orElse(defaultTime / 1000);
    }

    private Double nextHotScore(List<Video> videoList) {
        if (videoList == null || videoList.isEmpty()) {
            return null;
        }
        return videoList.stream()
                .map(feedCacheService::hotScore)
                .min(Double::compareTo)
                .orElse(null);
    }

    private List<VideoExt> convertVideoExtList(List<Video> videoList, long loginUserID) {
        if (videoList == null || videoList.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> videoIDList = videoList.stream().map(Video::getId).collect(Collectors.toList());
        Map<Long, Boolean> favoriteStateDict = favoriteFeignClient.batchFavoriteState(videoIDList, loginUserID);
        Map<Long, Boolean> collectStateDict = collectFeignClient.batchCollectState(videoIDList, loginUserID);
        Map<Long, Long> favoriteCountDict = counterService.getVideoLikeCounts(videoIDList);
        Map<Long, Long> collectCountDict = counterService.getVideoCollectCounts(videoIDList);

        return videoList.stream()
                .map(video -> {
                    // fixme batch request
                    VideoExt videoExt = ConvertUtil.convertVideo(video);
                    videoExt.setPlayUrl(minioTemplate.getObjectUrl(video.getPlayUrl()));
                    videoExt.setCoverUrl(minioTemplate.getObjectUrl(video.getCoverUrl()));
                    videoExt.setAuthor(userFeignClient.getUserInfo(video.getAuthorId(), loginUserID));
                    videoExt.setFavorite(favoriteStateDict.getOrDefault(video.getId(), false));
                    videoExt.setCollect(collectStateDict.getOrDefault(video.getId(), false));
                    Long redisFavoriteCount = favoriteCountDict.get(video.getId());
                    if (redisFavoriteCount != null && (redisFavoriteCount > 0 || video.getFavoriteCount() == null || video.getFavoriteCount() == 0)) {
                        videoExt.setFavoriteCount(redisFavoriteCount);
                    }
                    Long redisCollectCount = collectCountDict.get(video.getId());
                    if (redisCollectCount != null) {
                        videoExt.setCollectCount(redisCollectCount);
                    }
                    return videoExt;
                })
                .collect(Collectors.toList());
    }
}
