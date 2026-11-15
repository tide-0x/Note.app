package com.iteam.examproject.data;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.regex.Pattern;

/**
 * 密码摘要工具。
 *
 * 存储格式：sha256$迭代次数$盐(Base64)$摘要(Base64)
 * 例如：sha256$10000$q83n...$Yk2p...
 *
 * 每个密码用独立的随机盐，迭代 10000 次 SHA-256，避免明文入库、也避免彩虹表。
 */
public final class PasswordUtil {
    private static final String ALGORITHM = "SHA-256";
    private static final String PREFIX = "sha256";
    private static final String SEPARATOR = "$";
    private static final int ITERATIONS = 10000;
    private static final int SALT_BYTES = 16;

    private PasswordUtil() {
    }

    /**
     * 生成可直接入库的密码摘要。
     */
    public static String hash(String rawPassword) {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        byte[] digest = digest(rawPassword, salt, ITERATIONS);
        return PREFIX + SEPARATOR + ITERATIONS + SEPARATOR
                + Base64.encodeToString(salt, Base64.NO_WRAP) + SEPARATOR
                + Base64.encodeToString(digest, Base64.NO_WRAP);
    }

    /**
     * 判断某个已存储的值是否是新格式的摘要。
     */
    public static boolean isHashed(String stored) {
        return stored != null && stored.startsWith(PREFIX + SEPARATOR);
    }

    /**
     * 校验密码。
     * 对旧版本以明文存储的记录也能通过（按明文比对），以便登录成功后自动升级为摘要。
     */
    public static boolean verify(String rawPassword, String stored) {
        if (rawPassword == null || stored == null) {
            return false;
        }
        if (!isHashed(stored)) {
            // 旧数据：早期版本直接存明文
            return stored.equals(rawPassword);
        }
        String[] parts = stored.split(Pattern.quote(SEPARATOR));
        if (parts.length != 4) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.decode(parts[2], Base64.NO_WRAP);
            byte[] expected = Base64.decode(parts[3], Base64.NO_WRAP);
            // 使用长度无关比较，避免时序攻击
            return MessageDigest.isEqual(expected, digest(rawPassword, salt, iterations));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] digest(String rawPassword, byte[] salt, int iterations) {
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            byte[] current = digestOnce(md, salt, rawPassword.getBytes(StandardCharsets.UTF_8));
            for (int i = 1; i < iterations; i++) {
                md.reset();
                current = md.digest(current);
            }
            return current;
        } catch (NoSuchAlgorithmException e) {
            // Android 平台一定支持 SHA-256，这里不会发生
            throw new IllegalStateException("当前设备不支持 " + ALGORITHM, e);
        }
    }

    private static byte[] digestOnce(MessageDigest md, byte[] salt, byte[] password) {
        md.update(salt);
        md.update(password);
        return md.digest();
    }
}
