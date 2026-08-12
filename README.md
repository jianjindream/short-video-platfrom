# Short Video Platform

短视频微服务平台，基于 Spring Boot、Spring Cloud Alibaba、Gateway、Nacos、Redis、Kafka、MinIO 与 FFmpeg 构建。

项目包含用户、视频发布与 Feed 流、评论、点赞、收藏和关注等服务，并支持 JWT 鉴权、网关限流、视频分片上传、断点续传，以及基于 Outbox 与 Kafka 的异步计数同步。

## 项目文档

完整的架构说明、技术栈、模块职责和本地启动步骤见：[项目 README](short-video-platfrom/README.md)。

## 快速启动

```bash
cd short-video-platfrom
mvn clean package -DskipTests
docker compose up -d --build
```

启动完成后，通过 `http://localhost:8008` 访问 API 网关。
