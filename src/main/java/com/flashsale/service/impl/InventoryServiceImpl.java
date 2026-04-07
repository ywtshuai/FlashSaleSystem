package com.flashsale.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flashsale.config.ReadOnlyRoute;
import com.flashsale.entity.Inventory;
import com.flashsale.exception.BusinessException;
import com.flashsale.mapper.InventoryMapper;
import com.flashsale.service.InventoryService;
import com.flashsale.util.RedisKeyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "flash-sale.microservice", name = "enabled", havingValue = "false", matchIfMissing = true)
public class InventoryServiceImpl implements InventoryService {

    private static final String INIT_LOCK_VALUE = "1";

    private final InventoryMapper inventoryMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    @ReadOnlyRoute
    public Inventory getByProductId(Long productId) {
        return findInventory(productId);
    }

    @Override
    @ReadOnlyRoute
    public int getAvailableStock(Long productId) {
        return getByProductId(productId).getAvailableStock();
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
    @Transactional(rollbackFor = Exception.class)
    public void deductDatabaseStock(Long productId) {
        while (true) {
            Inventory inventory = findInventory(productId);
            if (inventory.getAvailableStock() <= 0) {
                throw new BusinessException("数据库库存不足");
            }
            int updated = inventoryMapper.deductStockWithVersion(productId, inventory.getVersion());
            if (updated > 0) {
                return;
            }
        }
    }

    @Override
    public void incrementRedisStock(Long productId) {
        redisTemplate.opsForValue().increment(RedisKeyUtil.seckillStockKey(productId));
    }

    private Inventory findInventory(Long productId) {
        Inventory inventory = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getProductId, productId));
        if (inventory == null) {
            throw new BusinessException("库存记录不存在");
        }
        return inventory;
    }
}
