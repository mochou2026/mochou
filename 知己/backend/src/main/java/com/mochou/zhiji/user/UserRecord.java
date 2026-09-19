package com.mochou.zhiji.user;

/**
 * 用户表里参与业务的最小字段集合。
 */
public record UserRecord(long id, String username, String password) {
}
