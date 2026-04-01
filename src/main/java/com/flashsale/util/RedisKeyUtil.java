package com.flashsale.util;

public final class RedisKeyUtil {

    private RedisKeyUtil() {
    }

    public static String seckillStockKey(Long productId) {
        return "seckill:stock:" + productId;
    }

    public static String seckillUsersKey(Long productId) {
        return "seckill:users:" + productId;
    }

    public static String seckillResultKey(Long userId, Long productId) {
        return "seckill:result:" + userId + ":" + productId;
    }

    public static String seckillInitLockKey(Long productId) {
        return "seckill:init:lock:" + productId;
    }
}
