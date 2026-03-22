package fun.witt.video.controller;

import fun.witt.api.req.ChunkUploadInitReq;
import fun.witt.api.vo.ChunkUploadInitVO;
import fun.witt.api.vo.ResultVO;
import fun.witt.api.vo.UploadProgressVO;
import fun.witt.common.auth.LoginUser;
import fun.witt.video.service.ChunkUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/publish/upload")
public class ChunkUploadController {

    @Autowired
    private ChunkUploadService chunkUploadService;

    /**
     * 初始化分片上传，返回 uploadId、chunkSize、chunkCount
     */
    @PostMapping("/init")
    public ChunkUploadInitVO init(@AuthenticationPrincipal LoginUser loginUser,
                                  @RequestBody ChunkUploadInitReq req) {
        return chunkUploadService.initUpload(
                loginUser.getUserId(),
                req.getFileName(),
                req.getFileSize(),
                req.getTitle()
        );
    }

    /**
     * 上传单个分片
     *
     * @param uploadId    上传任务ID
     * @param chunkNumber 分片编号（从 1 开始）
     * @param file        分片二进制数据
     */
    @PostMapping("/chunk")
    public ResultVO uploadChunk(@RequestParam String uploadId,
                                @RequestParam int chunkNumber,
                                @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResultVO.fail("分片数据为空");
        }
        return chunkUploadService.uploadChunk(uploadId, chunkNumber, file);
    }

    /**
     * 完成上传：合并分片、提取封面、创建视频记录
     */
    @PostMapping("/complete")
    public ResultVO complete(@AuthenticationPrincipal LoginUser loginUser,
                             @RequestParam String uploadId) {
        return chunkUploadService.completeUpload(loginUser.getUserId(), uploadId);
    }

    /**
     * 查询上传进度（断点续传时使用）
     * 返回已成功上传的分片编号列表，客户端只需补传缺失分片即可
     */
    @GetMapping("/progress/{uploadId}")
    public UploadProgressVO progress(@PathVariable String uploadId) {
        return chunkUploadService.getUploadProgress(uploadId);
    }
}
