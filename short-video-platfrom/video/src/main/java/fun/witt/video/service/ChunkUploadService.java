package fun.witt.video.service;

import fun.witt.api.vo.ChunkUploadInitVO;
import fun.witt.api.vo.ResultVO;
import fun.witt.api.vo.UploadProgressVO;
import org.springframework.web.multipart.MultipartFile;

public interface ChunkUploadService {

    /**
     * 初始化分片上传任务
     *
     * @param userId   用户ID
     * @param fileName 原始文件名（含扩展名）
     * @param fileSize 文件总大小（字节）
     * @param title    视频标题
     * @return 包含 uploadId、chunkSize、chunkCount 的响应
     */
    ChunkUploadInitVO initUpload(long userId, String fileName, long fileSize, String title);

    /**
     * 上传单个分片
     *
     * @param uploadId    上传任务ID
     * @param chunkNumber 分片编号（从 1 开始）
     * @param file        分片数据
     * @return 上传结果
     */
    ResultVO uploadChunk(String uploadId, int chunkNumber, MultipartFile file);

    /**
     * 完成上传：合并分片、提取封面、写入视频记录
     *
     * @param userId   用户ID
     * @param uploadId 上传任务ID
     * @return 操作结果
     */
    ResultVO completeUpload(long userId, String uploadId);

    /**
     * 查询上传进度（用于断点续传时获取已上传的分片列表）
     *
     * @param uploadId 上传任务ID
     * @return 已上传分片编号列表及状态
     */
    UploadProgressVO getUploadProgress(String uploadId);
}
