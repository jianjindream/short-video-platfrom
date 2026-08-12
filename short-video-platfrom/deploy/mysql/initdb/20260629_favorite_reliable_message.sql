USE camps_tiktok;

ALTER TABLE `t_outbox`
    ADD COLUMN IF NOT EXISTS `event_id` varchar(64) DEFAULT NULL AFTER `aggregate_id`,
    ADD COLUMN IF NOT EXISTS `topic` varchar(64) DEFAULT NULL AFTER `event_id`,
    ADD COLUMN IF NOT EXISTS `event_key` varchar(128) DEFAULT NULL AFTER `topic`,
    ADD COLUMN IF NOT EXISTS `status` varchar(16) NOT NULL DEFAULT 'NEW' AFTER `processed`,
    ADD COLUMN IF NOT EXISTS `retry_count` int NOT NULL DEFAULT 0 AFTER `status`,
    ADD COLUMN IF NOT EXISTS `next_retry_at` datetime DEFAULT NULL AFTER `retry_count`,
    ADD COLUMN IF NOT EXISTS `last_error` varchar(1024) DEFAULT NULL AFTER `next_retry_at`,
    ADD COLUMN IF NOT EXISTS `processed_at` datetime DEFAULT NULL AFTER `last_error`;

CREATE UNIQUE INDEX IF NOT EXISTS `uk_event_id` ON `t_outbox` (`event_id`);
CREATE INDEX IF NOT EXISTS `idx_topic_status_retry`
    ON `t_outbox` (`topic`, `processed`, `status`, `next_retry_at`);

CREATE TABLE IF NOT EXISTS `t_message_dead_letter` (
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
