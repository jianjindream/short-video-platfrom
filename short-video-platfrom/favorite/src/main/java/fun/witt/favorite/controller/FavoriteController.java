package fun.witt.favorite.controller;

import fun.witt.api.req.FavoriteReq;
import fun.witt.api.vo.ResultVO;
import fun.witt.common.auth.LoginUser;
import fun.witt.favorite.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/favorite")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    @PostMapping("/action")
    public ResultVO action(@AuthenticationPrincipal LoginUser loginUser, FavoriteReq req) {
        long authorId = 0;
        if (req.getAuthor_id() != null) {
            authorId = Long.parseLong(req.getAuthor_id());
        }

        if (favoriteService.likeAction(req.getAction_type(),
                Long.parseLong(req.getVideo_id()),
                loginUser.getUserId(),
                authorId)) {
            return ResultVO.ok();
        }
        return ResultVO.fail("fail");
    }
}
