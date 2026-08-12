# Short Video Platform

基于 Spring Boot 与 Spring Cloud Alibaba 构建的短视频微服务平台。项目通过 API 网关统一对外提供服务，覆盖用户、视频、评论、点赞、收藏和关注等核心场景，并提供视频分片上传、断点续传、Feed 流缓存、异步计数聚合等能力。

> 当前代码中的服务、数据库和容器名称仍保留 ticktock 作为内部标识；仓库目录名称已调整为 short-video-platfrom。

## 功能概览

- 用户：注册、登录、JWT 鉴权、刷新令牌及用户信息查询。
- 视频：发布、查询、分片上传、断点续传，以及 MinIO 对象存储与 FFmpeg 视频处理。
- Feed 流：热门 Feed 缓存及定时刷新，降低热门内容查询压力。
- 社交互动：评论、点赞、收藏、关注与粉丝关系维护。
- 计数一致性：通过 Redis 缓存、Kafka 事件和可靠消息表（Outbox）进行异步计数聚合与补偿。
- 网关安全：Gateway 统一鉴权；登录、注册、发布、评论和互动接口配置 Redis 滑动窗口限流。

## 架构

~~~mermaid
flowchart TB
    Client["客户端"] --> Gateway["Gateway :8008"]
    Gateway --> User["用户服务"]
    Gateway --> Video["视频服务"]
    Gateway --> Comment["评论服务"]
    Gateway --> Favorite["点赞/收藏服务"]
    Gateway --> Relation["关注服务"]

    User & Video & Comment & Favorite & Relation --> Nacos["Nacos 服务注册与发现"]
    User & Video & Comment & Favorite & Relation --> DB["MariaDB"]
    User & Video & Comment & Favorite & Relation --> Redis["Redis"]
    Video --> MinIO["MinIO 对象存储"]
    Favorite & Relation --> Kafka["Kafka 事件流"]
    Relation --> Canal["Canal"]
    Canal --> Kafka
~~~

## 技术栈

| 类别 | 组件 |
| --- | --- |
| 基础框架 | Spring Boot 2.7.7、Spring Cloud 2021.0.5、Spring Cloud Alibaba 2021.0.4.0 |
| 服务治理 | Nacos、Spring Cloud Gateway、OpenFeign |
| 数据与缓存 | MariaDB、MyBatis、PageHelper、Redis |
| 消息与同步 | Kafka、Canal、可靠消息表（Outbox） |
| 文件与视频 | MinIO、FFmpeg |
| 安全 | Spring Security、JWT、网关限流 |
| 部署 | Maven、Docker Compose |

## 模块说明

| 模块 | 职责 |
| --- | --- |
| api | 共享请求/响应对象、Feign 接口及 Proto 定义 |
| common | 公共配置、安全、JWT、缓存与基础工具 |
| model | 数据模型、Mapper 与数据库映射文件 |
| gateway | API 网关、统一鉴权、路由和限流 |
| user | 用户注册、认证与用户资料 |
| video | 视频发布、上传、Feed 流与对象存储 |
| comment | 视频评论与评论计数 |
| favorite | 点赞、收藏、事件投递与计数同步 |
| relation | 关注关系、粉丝关系与 Outbox 消费 |
| deploy | 本地基础设施启动配置与数据库初始化脚本 |

## 快速开始

### 1. 环境准备

- JDK 8 或更高版本（容器镜像使用 JDK 17）。
- Maven 3.6 或更高版本。
- Docker 与 Docker Compose。

### 2. 构建服务

在项目目录执行：

~~~bash
cd short-video-platfrom
mvn clean package -DskipTests
~~~

### 3. 启动全部服务

~~~bash
docker compose up -d --build
~~~

查看服务状态与日志：

~~~bash
docker compose ps
docker compose logs -f gateway
~~~

服务首次启动时，Nacos、MariaDB、Redis、Kafka、MinIO、Canal 及各业务服务会一并启动。若某个服务因基础组件尚未就绪而注册失败，可在基础组件健康后重启该服务：

~~~bash
docker compose restart gateway user video comment favorite relation
~~~

### 4. 常用本地地址

| 服务 | 地址 |
| --- | --- |
| API 网关 | http://localhost:8008 |
| Nacos 控制台 | http://localhost:8848/nacos |
| MinIO 控制台 | http://localhost:9001 |
| MinIO API | http://localhost:9000 |

网关接口以 /douyin 为前缀，例如：

~~~text
POST /douyin/user/register
POST /douyin/user/login
GET  /douyin/feed
~~~

具体请求参数以 api 模块中的请求对象及各服务 Controller 为准。

## 本地开发说明

### 仅启动基础组件

deploy/docker-compose.yml 用于仅启动 Nacos、MinIO 与数据库等基础依赖。进入 deploy 目录后执行：

~~~bash
docker compose up -d
~~~

随后可使用 IDE 分别运行各模块中的 *Application 启动类进行调试。

### 配置与安全

- 默认 Docker Compose 配置内含便于本地启动的数据库、MinIO 与 Nacos 凭据；它们不应直接用于生产环境。
- 部署前请通过环境变量、配置中心或密钥管理服务替换密码、JWT 签名及外部服务地址。
- 对外部署时请按实际网络环境调整 Kafka 的 KAFKA_ADVERTISED_LISTENERS、Nacos 地址和 Gateway 路由。

## 目录结构

~~~text
short-video-platfrom/
├── api/                 # 跨服务 API 契约
├── common/              # 公共能力
├── model/               # 数据模型与 Mapper
├── gateway/             # 网关服务
├── user/                # 用户服务
├── video/               # 视频服务
├── comment/             # 评论服务
├── favorite/            # 点赞/收藏服务
├── relation/            # 关注服务
├── deploy/              # 基础设施与 SQL 脚本
├── docker-compose.yml   # 全量本地启动编排
└── pom.xml              # Maven 聚合工程
~~~

## 许可证

本项目暂未声明开源许可证。若计划公开分发或复用，请在发布前补充合适的许可证文件。
