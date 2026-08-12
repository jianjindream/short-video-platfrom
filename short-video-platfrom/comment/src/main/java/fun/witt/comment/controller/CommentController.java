package fun.witt.comment.controller;

import fun.witt.api.req.CommentReq;
import fun.witt.api.req.VideoReq;
import fun.witt.api.vo.ResultVO;
import fun.witt.comment.service.CommentService;
import fun.witt.common.auth.LoginUser;
import fun.witt.constant.Constant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/comment")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @PostMapping("/action")
    public ResultVO action(@AuthenticationPrincipal LoginUser loginUser, CommentReq req) {
        long loginUserID = loginUser.getUserId();
        // todo text UGC check
        switch (req.getAction_type()) {
            case Constant.COMMENT_PUBLISH:
                return commentService.publish(Long.parseLong(req.getVideo_id()),
                        loginUserID,
                        req.getComment_text());
            case Constant.COMMENT_REMOVE:
                return commentService.delete(Long.parseLong(req.getComment_id()),
                        loginUserID);
            default:
                return ResultVO.fail("fail");
        }
    }

    @GetMapping("/list")
    public ResultVO list(@AuthenticationPrincipal LoginUser loginUser, VideoReq req) {
        return commentService.list(Long.parseLong(req.getVideo_id()), loginUser.getUserId());
    }
}
