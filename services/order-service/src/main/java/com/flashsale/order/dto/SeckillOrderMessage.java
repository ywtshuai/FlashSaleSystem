package com.flashsale.order.dto;

import lombok.Data;

@Data
public class SeckillOrderMessage {

    private String requestId;
    private Long userId;
    private Long productId;
    private Long orderId;
    private Long timestamp;
}
