package com.flashsale.service;

import com.flashsale.dto.OrderResponse;
import com.flashsale.entity.OrderInfo;

public interface OrderService {

    OrderInfo createOrder(Long orderId, Long userId, Long productId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    OrderInfo getByUserIdAndProductId(Long userId, Long productId);

    OrderResponse getOrderById(Long orderId);
}
