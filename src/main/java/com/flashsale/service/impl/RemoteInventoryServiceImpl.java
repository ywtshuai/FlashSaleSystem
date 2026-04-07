package com.flashsale.service.impl;

import com.flashsale.entity.Inventory;
import com.flashsale.exception.BusinessException;
import com.flashsale.properties.FlashSaleServiceProperties;
import com.flashsale.service.InventoryService;
import com.flashsale.service.ServiceEndpointResolver;
import com.flashsale.util.RedisKeyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "flash-sale.microservice", name = "enabled", havingValue = "true")
public class RemoteInventoryServiceImpl implements InventoryService {

    private static final String INIT_LOCK_VALUE = "1";

    private final RestClient.Builder restClientBuilder;
    private final FlashSaleServiceProperties serviceProperties;
    private final StringRedisTemplate redisTemplate;
    private final ServiceEndpointResolver serviceEndpointResolver;

    @Override
    public Inventory getByProductId(Long productId) {
        Inventory inventory = restClient().get()
                .uri("/internal/inventories/{productId}", productId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, result) -> {
                    throw new BusinessException("库存记录不存在");
                })
                .body(Inventory.class);
        if (inventory == null) {
            throw new BusinessException("库存记录不存在");
        }
        return inventory;
    }

    @Override
    public int getAvailableStock(Long productId) {
        Integer stock = restClient().get()
                .uri("/internal/inventories/{productId}/stock", productId)
                .retrieve()
                .body(Integer.class);
        if (stock == null) {
            throw new BusinessException("库存记录不存在");
        }
        return stock;
    }

    @Override
    public int ensureRedisStock(Long productId) {
        String stockKey = RedisKeyUtil.seckillStockKey(productId);
        String cachedStock = redisTemplate.opsForValue().get(stockKey);
        if (cachedStock != null) {
            return Integer.parseInt(cachedStock);
        }

        String lockKey = RedisKeyUtil.seckillInitLockKey(productId);
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, INIT_LOCK_VALUE, Duration.ofSeconds(5));
        if (Boolean.TRUE.equals(locked)) {
            try {
                cachedStock = redisTemplate.opsForValue().get(stockKey);
                if (cachedStock != null) {
                    return Integer.parseInt(cachedStock);
                }
                int availableStock = getAvailableStock(productId);
                redisTemplate.opsForValue().set(stockKey, String.valueOf(availableStock));
                return availableStock;
            } finally {
                redisTemplate.delete(lockKey);
            }
        }

        cachedStock = redisTemplate.opsForValue().get(stockKey);
        return cachedStock == null ? getAvailableStock(productId) : Integer.parseInt(cachedStock);
    }

    @Override
    public void deductDatabaseStock(Long productId) {
        restClient().post()
                .uri("/internal/inventories/{productId}/deduct", productId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, result) -> {
                    throw new BusinessException("数据库库存不足");
                })
                .toBodilessEntity();
    }

    @Override
    public void incrementRedisStock(Long productId) {
        redisTemplate.opsForValue().increment(RedisKeyUtil.seckillStockKey(productId));
    }

    private RestClient restClient() {
        return restClientBuilder.baseUrl(serviceEndpointResolver.resolve(
                serviceProperties.getInventoryServiceId(),
                serviceProperties.getInventoryServiceUrl())).build();
    }
}
