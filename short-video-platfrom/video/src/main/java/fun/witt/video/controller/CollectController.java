package fun.witt.video.controller;

import fun.witt.api.req.UserReq;
import fun.witt.api.vo.ResultVO;
import fun.witt.common.auth.LoginUser;
import fun.witt.video.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/collect")
public class CollectController {

    @Autowired
    private VideoService videoService;

    @GetMapping("/list")
    public ResultVO list(@AuthenticationPrincipal LoginUser loginUser, UserReq req) {
        return videoService.listCollectVideo(Long.parseLong(req.getUser_id()), loginUser.getUserId());
    }
}
