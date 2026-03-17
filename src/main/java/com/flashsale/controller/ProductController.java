package com.flashsale.controller;

import com.flashsale.common.ApiResponse;
import com.flashsale.entity.Product;
import com.flashsale.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 商品详情查询接口，优先走 Redis 缓存，未命中时再回源数据库。
     */
    @GetMapping("/{id}")
    public ApiResponse<Product> getProductById(@PathVariable Long id) {
        return ApiResponse.success(productService.getProductById(id));
    }
}