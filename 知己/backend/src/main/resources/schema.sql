-- 知己 - 建表脚本
-- 可重复执行：Spring Boot 每次启动都会运行本文件
-- mochou 库：用户、反馈；chou 库：照片墙图片

-- ============ mochou 库 ============
CREATE TABLE IF NOT EXISTS `user`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(10)     NOT NULL COMMENT '用户名：唯一、非空、最多10个字符',
    `password`    VARCHAR(200)    NOT NULL COMMENT '密码：PBKDF2 加盐哈希（salt:hash）',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '用户表';

CREATE TABLE IF NOT EXISTS `feedback`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(10)     NOT NULL DEFAULT '' COMMENT '提交人（可为空）',
    `content`     VARCHAR(1000)   NOT NULL COMMENT '反馈 / 投诉内容',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '反馈与投诉';

-- ============ chou 库：照片墙 ============
CREATE DATABASE IF NOT EXISTS `chou`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `chou`.`photo`
(
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`     VARCHAR(10)     NOT NULL COMMENT '上传者',
    `filename`     VARCHAR(255)    NOT NULL COMMENT '原始文件名',
    `content_type` VARCHAR(100)    NOT NULL COMMENT 'MIME 类型',
    `size_bytes`   INT UNSIGNED    NOT NULL COMMENT '字节数',
    `data`         LONGBLOB        NOT NULL COMMENT '图片二进制内容',
    `create_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    PRIMARY KEY (`id`),
    KEY `idx_photo_username` (`username`),
    KEY `idx_photo_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '照片墙图片（存放于 chou 库）';
