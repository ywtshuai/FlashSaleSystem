package com.flashsale.inventory.controller;

import com.flashsale.inventory.entity.Inventory;
import com.flashsale.inventory.service.InventoryApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/inventories")
@RequiredArgsConstructor
public class InternalInventoryController {

    private final InventoryApplicationService inventoryService;

    @GetMapping("/{productId}")
    public Inventory getInventory(@PathVariable Long productId) {
        return inventoryService.getByProductId(productId);
    }

    @GetMapping("/{productId}/stock")
    public Integer getAvailableStock(@PathVariable Long productId) {
        return inventoryService.getAvailableStock(productId);
    }

    @PostMapping("/{productId}/deduct")
    public void deduct(@PathVariable Long productId) {
        inventoryService.deduct(productId);
    }
}
