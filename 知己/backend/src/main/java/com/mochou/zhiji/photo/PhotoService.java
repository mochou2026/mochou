package com.mochou.zhiji.photo;

import com.mochou.zhiji.common.BizException;
import com.mochou.zhiji.user.UserRepository;
import com.mochou.zhiji.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * 照片墙：上传、列表、读取图片内容。
 */
@Service
public class PhotoService {

    /** 单张图片大小上限，与 application.yml 里的 multipart 配置保持一致 */
    public static final long MAX_BYTES = 12L * 1024 * 1024;
    /** 一次最多返回多少张 */
    public static final int LIST_LIMIT = 120;

    private final PhotoRepository photoRepository;
    private final UserRepository userRepository;

    public PhotoService(PhotoRepository photoRepository, UserRepository userRepository) {
        this.photoRepository = photoRepository;
        this.userRepository = userRepository;
    }

    /** 上传图片，落库到 chou.photo */
    public PhotoSummary upload(String rawUsername, MultipartFile file) {
        String username = UserService.normalizeUsername(rawUsername);
        if (!userRepository.existsByUsername(username)) {
            throw new BizException("用户不存在");
        }
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要上传的图片");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BizException("图片不能超过 " + (MAX_BYTES / 1024 / 1024) + "MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BizException("只能上传图片文件");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new BizException("图片读取失败，请重试");
        }

        long id = photoRepository.insert(username, safeFilename(file.getOriginalFilename()), contentType, bytes);
        return photoRepository.findById(id)
                .orElseThrow(() -> new BizException("图片保存失败，请重试"));
    }

    /** 图片列表；username 为空返回所有人的（照片墙），否则只返回该用户的（个人中心） */
    public List<PhotoSummary> list(String username) {
        String filter = (username == null || username.isBlank()) ? null : username.trim();
        return photoRepository.findAll(filter, LIST_LIMIT);
    }

    public Optional<PhotoData> data(long id) {
        return photoRepository.findData(id);
    }

    /** 去掉路径、限制长度，避免文件名里带奇怪内容 */
    private static String safeFilename(String original) {
        if (original == null || original.isBlank()) {
            return "image";
        }
        String name = original.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.trim();
        if (name.isEmpty()) {
            return "image";
        }
        return name.length() > 200 ? name.substring(name.length() - 200) : name;
    }
}
