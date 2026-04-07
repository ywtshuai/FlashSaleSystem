package com.flashsale.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RemoteOrderLookupResponse {

    private Long orderId;
    private Long userId;
    private Long productId;
    private Integer status;
    private LocalDateTime createTime;
}
