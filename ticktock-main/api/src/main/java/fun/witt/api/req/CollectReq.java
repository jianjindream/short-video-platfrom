package fun.witt.api.req;

import lombok.Data;

@Data
public class CollectReq {
    private String action_type;
    private String video_id;
}
