USE short_video_platfrom;

ALTER TABLE `t_relation`
    ADD COLUMN IF NOT EXISTS `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD KEY `idx_follow_created` (`follow_id`, `created_at`, `follower_id`),
    ADD KEY `idx_follower_created` (`follower_id`, `created_at`, `follow_id`);

ALTER TABLE `t_follower`
    ADD KEY `idx_to_created` (`to_user_id`, `created_at`, `from_user_id`);

ALTER TABLE `t_outbox`
    ADD KEY `idx_aggregate_created` (`aggregate_type`, `created_at`);

CREATE USER IF NOT EXISTS 'canal'@'%' IDENTIFIED BY 'canal';
GRANT SELECT, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'canal'@'%';
FLUSH PRIVILEGES;
