package com.flashsale.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserInfoResponse {

    private Long userId;
    private String username;
    private LocalDateTime createTime;
}
