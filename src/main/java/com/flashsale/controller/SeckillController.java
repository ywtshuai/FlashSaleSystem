package com.flashsale.controller;

import com.flashsale.common.ApiResponse;
import com.flashsale.dto.SeckillRequestResponse;
import com.flashsale.dto.SeckillResultResponse;
import com.flashsale.service.SeckillService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seckill")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;

    @PostMapping("/{productId}")
    public ApiResponse<SeckillRequestResponse> seckill(@PathVariable Long productId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(UserController.CURRENT_USER_ID);
        return ApiResponse.success(seckillService.execute(userId, productId));
    }

    @GetMapping("/result")
    public ApiResponse<SeckillResultResponse> getResult(@RequestParam Long productId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(UserController.CURRENT_USER_ID);
        return ApiResponse.success(seckillService.getResult(userId, productId));
    }
}
