package fun.witt.common.template;

import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class MinioTemplate {
    private final String bucket;
    private final MinioClient minioClient;

    public MinioTemplate(String bucket, String endpoint, String accessKeyID, String accessKeySecret) {
        this.bucket = bucket;
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKeyID, accessKeySecret)
                .build();
    }

    public void removeObject(String objectName) throws Exception {
        minioClient.removeObject(RemoveObjectArgs.builder()
                .object(objectName)
                .bucket(bucket)
                .build());
    }

    public String getObjectUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucket)
                    .object(objectName).expiry(12, TimeUnit.HOURS)
                    .method(Method.GET)
                    .build());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void uploadFile(byte[] bytes, String filePath, String contentType) throws IOException {
        try (InputStream input = new ByteArrayInputStream(bytes)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .contentType(contentType)
                            .stream(input, input.available(), -1)
                            .object(filePath)
                            .build()
            );
        } catch (Exception e) {
            log.error("minio上传文件错误：", e);
        }
    }

    public void uploadFile(MultipartFile file) throws IOException {
        String path = file.getName();
        uploadFile(file.getBytes(), path, file.getContentType());
    }

    /**
     * 上传分片到临时路径
     */
    public void uploadChunk(byte[] bytes, String chunkObjectName, String contentType) throws IOException {
        uploadFile(bytes, chunkObjectName, contentType);
    }

    /**
     * 合并多个分片对象为目标对象（基于 S3 ComposeObject）
     * 注意：除最后一个分片外，每个源对象大小不得低于 5MB
     */
    public void composeObject(String targetObjectName, List<String> sourceObjectNames) {
        try {
            List<ComposeSource> sources = sourceObjectNames.stream()
                    .map(name -> ComposeSource.builder()
                            .bucket(bucket)
                            .object(name)
                            .build())
                    .collect(Collectors.toList());

            minioClient.composeObject(
                    ComposeObjectArgs.builder()
                            .bucket(bucket)
                            .object(targetObjectName)
                            .sources(sources)
                            .build()
            );
        } catch (Exception e) {
            log.error("minio合并分片错误：", e);
            throw new RuntimeException("分片合并失败", e);
        }
    }

    /**
     * 批量删除对象
     */
    public void removeObjects(List<String> objectNames) {
        for (String name : objectNames) {
            try {
                removeObject(name);
            } catch (Exception e) {
                log.warn("删除临时分片 {} 失败：{}", name, e.getMessage());
            }
        }
    }

    /**
     * 获取对象输入流（用于读取已上传的文件）
     */
    public InputStream getObjectStream(String objectName) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .build()
        );
    }

    /**
     * 检查对象是否存在
     */
    public boolean objectExists(String objectName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getBucket() {
        return bucket;
    }
}
