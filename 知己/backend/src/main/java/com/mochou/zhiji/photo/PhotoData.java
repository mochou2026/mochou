package com.mochou.zhiji.photo;

/**
 * 图片二进制内容 + MIME 类型，用于 /api/photos/{id}/raw 输出。
 */
public record PhotoData(String contentType, byte[] data) {
}
