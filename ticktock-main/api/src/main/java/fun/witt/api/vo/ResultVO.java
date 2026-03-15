package fun.witt.api.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ResultVO {
    private static final int STATUS_CODE_SUCCESS = 0;
    private static final int STATUS_CODE_ERROR = 1;

    private int statusCode;
    private String statusMsg;

    public ResultVO(int statusCode, String statusMsg) {
        this.statusCode = statusCode;
        this.statusMsg = statusMsg;
    }

    public static ResultVO fail(String msg) {
        return new ResultVO(STATUS_CODE_ERROR, msg);
    }

    public static ResultVO ok() {
        return new ResultVO(STATUS_CODE_SUCCESS, "success");
    }
}
