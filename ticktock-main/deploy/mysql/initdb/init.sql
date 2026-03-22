SET NAMES utf8mb4;
SET
    FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for comments
-- ----------------------------
CREATE DATABASE camps_tiktok;

USE camps_tiktok;

DROP TABLE IF EXISTS `t_comment`;
CREATE TABLE `t_comment`
(
    `id`           bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '评论id，自增主键',
    `user_id`      bigint(20)   NOT NULL COMMENT '评论发布用户id',
    `video_id`     bigint(20)   NOT NULL COMMENT '评论视频id',
    `comment_text` varchar(255) NOT NULL COMMENT '评论内容',
    `create_time`  datetime     NOT NULL COMMENT '评论发布时间',
    PRIMARY KEY (`id`),
    KEY `videoIdIdx` (`video_id`) USING BTREE COMMENT '评论列表使用视频id作为索引-方便查看视频下的评论列表'
) ENGINE = InnoDB
  AUTO_INCREMENT = 1206
  DEFAULT CHARSET = utf8 COMMENT ='评论表';

-- ----------------------------
-- 主表: 关注关系 (following), 分片键 from_user_id
-- ----------------------------
DROP TABLE IF EXISTS `t_relation`;
CREATE TABLE `t_relation`
(
    `id`          bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `follow_id`   bigint(20) NOT NULL COMMENT '被关注的用户id (to_user_id)',
    `follower_id` bigint(20) NOT NULL COMMENT '发起关注的用户id (from_user_id)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_from_to` (`follower_id`, `follow_id`) USING BTREE,
    KEY `idx_follow_id` (`follow_id`) USING BTREE,
    KEY `idx_follower_id` (`follower_id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1096
  DEFAULT CHARSET = utf8 COMMENT ='关注主表(following)';

-- ----------------------------
-- 从表: 粉丝关系 (follower), 分片键 to_user_id
-- ----------------------------
DROP TABLE IF EXISTS `t_follower`;
CREATE TABLE `t_follower`
(
    `id`            bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `from_user_id`  bigint(20)  NOT NULL COMMENT '发起关注的用户id',
    `to_user_id`    bigint(20)  NOT NULL COMMENT '被关注的用户id',
    `created_at`    datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_from_to` (`from_user_id`, `to_user_id`) USING BTREE,
    KEY `idx_to_user_id` (`to_user_id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8 COMMENT ='粉丝从表(follower)';

-- ----------------------------
-- Outbox 事件表
-- ----------------------------
DROP TABLE IF EXISTS `t_outbox`;
CREATE TABLE `t_outbox`
(
    `id`             bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '事件id',
    `aggregate_type` varchar(32)  NOT NULL COMMENT '事件类型: FOLLOW / UNFOLLOW',
    `aggregate_id`   varchar(64)  NOT NULL COMMENT '聚合ID',
    `payload`        text         NOT NULL COMMENT '事件负载JSON',
    `created_at`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `processed`      tinyint(1)   NOT NULL DEFAULT 0 COMMENT '是否已处理: 0-未处理 1-已处理',
    PRIMARY KEY (`id`),
    KEY `idx_processed_created` (`processed`, `created_at`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8 COMMENT ='Outbox事件表';

-- ----------------------------
-- Table structure for likes
-- ----------------------------
DROP TABLE IF EXISTS `t_favorite`;
CREATE TABLE `t_favorite`
(
    `id`       bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `user_id`  bigint(20) NOT NULL COMMENT '点赞用户id',
    `video_id` bigint(20) NOT NULL COMMENT '被点赞的视频id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `userIdtoVideoIdIdx` (`user_id`, `video_id`) USING BTREE,
    KEY `userIdIdx` (`user_id`) USING BTREE,
    KEY `videoIdx` (`video_id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1229
  DEFAULT CHARSET = utf8 COMMENT ='点赞表';

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user`
(
    `id`               bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '用户id，自增主键',
    `user_name`        varchar(255) NOT NULL COMMENT '用户名',
    `password`         varchar(255) NOT NULL COMMENT '用户密码',
    `follow_count`     bigint(20)   NOT NULL DEFAULT 0 COMMENT '该用户关注其他用户个数',
    `follower_count`   bigint(20)   NOT NULL DEFAULT 0 COMMENT '该用户粉丝个数',
    `total_favorited`  bigint(20)   NOT NULL DEFAULT 0 COMMENT '该用户被喜欢的视频数量',
    `favorite_count`   bigint(20)   NOT NULL DEFAULT 0 COMMENT '该用户喜欢的视频数量',
    `signature`        varchar(1024) COMMENT '签名',
    `avatar`           varchar(1024) COMMENT '用户头像',
    `background_image` varchar(1024) COMMENT '主页背景',
    PRIMARY KEY (`id`),
    KEY `name_password_idx` (`user_name`, `password`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 20044
  DEFAULT CHARSET = utf8 COMMENT ='用户表';

-- ----------------------------
-- Table structure for videos
-- ----------------------------
DROP TABLE IF EXISTS `t_video`;
CREATE TABLE `t_video`
(
    `id`             bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '自增主键，视频唯一id',
    `author_id`      bigint(20)   NOT NULL COMMENT '视频作者id',
    `play_url`       varchar(255) NOT NULL COMMENT '播放url',
    `cover_url`      varchar(255) NOT NULL COMMENT '封面url',
    `favorite_count` bigint(20)   NOT NULL DEFAULT 0 COMMENT '视频的点赞数量',
    `comment_count`  bigint(20)   NOT NULL DEFAULT 0 COMMENT '视频的评论数量',
    `publish_time`   datetime     NOT NULL COMMENT '发布时间戳',
    `title`          varchar(255)          DEFAULT NULL COMMENT '视频名称',
    PRIMARY KEY (`id`),
    KEY `time` (`publish_time`) USING BTREE,
    KEY `author` (`author_id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 115
  DEFAULT CHARSET = utf8 COMMENT ='视频表';

