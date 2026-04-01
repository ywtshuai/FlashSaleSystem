package com.flashsale.service;

import com.flashsale.entity.Inventory;

public interface InventoryService {

    Inventory getByProductId(Long productId);

    int getAvailableStock(Long productId);

    int ensureRedisStock(Long productId);

    void deductDatabaseStock(Long productId);

    void incrementRedisStock(Long productId);
}
