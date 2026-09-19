package com.mochou.zhiji.photo;

/**
 * 照片墙图片的元信息（不含二进制内容），直接返回给前端。
 */
public record PhotoSummary(
        long id,
        String username,
        String filename,
        long size,
        String createTime,
        /** 前端直接把这个地址塞进 img 的 src */
        String url) {
}
