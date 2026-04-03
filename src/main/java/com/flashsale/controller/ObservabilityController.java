package com.flashsale.controller;

import com.flashsale.common.ApiResponse;
import com.flashsale.dto.SeckillMetricsSnapshot;
import com.flashsale.service.SeckillMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/observability")
@RequiredArgsConstructor
public class ObservabilityController {

    private final SeckillMetricsService seckillMetricsService;

    @GetMapping("/seckill")
    public ApiResponse<SeckillMetricsSnapshot> getSeckillMetrics() {
        return ApiResponse.success(seckillMetricsService.snapshot());
    }
}
