package com.flashsale.order.interceptor;

import com.flashsale.order.exception.BusinessException;
import com.flashsale.order.properties.InternalAuthProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class InternalRequestAuthInterceptor implements HandlerInterceptor {

    private final InternalAuthProperties internalAuthProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!internalAuthProperties.isEnabled()) {
            return true;
        }

        String token = request.getHeader(internalAuthProperties.getHeaderName());
        if (!internalAuthProperties.getToken().equals(token)) {
            throw new BusinessException("非法内部调用");
        }

        String serviceName = request.getHeader(internalAuthProperties.getServiceNameHeader());
        if (!internalAuthProperties.getAllowedServices().isEmpty()
                && !internalAuthProperties.getAllowedServices().contains(serviceName)) {
            throw new BusinessException("未授权的服务调用方");
        }
        return true;
    }
}
