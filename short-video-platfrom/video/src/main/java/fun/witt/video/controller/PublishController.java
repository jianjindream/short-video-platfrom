package fun.witt.video.controller;

import fun.witt.api.req.PublishReq;
import fun.witt.api.req.UserReq;
import fun.witt.api.vo.ResultVO;
import fun.witt.common.auth.LoginUser;
import fun.witt.video.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/publish")
public class PublishController {

    @Autowired
    private VideoService videoService;

    @PostMapping("/action")
    public ResultVO action(@AuthenticationPrincipal LoginUser loginUser, @ModelAttribute PublishReq req) {
        // todo 统一参数校验
        if (req.getData().isEmpty()) {
            return ResultVO.fail("video is empty");
        }
        return videoService.publish(loginUser.getUserId(), req.getTitle(), req.getData());
    }


    @GetMapping("/list")
    public ResultVO list(@AuthenticationPrincipal LoginUser loginUser, UserReq req) {
        // todo pagination
        return videoService.listVideo(Long.parseLong(req.getUser_id()), loginUser.getUserId());
    }
}
