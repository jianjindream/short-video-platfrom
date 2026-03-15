# ticktock

## development

### 调试

依赖环境

jdk 17
maven 3.x

```bash
# 进入项目根目录
cd ticktock

# 编译服务打包 jar 文件
# 线上一般会有一套完整的构建服务去构建 docker 服务镜像，包括版本控制，这里只是为了测试，所有没有做的很复杂，直接在项目里编译好，docker 只负责打包 jar 到镜像文件
mvn clean install -Dmaven.test.skip=true

# 启动服务（包含依赖组件 nacos、minio、db 和所有服务模块）
# 服务模块会自动打包 docker 镜像，并启动容器
docker compose up -d

# 查看日志
docker compose logs -f

# 如果有服务链接注册不到 nacos 可以访问 nacos 地址
http://x.x.x.x:8848/nacos/index.html
# 确定 nacos 启动完毕后，重启所有服务模块
docker compose start

# minio 地址，用户名密码默认都为 minioadmin
http://x.x.x.x:9000

# API 测试地址
http://x.x.x.x:8008

```

## 技术栈

## library

spring framework (core web boot)

spring cloud (nacos openfeign gateway config)

mybatis / generator

pagehelper

lombok

jwt

## deploy

maven

docker

makefile

## cvs

git

## TODO

### 全局配置

- [x] 项目结构搭建 && init modules
- [x] nacos 接入
- [x] open feign 接入
- [x] spring cloud gateway 接入
- [x] model 构建
- [x] docker compose 基础组件（MySQL & nacos）
- [x] protobuf generator
- [ ] 统一参数校验
- [ ] 全局异常处理
- [ ] 服务限流
- [ ] 熔断 & 降级
- [ ] 链路追踪
- [ ] nacos 配置中心接入，动态配置改造
- [ ] 服务容器化
- [ ] 集成发号器
- [ ] 抽离 auth model
- [ ] api request 打包
- [ ] 处理互相关注
- [ ] 计数同步（关注&被关注 count，评论 count）

### 服务接口

api

- [x] user feign client

model

- [x] mybatis generator POJO

comment

- [x] init
- [x] 评论 & 删除评论
- [x] 评论列表

user service

- [x] 用户注册
- [x] 用户登录
- [x] 获取用户信息
- [x] 统一处理 user token 验证

video service

- [x] 视频发布
- [x] 视频列表
- [x] feed

relation service

- [x] 是否关注服务 api
- [x] 关注 & 取消关注
- [x] 关注列表
- [x] 粉丝列表

favorite service

- [x] 点赞 & 取消点赞
- [x] 点赞列表
