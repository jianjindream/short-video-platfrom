package fun.witt.api.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ChunkUploadInitVO extends ResultVO {
    /**
     * 上传任务唯一标识，后续分片上传和完成合并时使用
     */
    private String uploadId;
    /**
     * 每个分片的大小（字节），最后一个分片可能小于此值
     */
    private long chunkSize;
    /**
     * 总分片数量
     */
    private int chunkCount;
}
