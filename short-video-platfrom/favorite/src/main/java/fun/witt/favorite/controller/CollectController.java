package fun.witt.favorite.controller;

import fun.witt.api.req.CollectReq;
import fun.witt.api.vo.ResultVO;
import fun.witt.common.auth.LoginUser;
import fun.witt.favorite.service.CollectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/collect")
public class CollectController {

    @Autowired
    private CollectService collectService;

    @PostMapping("/action")
    public ResultVO action(@AuthenticationPrincipal LoginUser loginUser, CollectReq req) {
        if (collectService.collectAction(req.getAction_type(),
                Long.parseLong(req.getVideo_id()),
                loginUser.getUserId())) {
            return ResultVO.ok();
        }
        return ResultVO.fail("fail");
    }
}
