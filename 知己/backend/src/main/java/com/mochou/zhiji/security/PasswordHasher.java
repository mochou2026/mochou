package com.mochou.zhiji.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * 密码哈希工具：PBKDF2WithHmacSHA256 + 随机盐，存储格式为 {@code saltHex:hashHex}。
 *
 * <p>密码本身不做复杂度要求，但绝不明文落库。</p>
 */
public final class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 10_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_BYTES = 16;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of();

    private PasswordHasher() {
    }

    /** 生成 {@code saltHex:hashHex} 形式的密文。 */
    public static String hash(String rawPassword) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return HEX.formatHex(salt) + ":" + HEX.formatHex(pbkdf2(rawPassword, salt));
    }

    /** 校验明文密码与已存密文是否匹配（恒定时间比较）。 */
    public static boolean matches(String rawPassword, String stored) {
        if (rawPassword == null || stored == null) {
            return false;
        }
        int separator = stored.indexOf(':');
        if (separator <= 0 || separator == stored.length() - 1) {
            return false;
        }
        byte[] salt;
        byte[] expected;
        try {
            salt = HEX.parseHex(stored.substring(0, separator));
            expected = HEX.parseHex(stored.substring(separator + 1));
        } catch (IllegalArgumentException ex) {
            return false;
        }
        return MessageDigest.isEqual(expected, pbkdf2(rawPassword, salt));
    }

    private static byte[] pbkdf2(String rawPassword, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(rawPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (Exception ex) {
            throw new IllegalStateException("密码加密失败", ex);
        } finally {
            spec.clearPassword();
        }
    }
}
