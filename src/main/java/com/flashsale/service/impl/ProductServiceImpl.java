package com.flashsale.service.impl;

import com.flashsale.config.ReadOnlyRoute;
import com.flashsale.entity.Product;
import com.flashsale.exception.BusinessException;
import com.flashsale.mapper.ProductMapper;
import com.flashsale.service.ProductService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final String PRODUCT_CACHE_KEY_PREFIX = "product:detail:";
    private static final String PRODUCT_LOCK_KEY_PREFIX = "product:lock:";
    private static final String EMPTY_VALUE = "EMPTY";
    private static final long NULL_CACHE_MINUTES = 5L;
    private static final long CACHE_BASE_MINUTES = 60L;
    private static final int CACHE_RANDOM_MINUTES_BOUND = 31;
    private static final long LOCK_EXPIRE_SECONDS = 10L;
    private static final long RETRY_SLEEP_MILLIS = 50L;
    private static final int MAX_RETRY_TIMES = 20;
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>();

    static {
        UNLOCK_SCRIPT.setScriptText("if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end");
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    private final ProductMapper productMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 商品详情查询的缓存完整链路：
     * 1. 先查 Redis，命中直接返回，降低数据库压力。
     * 2. 命中 EMPTY 说明该商品不存在，直接拦截，防止缓存穿透。
     * 3. Redis 未命中时尝试抢分布式锁，只有抢到锁的线程回源数据库并重建缓存，防止缓存击穿。
     * 4. 写缓存时使用“固定 TTL + 随机 TTL”，避免大量缓存在同一时间一起失效，防止缓存雪崩。
     */
    @Override
    @ReadOnlyRoute
    public Product getProductById(Long id) {
        String cacheKey = PRODUCT_CACHE_KEY_PREFIX + id;
        String cachedValue = redisTemplate.opsForValue().get(cacheKey);

        if (cachedValue != null) {
            if (EMPTY_VALUE.equals(cachedValue)) {
                throw new BusinessException("商品不存在");
            }
            return deserializeProduct(cachedValue);
        }

        String lockKey = PRODUCT_LOCK_KEY_PREFIX + id;
        String lockValue = Thread.currentThread().getId() + "-" + System.nanoTime();
        boolean locked = tryLock(lockKey, lockValue);

        if (locked) {
            try {
                // 双重检查，避免等待锁期间其他线程已经把缓存重建好。
                String doubleCheckValue = redisTemplate.opsForValue().get(cacheKey);
                if (doubleCheckValue != null) {
                    if (EMPTY_VALUE.equals(doubleCheckValue)) {
                        throw new BusinessException("商品不存在");
                    }
                    return deserializeProduct(doubleCheckValue);
                }

                Product product = productMapper.selectById(id);
                if (product == null) {
                    // 防缓存穿透：数据库也不存在时，缓存一个短 TTL 的空值，拦住恶意或无效请求。
                    redisTemplate.opsForValue().set(cacheKey, EMPTY_VALUE, NULL_CACHE_MINUTES, TimeUnit.MINUTES);
                    throw new BusinessException("商品不存在");
                }

                // 防缓存雪崩：在基础过期时间上增加随机时间，让热点 Key 分散失效。
                long ttlMinutes = CACHE_BASE_MINUTES + ThreadLocalRandom.current().nextInt(CACHE_RANDOM_MINUTES_BOUND);
                redisTemplate.opsForValue().set(cacheKey, serializeProduct(product), ttlMinutes, TimeUnit.MINUTES);
                return product;
            } finally {
                unlock(lockKey, lockValue);
            }
        }

        // 防缓存击穿：未抢到锁的线程不直接打数据库，而是短暂等待后重试读取缓存。
        for (int i = 0; i < MAX_RETRY_TIMES; i++) {
            sleepBriefly();
            String retryValue = redisTemplate.opsForValue().get(cacheKey);
            if (retryValue == null) {
                continue;
            }
            if (EMPTY_VALUE.equals(retryValue)) {
                throw new BusinessException("商品不存在");
            }
            return deserializeProduct(retryValue);
        }

        // 如果锁持有线程执行时间偏长，最后兜底一次重试，仍然遵循同一套缓存逻辑。
        return getProductById(id);
    }

    private boolean tryLock(String lockKey, String lockValue) {
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, Duration.ofSeconds(LOCK_EXPIRE_SECONDS));
        return Boolean.TRUE.equals(success);
    }

    private void unlock(String lockKey, String lockValue) {
        redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(lockKey), lockValue);
    }

    private String serializeProduct(Product product) {
        try {
            return objectMapper.writeValueAsString(product);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("商品缓存序列化失败");
        }
    }

    private Product deserializeProduct(String cachedValue) {
        try {
            return objectMapper.readValue(cachedValue, Product.class);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("商品缓存反序列化失败");
        }
    }

    private void sleepBriefly() {
        try {
            TimeUnit.MILLISECONDS.sleep(RETRY_SLEEP_MILLIS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException("线程被中断");
        }
    }
}
