package fun.witt.video.controller;

import cn.hutool.core.util.StrUtil;
import fun.witt.api.vo.ResultVO;
import fun.witt.common.auth.LoginUser;
import fun.witt.video.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class FeedController {
    private static final int DEFAULT_LIMIT_COUNT = 30;
    private static final int MAX_LIMIT_COUNT = 50;

    @Autowired
    private VideoService videoService;

    @GetMapping("/feed")
    public ResultVO feed(@AuthenticationPrincipal LoginUser loginUser,
                         @RequestParam(name = "latest_time", required = false) String latestTime,
                         @RequestParam(name = "latestTime", required = false) String latestTimeCompat,
                         @RequestParam(name = "feed_type", required = false) String feedType,
                         @RequestParam(name = "feedType", required = false) String feedTypeCompat,
                         @RequestParam(name = "hot_score", required = false) String hotScore,
                         @RequestParam(name = "hotScore", required = false) String hotScoreCompat,
                         @RequestParam(name = "count", required = false) Integer count) {
        long loginUserID = loginUser == null ? 0L : loginUser.getUserId();
        int limit = normalizeLimit(count);
        String realFeedType = firstNotBlank(feedType, feedTypeCompat);

        try {
            if ("hot".equalsIgnoreCase(realFeedType) || "popular".equalsIgnoreCase(realFeedType)) {
                String realHotScore = firstNotBlank(hotScore, hotScoreCompat);
                double cursorScore = StrUtil.isBlank(realHotScore) ? Double.POSITIVE_INFINITY : Double.parseDouble(realHotScore);
                return videoService.hotFeedVideo(loginUserID, cursorScore, limit);
            }

            String realLatestTime = firstNotBlank(latestTime, latestTimeCompat);
            long lastTime = StrUtil.isNotBlank(realLatestTime)
                    ? Long.parseLong(realLatestTime) * 1000
                    : System.currentTimeMillis() + 1000;
            return videoService.feedVideo(loginUserID, lastTime, limit);
        } catch (NumberFormatException e) {
            return ResultVO.fail("invalid feed cursor");
        }
    }

    private int normalizeLimit(Integer count) {
        if (count == null || count <= 0) {
            return DEFAULT_LIMIT_COUNT;
        }
        return Math.min(count, MAX_LIMIT_COUNT);
    }

    private String firstNotBlank(String first, String second) {
        return StrUtil.isNotBlank(first) ? first : second;
    }
}