package com.flashsale.controller;

import com.flashsale.common.ApiResponse;
import com.flashsale.dto.OrderResponse;
import com.flashsale.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> getOrder(@PathVariable Long orderId) {
        return ApiResponse.success(orderService.getOrderById(orderId));
    }
}
