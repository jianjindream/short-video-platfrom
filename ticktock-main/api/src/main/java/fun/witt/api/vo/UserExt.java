package fun.witt.api.vo;

import lombok.Data;

@Data
public class UserExt {
    private long id;
    private String name;
    private long followCount;
    private long followerCount;
    private long totalFavorited;
    private long favoriteCount;
    private long collectCount;
    private boolean isFollow;
}
