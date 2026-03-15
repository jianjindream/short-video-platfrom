package fun.witt.comment.controller;

import fun.witt.api.req.CommentReq;
import fun.witt.api.req.VideoReq;
import fun.witt.api.vo.ResultVO;
import fun.witt.comment.service.CommentService;
import fun.witt.common.template.JWTTemplate;
import fun.witt.constant.Constant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/comment")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private JWTTemplate jwtTemplate;

    @PostMapping("/action")
    public ResultVO action(CommentReq req) {
        Number loginUserID = jwtTemplate.getUserIDFromToken(req.getToken());
        // todo text UGC check
        switch (req.getAction_type()) {
            case Constant.COMMENT_PUBLISH -> {
                return commentService.publish(Long.parseLong(req.getVideo_id()),
                        loginUserID.longValue(),
                        req.getComment_text());
            }
            case Constant.COMMENT_REMOVE -> {
                return commentService.delete(Long.parseLong(req.getComment_id()),
                        loginUserID.longValue());
            }
        }
        return ResultVO.fail("fail");
    }

    @GetMapping("/list")
    public ResultVO list(VideoReq req) {
        Number loginUserID = jwtTemplate.getUserIDFromToken(req.getToken());
        return commentService.list(Long.parseLong(req.getVideo_id()), loginUserID.longValue());
    }
}
