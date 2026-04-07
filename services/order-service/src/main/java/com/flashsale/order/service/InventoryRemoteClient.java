package com.flashsale.order.service;

import com.flashsale.order.exception.BusinessException;
import com.flashsale.order.properties.ServiceClientProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class InventoryRemoteClient {

    private final RestClient.Builder restClientBuilder;
    private final ServiceClientProperties serviceClientProperties;
    private final ServiceEndpointResolver serviceEndpointResolver;

    public void deduct(Long productId) {
        restClientBuilder.baseUrl(serviceEndpointResolver.resolve(
                        serviceClientProperties.getInventoryServiceId(),
                        serviceClientProperties.getInventoryServiceUrl())).build().post()
                .uri("/internal/inventories/{productId}/deduct", productId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, result) -> {
                    throw new BusinessException("数据库库存不足");
                })
                .toBodilessEntity();
    }
}
