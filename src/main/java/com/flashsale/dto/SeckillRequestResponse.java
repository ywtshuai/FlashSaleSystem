package com.flashsale.dto;

import lombok.Data;

@Data
public class SeckillRequestResponse {

    private String requestId;
    private Long orderId;
    private String status;
    private String message;
}
