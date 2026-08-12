package fun.witt.api.vo;

import lombok.Data;

@Data
public class VideoExt {
    private long id;
    private String title;
    private String coverUrl;
    private String playUrl;
    private long favoriteCount;
    private long collectCount;
    private long commentCount;
    private boolean isFavorite;
    private boolean isCollect;
    private UserExt author;
}
