package fun.witt.video.service;

import fun.witt.api.vo.ResultVO;
import fun.witt.api.vo.VideoListVO;
import org.springframework.web.multipart.MultipartFile;

public interface VideoService {

    ResultVO publish(long userID, String title, MultipartFile file);

    VideoListVO listVideo(long userID, long loginUserID);

    VideoListVO feedVideo(long loginUserID, long lastTime, int count);

    VideoListVO hotFeedVideo(long loginUserID, double maxScore, int count);

    VideoListVO listFavoriteVideo(long userID, long loginUserID);

    VideoListVO listCollectVideo(long userID, long loginUserID);

}
