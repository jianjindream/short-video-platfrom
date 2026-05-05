package fun.witt.video.task;

import fun.witt.common.template.MinioTemplate;
import fun.witt.video.support.ChunkUploadConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Cursor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class ChunkUploadCleanupTask {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private MinioTemplate minioTemplate;

    @Scheduled(fixedDelay = ChunkUploadConstants.CLEANUP_FIXED_DELAY_MS)
    public void cleanupExpiredUploads() {
        long now = System.currentTimeMillis();
        long timeoutMillis = ChunkUploadConstants.UPLOAD_TIMEOUT_HOURS * 60 * 60 * 1000L;

        for (String metaKey : scanKeys(ChunkUploadConstants.UPLOAD_META_PREFIX + "*")) {
            Map<Object, Object> meta = redisTemplate.opsForHash().entries(metaKey);
            if (meta.isEmpty()) {
                continue;
            }

            String status = stringValue(meta.get(ChunkUploadConstants.META_FIELD_STATUS));
            if (ChunkUploadConstants.STATUS_COMPLETED.equals(status)) {
                continue;
            }

            long lastChunkAt = parseLong(meta.get(ChunkUploadConstants.META_FIELD_LAST_CHUNK_AT));
            if (lastChunkAt <= 0) {
                lastChunkAt = parseLong(meta.get(ChunkUploadConstants.META_FIELD_CREATED_AT));
            }
            if (lastChunkAt <= 0 || now - lastChunkAt < timeoutMillis) {
                continue;
            }

            String uploadId = metaKey.substring(ChunkUploadConstants.UPLOAD_META_PREFIX.length());
            cleanupUpload(uploadId);
        }
    }

    private void cleanupUpload(String uploadId) {
        String metaKey = ChunkUploadConstants.UPLOAD_META_PREFIX + uploadId;
        String partsKey = ChunkUploadConstants.UPLOAD_PARTS_PREFIX + uploadId;
        String prefix = ChunkUploadConstants.CHUNK_TMP_PREFIX + uploadId + "/";

        if (!minioTemplate.removeObjectsByPrefix(prefix)) {
            log.warn("upload {} 临时分片清理失败，稍后重试", uploadId);
            return;
        }

        redisTemplate.delete(metaKey);
        redisTemplate.delete(partsKey);
        log.info("upload {} 已超时清理完成", uploadId);
    }

    private Set<String> scanKeys(String pattern) {
        Set<String> keys = new HashSet<String>();
        RedisConnection connection = null;
        Cursor<byte[]> cursor = null;
        try {
            connection = redisTemplate.getConnectionFactory().getConnection();
            cursor = connection.scan(ScanOptions.scanOptions().match(pattern).count(200).build());
            while (cursor.hasNext()) {
                keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            log.error("扫描上传任务键失败 pattern={}", pattern, e);
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Exception e) {
                    log.warn("关闭 Redis 游标失败", e);
                }
            }
            if (connection != null) {
                connection.close();
            }
        }
        return keys;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private long parseLong(Object value) {
        if (value == null) {
            return -1L;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return -1L;
        }
    }
}
