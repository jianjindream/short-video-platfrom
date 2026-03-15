package fun.witt.api.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserTokenVO extends ResultVO {
    private long userID;
    private String accessToken;
    private String refreshToken;
    private long accessTokenExpireIn;
    private long refreshTokenExpireIn;
}