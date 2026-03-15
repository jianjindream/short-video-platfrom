package fun.witt.user.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import fun.witt.api.req.AccountReq;
import fun.witt.api.vo.ResultVO;
import fun.witt.api.vo.UserTokenVO;
import fun.witt.common.service.JwtService;
import fun.witt.common.service.RefreshTokenStore;
import fun.witt.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RefreshTokenStore refreshTokenStore;

    public ResultVO register(AccountReq req) {
        User exist = userService.queryNameIsExist(req.getUsername());
        if (exist != null) {
            return ResultVO.fail("注册失败");
        }
        User user = userService.createUser(req.getUsername(), req.getPassword());
        return buildTokenVO(user);
    }

    public ResultVO login(AccountReq req) {
        User user = userService.queryNameIsExist(req.getUsername());
        if (user == null) {
            return ResultVO.fail("请检查用户名");
        }

        boolean passwordPass = StrUtil.isNotBlank(req.getPassword())
                && user.getPassword().equals(DigestUtil.sha1Hex(req.getPassword()));
        boolean codePass = StrUtil.isNotBlank(req.getCode()) && verifyCode(req.getUsername(), req.getCode());

        if (!passwordPass && !codePass) {
            return ResultVO.fail("登录失败");
        }
        return buildTokenVO(user);
    }

    public ResultVO refresh(String refreshToken) {
        try {
            var claims = jwtService.parseRefreshToken(refreshToken);
            String jti = claims.getId();
            Long tokenUserId = refreshTokenStore.getUserId(jti);
            if (tokenUserId == null) {
                return ResultVO.fail("refresh token已失效");
            }

            Long claimUserId = Long.parseLong(String.valueOf(claims.get("uid")));
            if (!tokenUserId.equals(claimUserId)) {
                refreshTokenStore.delete(jti);
                return ResultVO.fail("refresh token无效");
            }

            refreshTokenStore.delete(jti);

            User user = new User();
            user.setId(claimUserId);
            user.setUserName(claims.getSubject());
            return buildTokenVO(user);
        } catch (Exception e) {
            return ResultVO.fail("refresh token无效");
        }
    }

    public ResultVO logout(String refreshToken) {
        try {
            var claims = jwtService.parseRefreshToken(refreshToken);
            refreshTokenStore.delete(claims.getId());
        } catch (Exception ignored) {
        }
        return ResultVO.ok();
    }

    private UserTokenVO buildTokenVO(User user) {
        String accessToken = jwtService.createAccessToken(user);
        String refreshJti = UUID.randomUUID().toString().replace("-", "");
        String refreshToken = jwtService.createRefreshToken(user, refreshJti);

        refreshTokenStore.save(refreshJti, user.getId(), jwtService.getRefreshTokenValidityMs());

        UserTokenVO tokenVO = new UserTokenVO();
        tokenVO.setStatusCode(0);
        tokenVO.setStatusMsg("success");
        tokenVO.setUserID(user.getId());
        tokenVO.setAccessToken(accessToken);
        tokenVO.setRefreshToken(refreshToken);
        tokenVO.setAccessTokenExpireIn(jwtService.getAccessTokenValidityMs() / 1000);
        tokenVO.setRefreshTokenExpireIn(jwtService.getRefreshTokenValidityMs() / 1000);
        return tokenVO;
    }

    private boolean verifyCode(String username, String code) {
        return false;
    }
}
