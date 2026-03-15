package com.flashsale.dto;

import lombok.Data;

@Data
public class UserLoginResponse {

    private Long userId;
    private String username;
    private String token;
    private Long expireAt;
}
