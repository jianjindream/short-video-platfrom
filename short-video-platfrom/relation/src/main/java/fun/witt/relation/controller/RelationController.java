package fun.witt.relation.controller;


import fun.witt.api.req.RelationReq;
import fun.witt.api.req.UserReq;
import fun.witt.api.vo.ResultVO;
import fun.witt.common.auth.LoginUser;
import fun.witt.relation.service.RelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/relation")
public class RelationController {

    @Autowired
    private RelationService relationService;

    @PostMapping("/action")
    public ResultVO action(@AuthenticationPrincipal LoginUser loginUser, RelationReq req) {
        return relationService.followAction(req.getAction_type(),
                Long.parseLong(req.getTo_user_id()),
                loginUser.getUserId());
    }

    @GetMapping("/follow/list")
    public ResultVO followList(@AuthenticationPrincipal LoginUser loginUser, UserReq req) {
        return relationService.followList(Long.parseLong(req.getUser_id()),
                loginUser.getUserId());
    }

    @GetMapping("/follower/list")
    public ResultVO followerList(@AuthenticationPrincipal LoginUser loginUser, UserReq req) {
        return relationService.followerList(Long.parseLong(req.getUser_id()),
                loginUser.getUserId());
    }
}
