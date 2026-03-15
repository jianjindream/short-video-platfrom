package fun.witt.video.controller;

import fun.witt.api.req.PublishReq;
import fun.witt.api.req.UserReq;
import fun.witt.api.vo.ResultVO;
import fun.witt.common.template.JWTTemplate;
import fun.witt.video.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/publish")
public class PublishController {

    @Autowired
    private VideoService videoService;

    @Autowired
    private JWTTemplate jwtTemplate;

    @PostMapping("/action")
    public ResultVO action(@ModelAttribute PublishReq req) {
        // todo 统一参数校验
        if (req.getData().isEmpty()) {
            return ResultVO.fail("video is empty");
        }
        Number loginUserID = jwtTemplate.getUserIDFromToken(req.getToken());
        return videoService.publish(loginUserID.longValue(), req.getTitle(), req.getData());
    }


    @GetMapping("/list")
    public ResultVO list(UserReq req) {
        // todo pagination
        Number loginUserID = jwtTemplate.getUserIDFromToken(req.getToken());
        return videoService.listVideo(Long.parseLong(req.getUser_id()), loginUserID.longValue());
    }
}
