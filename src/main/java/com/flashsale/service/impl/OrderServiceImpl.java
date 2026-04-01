package com.flashsale.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flashsale.config.ReadOnlyRoute;
import com.flashsale.dto.OrderResponse;
import com.flashsale.entity.OrderInfo;
import com.flashsale.exception.BusinessException;
import com.flashsale.mapper.OrderInfoMapper;
import com.flashsale.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderInfoMapper orderInfoMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderInfo createOrder(Long orderId, Long userId, Long productId) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setUserId(userId);
        orderInfo.setProductId(productId);
        orderInfo.setStatus(0);
        try {
            orderInfoMapper.insert(orderInfo);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("请勿重复秒杀");
        }
        return orderInfo;
    }

    @Override
    @ReadOnlyRoute
    public boolean existsByUserIdAndProductId(Long userId, Long productId) {
        Long count = orderInfoMapper.selectCount(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getUserId, userId)
                .eq(OrderInfo::getProductId, productId));
        return count != null && count > 0;
    }

    @Override
    @ReadOnlyRoute
    public OrderInfo getByUserIdAndProductId(Long userId, Long productId) {
        return orderInfoMapper.selectOne(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getUserId, userId)
                .eq(OrderInfo::getProductId, productId)
                .last("limit 1"));
    }

    @Override
    @ReadOnlyRoute
    public OrderResponse getOrderById(Long orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        if (orderInfo == null) {
            throw new BusinessException("订单不存在");
        }
        OrderResponse response = new OrderResponse();
        response.setOrderId(orderInfo.getId());
        response.setUserId(orderInfo.getUserId());
        response.setProductId(orderInfo.getProductId());
        response.setStatus(orderInfo.getStatus());
        response.setCreateTime(orderInfo.getCreateTime());
        return response;
    }
}
