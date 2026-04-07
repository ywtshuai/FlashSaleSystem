package com.flashsale.service.impl;

import com.flashsale.dto.OrderResponse;
import com.flashsale.dto.RemoteOrderLookupResponse;
import com.flashsale.entity.OrderInfo;
import com.flashsale.exception.BusinessException;
import com.flashsale.properties.FlashSaleServiceProperties;
import com.flashsale.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "flash-sale.microservice", name = "enabled", havingValue = "true")
public class RemoteOrderServiceImpl implements OrderService {

    private final RestClient.Builder restClientBuilder;
    private final FlashSaleServiceProperties serviceProperties;

    @Override
    public OrderInfo createOrder(Long orderId, Long userId, Long productId) {
        throw new UnsupportedOperationException("微服务模式下订单创建由 order-service 负责");
    }

    @Override
    public boolean existsByUserIdAndProductId(Long userId, Long productId) {
        Boolean exists = restClient().get()
                .uri(uriBuilder -> uriBuilder.path("/internal/orders/exist")
                        .queryParam("userId", userId)
                        .queryParam("productId", productId)
                        .build())
                .retrieve()
                .body(Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public OrderInfo getByUserIdAndProductId(Long userId, Long productId) {
        RemoteOrderLookupResponse response = restClient().get()
                .uri(uriBuilder -> uriBuilder.path("/internal/orders/by-user-product")
                        .queryParam("userId", userId)
                        .queryParam("productId", productId)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, result) -> {
                    throw new BusinessException("订单不存在");
                })
                .body(RemoteOrderLookupResponse.class);
        if (response == null || response.getOrderId() == null) {
            return null;
        }
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(response.getOrderId());
        orderInfo.setUserId(response.getUserId());
        orderInfo.setProductId(response.getProductId());
        orderInfo.setStatus(response.getStatus());
        orderInfo.setCreateTime(response.getCreateTime());
        return orderInfo;
    }

    @Override
    public OrderResponse getOrderById(Long orderId) {
        OrderResponse response = restClient().get()
                .uri("/internal/orders/{orderId}", orderId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, result) -> {
                    throw new BusinessException("订单不存在");
                })
                .body(OrderResponse.class);
        if (response == null) {
            throw new BusinessException("订单不存在");
        }
        return response;
    }

    private RestClient restClient() {
        return restClientBuilder.baseUrl(serviceProperties.getOrderServiceUrl()).build();
    }
}
