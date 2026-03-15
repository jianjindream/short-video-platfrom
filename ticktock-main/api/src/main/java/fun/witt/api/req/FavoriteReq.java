package fun.witt.api.req;

import lombok.Data;

@Data
public class FavoriteReq {
    /**
     * 1-点赞，2-取消点赞
     */
    private String action_type;
    /**
     * 视频id
     */
    private String video_id;
}
