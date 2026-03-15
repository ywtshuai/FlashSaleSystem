package com.flashsale.service;

import com.flashsale.dto.LoginRequest;
import com.flashsale.dto.RegisterRequest;
import com.flashsale.dto.UserInfoResponse;
import com.flashsale.dto.UserLoginResponse;

public interface UserService {

    Long register(RegisterRequest request);

    UserLoginResponse login(LoginRequest request);

    UserInfoResponse getCurrentUser(Long userId);
}
