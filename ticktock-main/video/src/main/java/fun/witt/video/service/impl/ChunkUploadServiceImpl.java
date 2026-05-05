package fun.witt.video.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import fun.witt.api.vo.ChunkUploadInitVO;
import fun.witt.api.vo.ResultVO;
import fun.witt.api.vo.UploadProgressVO;
import fun.witt.common.template.MinioTemplate;
import fun.witt.mapper.VideoMapper;
import fun.witt.model.Video;
import fun.witt.video.service.ChunkUploadService;
import fun.witt.video.support.ChunkUploadConstants;
import fun.witt.video.utils.FfmpegUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
public class ChunkUploadServiceImpl implements ChunkUploadService {

    private static final String NORM_DAY_PATTERN = "yyyy-MM-dd";

    @Value("${minio.chunk-size:5242880}")
    private long chunkSize;

    @Autowired
    private MinioTemplate minioTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private VideoMapper videoMapper;

    @Override
    public ChunkUploadInitVO initUpload(long userId, String fileName, long fileSize, String title) {
        ChunkUploadInitVO vo = new ChunkUploadInitVO();

        if (fileName == null || fileName.isEmpty() || fileSize <= 0) {
            vo.setStatusCode(1);
            vo.setStatusMsg("参数不合法");
            return vo;
        }

        String uploadId = IdUtil.simpleUUID();
        int chunkCount = (int) Math.ceil((double) fileSize / chunkSize);

        String ext = fileName.substring(fileName.lastIndexOf("."));
        String objectName = DateUtil.format(new Date(), NORM_DAY_PATTERN) + "/" + IdUtil.simpleUUID() + ext;
        long now = System.currentTimeMillis();

        Map<String, String> meta = new HashMap<String, String>();
        meta.put(ChunkUploadConstants.META_FIELD_USER_ID, String.valueOf(userId));
        meta.put(ChunkUploadConstants.META_FIELD_FILE_NAME, fileName);
        meta.put(ChunkUploadConstants.META_FIELD_FILE_SIZE, String.valueOf(fileSize));
        meta.put(ChunkUploadConstants.META_FIELD_OBJECT_NAME, objectName);
        meta.put(ChunkUploadConstants.META_FIELD_TITLE, title);
        meta.put(ChunkUploadConstants.META_FIELD_CHUNK_COUNT, String.valueOf(chunkCount));
        meta.put(ChunkUploadConstants.META_FIELD_CHUNK_SIZE, String.valueOf(chunkSize));
        meta.put(ChunkUploadConstants.META_FIELD_STATUS, ChunkUploadConstants.STATUS_UPLOADING);
        meta.put(ChunkUploadConstants.META_FIELD_CREATED_AT, String.valueOf(now));
        meta.put(ChunkUploadConstants.META_FIELD_LAST_CHUNK_AT, String.valueOf(now));

        String metaKey = ChunkUploadConstants.UPLOAD_META_PREFIX + uploadId;
        redisTemplate.opsForHash().putAll(metaKey, meta);
        redisTemplate.expire(metaKey, ChunkUploadConstants.UPLOAD_META_TTL_HOURS, TimeUnit.HOURS);

        String partsKey = ChunkUploadConstants.UPLOAD_PARTS_PREFIX + uploadId;
        redisTemplate.expire(partsKey, ChunkUploadConstants.UPLOAD_META_TTL_HOURS, TimeUnit.HOURS);

        vo.setStatusCode(0);
        vo.setStatusMsg("success");
        vo.setUploadId(uploadId);
        vo.setChunkSize(chunkSize);
        vo.setChunkCount(chunkCount);
        return vo;
    }

    @Override
    public ResultVO uploadChunk(String uploadId, int chunkNumber, MultipartFile file) {
        String metaKey = ChunkUploadConstants.UPLOAD_META_PREFIX + uploadId;
        Map<Object, Object> meta = redisTemplate.opsForHash().entries(metaKey);
        if (meta.isEmpty()) {
            return ResultVO.fail("上传任务不存在或已过期");
        }

        String status = stringValue(meta.get(ChunkUploadConstants.META_FIELD_STATUS));
        if (!ChunkUploadConstants.STATUS_UPLOADING.equals(status)) {
            return ResultVO.fail("上传任务状态异常：" + status);
        }

        int chunkCount = Integer.parseInt(stringValue(meta.get(ChunkUploadConstants.META_FIELD_CHUNK_COUNT)));
        if (chunkNumber < 1 || chunkNumber > chunkCount) {
            return ResultVO.fail("分片编号超出范围：1~" + chunkCount);
        }

        String chunkObjectName = ChunkUploadConstants.CHUNK_TMP_PREFIX + uploadId + "/" + chunkNumber;
        try {
            minioTemplate.uploadChunk(file.getBytes(), chunkObjectName, file.getContentType());
        } catch (IOException e) {
            log.error("分片上传失败，uploadId={}, chunkNumber={}", uploadId, chunkNumber, e);
            return ResultVO.fail("分片上传失败");
        }

        long now = System.currentTimeMillis();
        String partsKey = ChunkUploadConstants.UPLOAD_PARTS_PREFIX + uploadId;
        redisTemplate.opsForHash().put(partsKey, String.valueOf(chunkNumber), "1");
        redisTemplate.opsForHash().put(metaKey, ChunkUploadConstants.META_FIELD_LAST_CHUNK_AT, String.valueOf(now));
        redisTemplate.expire(metaKey, ChunkUploadConstants.UPLOAD_META_TTL_HOURS, TimeUnit.HOURS);
        redisTemplate.expire(partsKey, ChunkUploadConstants.UPLOAD_META_TTL_HOURS, TimeUnit.HOURS);

        return ResultVO.ok();
    }

    @Override
    public ResultVO completeUpload(long userId, String uploadId) {
        String metaKey = ChunkUploadConstants.UPLOAD_META_PREFIX + uploadId;
        Map<Object, Object> meta = redisTemplate.opsForHash().entries(metaKey);
        if (meta.isEmpty()) {
            return ResultVO.fail("上传任务不存在或已过期");
        }

        String status = stringValue(meta.get(ChunkUploadConstants.META_FIELD_STATUS));
        if (ChunkUploadConstants.STATUS_COMPLETED.equals(status)) {
            return ResultVO.ok();
        }
        if (!ChunkUploadConstants.STATUS_UPLOADING.equals(status)) {
            return ResultVO.fail("上传任务状态异常：" + status);
        }

        long metaUserId = Long.parseLong(stringValue(meta.get(ChunkUploadConstants.META_FIELD_USER_ID)));
        if (metaUserId != userId) {
            return ResultVO.fail("无权操作此上传任务");
        }

        int chunkCount = Integer.parseInt(stringValue(meta.get(ChunkUploadConstants.META_FIELD_CHUNK_COUNT)));
        String objectName = stringValue(meta.get(ChunkUploadConstants.META_FIELD_OBJECT_NAME));
        String title = stringValue(meta.get(ChunkUploadConstants.META_FIELD_TITLE));

        String partsKey = ChunkUploadConstants.UPLOAD_PARTS_PREFIX + uploadId;
        Set<Object> uploadedParts = redisTemplate.opsForHash().keys(partsKey);
        if (uploadedParts.size() < chunkCount) {
            List<Integer> missing = IntStream.rangeClosed(1, chunkCount)
                    .filter(i -> !uploadedParts.contains(String.valueOf(i)))
                    .boxed()
                    .collect(Collectors.toList());
            return ResultVO.fail("分片未全部上传，缺少：" + missing);
        }

        List<String> sourceObjectNames = IntStream.rangeClosed(1, chunkCount)
                .mapToObj(i -> ChunkUploadConstants.CHUNK_TMP_PREFIX + uploadId + "/" + i)
                .collect(Collectors.toList());

        try {
            minioTemplate.composeObject(objectName, sourceObjectNames);
        } catch (Exception e) {
            log.error("合并分片失败，uploadId={}", uploadId, e);
            return ResultVO.fail("合并分片失败");
        }

        String coverObjectName;
        try {
            String videoUrl = minioTemplate.getObjectUrl(objectName);
            byte[] coverBytes = FfmpegUtils.videoFrameFromUrl(videoUrl);
            coverObjectName = objectName.substring(0, objectName.lastIndexOf(".")) + ".jpg";
            if (coverBytes != null) {
                minioTemplate.uploadFile(coverBytes, coverObjectName, "image/jpeg");
            } else {
                coverObjectName = "";
            }
        } catch (Exception e) {
            log.warn("视频封面提取失败，uploadId={}，将使用空封面", uploadId, e);
            coverObjectName = "";
        }

        Video video = new Video();
        video.setFavoriteCount(0L);
        video.setCommentCount(0L);
        video.setAuthorId(userId);
        video.setTitle(title);
        video.setPlayUrl(objectName);
        video.setCoverUrl(coverObjectName);
        video.setPublishTime(new Date());
        if (videoMapper.insert(video) <= 0) {
            return ResultVO.fail("视频记录保存失败");
        }

        redisTemplate.opsForHash().put(metaKey, ChunkUploadConstants.META_FIELD_STATUS, ChunkUploadConstants.STATUS_COMPLETED);
        minioTemplate.removeObjects(sourceObjectNames);

        return ResultVO.ok();
    }

    @Override
    public UploadProgressVO getUploadProgress(String uploadId) {
        UploadProgressVO vo = new UploadProgressVO();

        String metaKey = ChunkUploadConstants.UPLOAD_META_PREFIX + uploadId;
        Map<Object, Object> meta = redisTemplate.opsForHash().entries(metaKey);
        if (meta.isEmpty()) {
            vo.setStatusCode(1);
            vo.setStatusMsg("上传任务不存在或已过期");
            return vo;
        }

        int chunkCount = Integer.parseInt(stringValue(meta.get(ChunkUploadConstants.META_FIELD_CHUNK_COUNT)));
        String status = stringValue(meta.get(ChunkUploadConstants.META_FIELD_STATUS));

        String partsKey = ChunkUploadConstants.UPLOAD_PARTS_PREFIX + uploadId;
        Set<Object> uploadedParts = redisTemplate.opsForHash().keys(partsKey);
        List<Integer> uploadedChunkList = uploadedParts.stream()
                .map(o -> Integer.parseInt(o.toString()))
                .sorted()
                .collect(Collectors.toList());

        vo.setStatusCode(0);
        vo.setStatusMsg("success");
        vo.setUploadId(uploadId);
        vo.setChunkCount(chunkCount);
        vo.setUploadedChunks(uploadedChunkList);
        vo.setStatus(status);
        return vo;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }
}
