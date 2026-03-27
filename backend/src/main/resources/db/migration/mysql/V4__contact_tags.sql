SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE user_contact_tag (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '标签所属用户ID',
    friend_id BIGINT NOT NULL COMMENT '被标记好友ID',
    tag_name VARCHAR(24) NOT NULL COMMENT '联系人标签',

    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    del_token VARCHAR(64) DEFAULT '0',

    FOREIGN KEY (user_id) REFERENCES user_profile(id) ON DELETE CASCADE,
    FOREIGN KEY (friend_id) REFERENCES user_profile(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_contact_tag (user_id, friend_id, tag_name),
    INDEX idx_user_contact_tag_user_friend (user_id, friend_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户联系人标签';

SET FOREIGN_KEY_CHECKS = 1;
