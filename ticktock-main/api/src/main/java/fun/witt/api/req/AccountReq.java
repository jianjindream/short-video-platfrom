package fun.witt.api.req;

import lombok.Data;

@Data
public class AccountReq {
    private String username;
    private String password;
    private String code;
}
