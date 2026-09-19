package com.mochou.zhiji.user;

import com.mochou.zhiji.common.BizException;
import com.mochou.zhiji.security.PasswordHasher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * 注册 / 登录 / 修改密码的业务规则。
 *
 * <p>用户名规则：不可为空、不可重复、不超过 10 个字符（按字符数而非字节数计）。<br>
 * 密码规则：简易，不做复杂度要求，只要求非空。</p>
 */
@Service
public class UserService {

    /** 用户名最大字符数。 */
    public static final int USERNAME_MAX_LENGTH = 10;
    /** 密码最大字符数，只是防止超长输入，不限制密码形式。 */
    public static final int PASSWORD_MAX_LENGTH = 64;

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 注册新用户：校验 -> 查重 -> 加盐哈希 -> 落库。
     *
     * @return 新用户
     */
    public UserRecord register(String rawUsername, String rawPassword) {
        String username = normalizeUsername(rawUsername);
        String password = normalizePassword(rawPassword, "密码不能为空");

        if (userRepository.existsByUsername(username)) {
            throw new BizException("用户名已存在");
        }
        try {
            userRepository.insert(username, PasswordHasher.hash(password));
        } catch (DuplicateKeyException ex) {
            // 并发下唯一索引兜底，保证用户名不可重复
            throw new BizException("用户名已存在");
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BizException("注册失败，请稍后重试"));
    }

    /**
     * 登录校验。
     */
    public UserRecord login(String rawUsername, String rawPassword) {
        String username = normalizeUsername(rawUsername);
        String password = normalizePassword(rawPassword, "密码不能为空");
        UserRecord user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BizException("用户不存在"));
        if (!PasswordHasher.matches(password, user.password())) {
            throw new BizException("密码错误");
        }
        return user;
    }

    /**
     * 修改密码：用户必须存在且原密码正确。
     */
    public void changePassword(String rawUsername, String rawOldPassword, String rawNewPassword) {
        String username = normalizeUsername(rawUsername);
        String oldPassword = normalizePassword(rawOldPassword, "请输入原密码");
        String newPassword = normalizePassword(rawNewPassword, "请输入新密码");

        UserRecord user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BizException("用户不存在"));
        if (!PasswordHasher.matches(oldPassword, user.password())) {
            throw new BizException("原密码错误");
        }
        userRepository.updatePassword(user.id(), PasswordHasher.hash(newPassword));
    }

    /** 用户名：去掉首尾空白，非空且不超过 10 个字符。 */
    public static String normalizeUsername(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new BizException("用户名不能为空");
        }
        String username = raw.trim();
        if (username.codePointCount(0, username.length()) > USERNAME_MAX_LENGTH) {
            throw new BizException("用户名不能超过" + USERNAME_MAX_LENGTH + "个字符");
        }
        return username;
    }

    /** 密码：非空即可，仅限制一个安全上限。 */
    public static String normalizePassword(String raw, String emptyMessage) {
        if (raw == null || raw.isEmpty()) {
            throw new BizException(emptyMessage);
        }
        if (raw.codePointCount(0, raw.length()) > PASSWORD_MAX_LENGTH) {
            throw new BizException("密码不能超过" + PASSWORD_MAX_LENGTH + "个字符");
        }
        return raw;
    }
}
