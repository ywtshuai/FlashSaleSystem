package com.flashsale.util;

import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class PasswordUtil {

    private PasswordUtil() {
    }

    public static String generateSalt() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String encrypt(String password, String salt) {
        String source = password + salt;
        return DigestUtils.md5DigestAsHex(source.getBytes(StandardCharsets.UTF_8));
    }
}
