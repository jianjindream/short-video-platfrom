USE camps_tiktok;

ALTER TABLE `t_user`
    ADD COLUMN IF NOT EXISTS `collect_count` bigint(20) NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS `t_collect` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `user_id` bigint(20) NOT NULL,
    `video_id` bigint(20) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_video` (`user_id`, `video_id`) USING BTREE,
    KEY `idx_user_id` (`user_id`) USING BTREE,
    KEY `idx_video_id` (`video_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
