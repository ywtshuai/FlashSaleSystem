package com.flashsale.order.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderResponse {

    private Long orderId;
    private Long userId;
    private Long productId;
    private Integer status;
    private LocalDateTime createTime;
}
