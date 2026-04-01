package com.flashsale.dto;

import lombok.Data;

@Data
public class SeckillResultResponse {

    private String requestId;
    private Long orderId;
    private String status;
    private String message;
}
