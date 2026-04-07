package com.flashsale.order.service;

import com.flashsale.order.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class InventoryRemoteClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${flash-sale.services.inventory-service-url}")
    private String inventoryServiceUrl;

    public void deduct(Long productId) {
        restClientBuilder.baseUrl(inventoryServiceUrl).build().post()
                .uri("/internal/inventories/{productId}/deduct", productId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, result) -> {
                    throw new BusinessException("数据库库存不足");
                })
                .toBodilessEntity();
    }
}
