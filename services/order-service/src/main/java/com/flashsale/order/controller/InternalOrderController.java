package com.flashsale.order.controller;

import com.flashsale.order.dto.OrderResponse;
import com.flashsale.order.entity.OrderInfo;
import com.flashsale.order.service.OrderApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/orders")
@RequiredArgsConstructor
public class InternalOrderController {

    private final OrderApplicationService orderService;

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@PathVariable Long orderId) {
        return orderService.getOrderById(orderId);
    }

    @GetMapping("/exist")
    public Boolean exists(@RequestParam Long userId, @RequestParam Long productId) {
        return orderService.existsByUserIdAndProductId(userId, productId);
    }

    @GetMapping("/by-user-product")
    public OrderInfo getByUserAndProduct(@RequestParam Long userId, @RequestParam Long productId) {
        return orderService.getByUserIdAndProductId(userId, productId);
    }
}
