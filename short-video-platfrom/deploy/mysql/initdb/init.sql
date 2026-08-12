SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS short_video_platfrom;
USE short_video_platfrom;

DROP TABLE IF EXISTS `t_comment`;
CREATE TABLE `t_comment` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `user_id` bigint(20) NOT NULL,
    `video_id` bigint(20) NOT NULL,
    `comment_text` varchar(255) NOT NULL,
    `create_time` datetime NOT NULL,
    PRIMARY KEY (`id`),
    KEY `videoIdIdx` (`video_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1206 DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `t_relation`;
CREATE TABLE `t_relation` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `follow_id` bigint(20) NOT NULL,
    `follower_id` bigint(20) NOT NULL,
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_from_to` (`follower_id`, `follow_id`) USING BTREE,
    KEY `idx_follow_id` (`follow_id`, `created_at`, `follower_id`) USING BTREE,
    KEY `idx_follower_id` (`follower_id`, `created_at`, `follow_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1096 DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `t_follower`;
CREATE TABLE `t_follower` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `from_user_id` bigint(20) NOT NULL,
    `to_user_id` bigint(20) NOT NULL,
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_from_to` (`from_user_id`, `to_user_id`) USING BTREE,
    KEY `idx_to_user_id` (`to_user_id`, `created_at`, `from_user_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `t_outbox`;
CREATE TABLE `t_outbox` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `aggregate_type` varchar(32) NOT NULL,
    `aggregate_id` varchar(64) NOT NULL,
    `event_id` varchar(64) DEFAULT NULL,
    `topic` varchar(64) DEFAULT NULL,
    `event_key` varchar(128) DEFAULT NULL,
    `payload` text NOT NULL,
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `processed` tinyint(1) NOT NULL DEFAULT 0,
    `status` varchar(16) NOT NULL DEFAULT 'NEW',
    `retry_count` int NOT NULL DEFAULT 0,
    `next_retry_at` datetime DEFAULT NULL,
    `last_error` varchar(1024) DEFAULT NULL,
    `processed_at` datetime DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_event_id` (`event_id`) USING BTREE,
    KEY `idx_processed_created` (`processed`, `created_at`) USING BTREE,
    KEY `idx_topic_status_retry` (`topic`, `processed`, `status`, `next_retry_at`) USING BTREE,
    KEY `idx_aggregate_created` (`aggregate_type`, `created_at`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `t_message_dead_letter`;
CREATE TABLE `t_message_dead_letter` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `event_id` varchar(128) NOT NULL,
    `topic` varchar(64) NOT NULL,
    `consumer_group` varchar(128) NOT NULL,
    `payload` text NOT NULL,
    `error_message` varchar(1024) DEFAULT NULL,
    `retry_count` int NOT NULL DEFAULT 0,
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_event_id` (`event_id`) USING BTREE,
    KEY `idx_topic_created` (`topic`, `created_at`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `t_favorite`;
CREATE TABLE `t_favorite` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `user_id` bigint(20) NOT NULL,
    `video_id` bigint(20) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `userIdtoVideoIdIdx` (`user_id`, `video_id`) USING BTREE,
    KEY `userIdIdx` (`user_id`) USING BTREE,
    KEY `videoIdx` (`video_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1229 DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `t_collect`;
CREATE TABLE `t_collect` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `user_id` bigint(20) NOT NULL,
    `video_id` bigint(20) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `userIdtoVideoIdIdx` (`user_id`, `video_id`) USING BTREE,
    KEY `userIdIdx` (`user_id`) USING BTREE,
    KEY `videoIdx` (`video_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `user_name` varchar(255) NOT NULL,
    `password` varchar(255) NOT NULL,
    `follow_count` bigint(20) NOT NULL DEFAULT 0,
    `follower_count` bigint(20) NOT NULL DEFAULT 0,
    `total_favorited` bigint(20) NOT NULL DEFAULT 0,
    `favorite_count` bigint(20) NOT NULL DEFAULT 0,
    `collect_count` bigint(20) NOT NULL DEFAULT 0,
    `signature` varchar(1024) DEFAULT NULL,
    `avatar` varchar(1024) DEFAULT NULL,
    `background_image` varchar(1024) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `name_password_idx` (`user_name`, `password`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=20044 DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `t_video`;
CREATE TABLE `t_video` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `author_id` bigint(20) NOT NULL,
    `play_url` varchar(255) NOT NULL,
    `cover_url` varchar(255) NOT NULL,
    `favorite_count` bigint(20) NOT NULL DEFAULT 0,
    `comment_count` bigint(20) NOT NULL DEFAULT 0,
    `publish_time` datetime NOT NULL,
    `title` varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `time` (`publish_time`) USING BTREE,
    KEY `author` (`author_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=115 DEFAULT CHARSET=utf8;

CREATE USER IF NOT EXISTS 'canal'@'%' IDENTIFIED BY 'canal';
GRANT SELECT, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'canal'@'%';
FLUSH PRIVILEGES;

SET FOREIGN_KEY_CHECKS = 1;
