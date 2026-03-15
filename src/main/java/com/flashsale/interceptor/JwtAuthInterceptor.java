package com.flashsale.interceptor;

import com.flashsale.controller.UserController;
import com.flashsale.exception.BusinessException;
import com.flashsale.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authorization = request.getHeader(AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new BusinessException("未登录或Token格式错误");
        }

        String token = authorization.substring(BEARER_PREFIX.length());
        Long userId = jwtUtil.parseUserId(token);
        request.setAttribute(UserController.CURRENT_USER_ID, userId);
        return true;
    }
}
