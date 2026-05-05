package fun.witt.video.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import fun.witt.api.feign.FavoriteFeignClient;
import fun.witt.api.feign.CollectFeignClient;
import fun.witt.api.feign.UserFeignClient;
import fun.witt.api.utils.ConvertUtil;
import fun.witt.api.vo.ResultVO;
import fun.witt.api.vo.VideoExt;
import fun.witt.api.vo.VideoListVO;
import fun.witt.common.service.CounterService;
import fun.witt.common.template.MinioTemplate;
import fun.witt.mapper.VideoMapper;
import fun.witt.model.Video;
import fun.witt.video.service.VideoService;
import fun.witt.video.utils.FfmpegUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tk.mybatis.mapper.entity.Example;

import java.io.IOException;
import java.util.*;
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
        vo.setVideoList(videoExtList);

        Video defaultVideo = new Video();
        defaultVideo.setPublishTime(new Date());

        Date maxDate = videoList.stream()
                .max(Comparator.comparing(Video::getPublishTime))
                .orElse(defaultVideo)
                .getPublishTime();
        vo.setNextTime(maxDate.getTime() / 1000);
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

    private List<VideoExt> convertVideoExtList(List<Video> videoList, long loginUserID) {
        List<Long> videoIDList = videoList.parallelStream().map(Video::getId).collect(Collectors.toList());
        Map<Long, Boolean> favoriteStateDict = favoriteFeignClient.batchFavoriteState(videoIDList, loginUserID);
        Map<Long, Boolean> collectStateDict = collectFeignClient.batchCollectState(videoIDList, loginUserID);
        Map<Long, Long> favoriteCountDict = counterService.getVideoLikeCounts(videoIDList);
        Map<Long, Long> collectCountDict = counterService.getVideoCollectCounts(videoIDList);

        return videoList.parallelStream()
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
