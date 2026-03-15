package com.flashsale.controller;

import com.flashsale.common.ApiResponse;
import com.flashsale.dto.LoginRequest;
import com.flashsale.dto.RegisterRequest;
import com.flashsale.dto.UserInfoResponse;
import com.flashsale.dto.UserLoginResponse;
import com.flashsale.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    public static final String CURRENT_USER_ID = "currentUserId";

    private final UserService userService;

    /**
     * 用户注册接口。
     */
    @PostMapping("/register")
    public ApiResponse<Long> register(@Valid @RequestBody RegisterRequest request) {
        Long userId = userService.register(request);
        return ApiResponse.success(userId);
    }

    /**
     * 用户登录接口。
     */
    @PostMapping("/login")
    public ApiResponse<UserLoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(userService.login(request));
    }

    /**
     * 获取当前登录用户信息。
     */
    @GetMapping("/me")
    public ApiResponse<UserInfoResponse> me(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(CURRENT_USER_ID);
        return ApiResponse.success(userService.getCurrentUser(userId));
    }
}
