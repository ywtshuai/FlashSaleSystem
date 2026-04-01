package com.flashsale.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(-1)
public class ReadOnlyRouteAspect {

    @Around("@within(com.flashsale.config.ReadOnlyRoute) || @annotation(com.flashsale.config.ReadOnlyRoute)")
    public Object routeToSlave(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            DataSourceContextHolder.useSlave();
            return joinPoint.proceed();
        } finally {
            DataSourceContextHolder.clear();
        }
    }
}
