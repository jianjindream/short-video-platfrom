package fun.witt.api.req;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChunkUploadInitReq {
    /**
     * 原始文件名（含扩展名，如 video.mp4）
     */
    private String fileName;
    /**
     * 文件总大小（字节）
     */
    private Long fileSize;
    /**
     * 视频标题
     */
    private String title;
}
