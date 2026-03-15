package fun.witt.api.req;

import lombok.Data;
import org.springframework.web.bind.annotation.RequestParam;

@Data
public class AccountReq {
    private String username;
    private String password;
}
