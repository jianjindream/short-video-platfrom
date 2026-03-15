package fun.witt.relation.controller;


import fun.witt.api.req.RelationReq;
import fun.witt.api.req.UserReq;
import fun.witt.api.vo.ResultVO;
import fun.witt.common.template.JWTTemplate;
import fun.witt.relation.service.RelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/relation")
public class RelationController {

    @Autowired
    private RelationService relationService;

    @Autowired
    private JWTTemplate jwtTemplate;

    @PostMapping("/action")
    public ResultVO action(RelationReq req) {
        Number loginUserID = jwtTemplate.getUserIDFromToken(req.getToken());
        return relationService.followAction(req.getAction_type(),
                Long.parseLong(req.getTo_user_id()),
                loginUserID.longValue());
    }

    @GetMapping("/follow/list")
    public ResultVO followList(UserReq req) {
        Number loginUserID = jwtTemplate.getUserIDFromToken(req.getToken());
        return relationService.followList(Long.parseLong(req.getUser_id()),
                loginUserID.longValue());
    }

    @GetMapping("/follower/list")
    public ResultVO followerList(UserReq req) {
        Number loginUserID = jwtTemplate.getUserIDFromToken(req.getToken());
        return relationService.followerList(Long.parseLong(req.getUser_id()),
                loginUserID.longValue());
    }
}
