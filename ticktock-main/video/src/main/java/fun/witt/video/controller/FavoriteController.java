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
@RequestMapping("/favorite")
public class FavoriteController {

    @Autowired
    private VideoService videoService;

    @GetMapping("/list")
    public ResultVO list(@AuthenticationPrincipal LoginUser loginUser, UserReq req) {
        // todo pagination
        return videoService.listFavoriteVideo(Long.parseLong(req.getUser_id()), loginUser.getUserId());
    }

}
