package com.flashsale.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flashsale.order.dto.OrderResponse;
import com.flashsale.order.entity.OrderInfo;
import com.flashsale.order.exception.BusinessException;
import com.flashsale.order.mapper.OrderInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderApplicationService {

    private final OrderInfoMapper orderInfoMapper;

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

    public boolean existsByUserIdAndProductId(Long userId, Long productId) {
        Long count = orderInfoMapper.selectCount(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getUserId, userId)
                .eq(OrderInfo::getProductId, productId));
        return count != null && count > 0;
    }

    public OrderInfo getByUserIdAndProductId(Long userId, Long productId) {
        return orderInfoMapper.selectOne(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getUserId, userId)
                .eq(OrderInfo::getProductId, productId)
                .last("limit 1"));
    }

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
