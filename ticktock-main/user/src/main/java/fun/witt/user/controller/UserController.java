package fun.witt.user.controller;

import fun.witt.api.req.AccountReq;
import fun.witt.api.req.RefreshTokenReq;
import fun.witt.api.req.UserReq;
import fun.witt.api.vo.ResultVO;
import fun.witt.api.vo.UserExt;
import fun.witt.api.vo.UserVO;
import fun.witt.common.auth.LoginUser;
import fun.witt.user.service.AuthService;
import fun.witt.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResultVO register(AccountReq req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public ResultVO login(AccountReq req) {
        return authService.login(req);
    }

    @PostMapping("/token/refresh")
    public ResultVO refresh(@RequestBody RefreshTokenReq req) {
        return authService.refresh(req.getRefreshToken());
    }

    @PostMapping("/logout")
    public ResultVO logout(@RequestBody RefreshTokenReq req) {
        return authService.logout(req.getRefreshToken());
    }

    @GetMapping
    public ResultVO info(@AuthenticationPrincipal LoginUser loginUser, UserReq req) {
        long loginUserID = loginUser == null ? 0L : loginUser.getUserId();
        UserExt userExt = userService.queryUserByID(Long.parseLong(req.getUser_id()), loginUserID);
        if (userExt != null) {
            UserVO userVO = new UserVO();
            userVO.setUser(userExt);
            return userVO;
        }
        return ResultVO.fail("not found");
    }
}
