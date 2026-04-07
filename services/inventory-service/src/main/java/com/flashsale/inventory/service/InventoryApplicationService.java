package com.flashsale.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flashsale.inventory.entity.Inventory;
import com.flashsale.inventory.exception.BusinessException;
import com.flashsale.inventory.mapper.InventoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryApplicationService {

    private final InventoryMapper inventoryMapper;

    public Inventory getByProductId(Long productId) {
        Inventory inventory = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getProductId, productId));
        if (inventory == null) {
            throw new BusinessException("库存记录不存在");
        }
        return inventory;
    }

    public int getAvailableStock(Long productId) {
        return getByProductId(productId).getAvailableStock();
    }

    @Transactional(rollbackFor = Exception.class)
    public void deduct(Long productId) {
        while (true) {
            Inventory inventory = getByProductId(productId);
            if (inventory.getAvailableStock() <= 0) {
                throw new BusinessException("数据库库存不足");
            }
            int updated = inventoryMapper.deductStockWithVersion(productId, inventory.getVersion());
            if (updated > 0) {
                return;
            }
        }
    }
}
