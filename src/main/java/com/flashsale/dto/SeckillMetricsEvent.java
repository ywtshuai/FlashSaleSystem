package com.flashsale.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SeckillMetricsEvent {

    private String requestId;
    private Long userId;
    private Long productId;
    private Long orderId;
    private String stage;
    private String detail;
    private Long timestamp;
}
