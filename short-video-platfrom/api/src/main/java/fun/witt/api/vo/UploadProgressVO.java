package fun.witt.api.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class UploadProgressVO extends ResultVO {
    /**
     * 上传任务标识
     */
    private String uploadId;
    /**
     * 总分片数
     */
    private int chunkCount;
    /**
     * 已上传成功的分片编号列表（从 1 开始）
     */
    private List<Integer> uploadedChunks;
    /**
     * 上传状态：UPLOADING / COMPLETED / ABORTED
     */
    private String status;
}
