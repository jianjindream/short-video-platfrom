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