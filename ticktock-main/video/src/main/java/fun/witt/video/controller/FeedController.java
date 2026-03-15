package fun.witt.video.controller;

import cn.hutool.core.util.StrUtil;
import fun.witt.api.vo.ResultVO;
import fun.witt.common.template.JWTTemplate;
import fun.witt.video.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class FeedController {
    private static final int DEFAULT_LIMIT_COUNT = 30;
    @Autowired
    private VideoService videoService;

    @Autowired
    private JWTTemplate jwtTemplate;

    @GetMapping("/feed")
    public ResultVO feed(String token, String latestTime) {
        Number loginUserID = 0;
        if (StrUtil.isNotBlank(token)) {
            loginUserID = jwtTemplate.getUserIDFromToken(token);
        }
        long lastTime;
        if (StrUtil.isNotBlank(latestTime)) {
            lastTime = Long.parseLong(latestTime) * 1000;
        } else {
            lastTime = System.currentTimeMillis();
        }
        return videoService.feedVideo(loginUserID.longValue(), lastTime, DEFAULT_LIMIT_COUNT);
    }
}
