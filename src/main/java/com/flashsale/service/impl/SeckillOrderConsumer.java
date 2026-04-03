package com.flashsale.service.impl;

import com.flashsale.dto.SeckillOrderMessage;
import com.flashsale.exception.BusinessException;
import com.flashsale.entity.OrderInfo;
import com.flashsale.service.InventoryService;
import com.flashsale.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SeckillOrderConsumer {

    private final OrderService orderService;
    private final InventoryService inventoryService;
    private final SeckillServiceImpl seckillService;

    @KafkaListener(topics = "#{@flashSaleKafkaProperties.seckillTopic}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional(rollbackFor = Exception.class)
    public void consume(SeckillOrderMessage message) {
        Long userId = message.getUserId();
        Long productId = message.getProductId();
        Long orderId = message.getOrderId();
        try {
            if (orderService.existsByUserIdAndProductId(userId, productId)) {
                OrderInfo existingOrder = orderService.getByUserIdAndProductId(userId, productId);
                seckillService.markSuccess(userId, productId, existingOrder == null ? orderId : existingOrder.getId());
                return;
            }
            inventoryService.deductDatabaseStock(productId);
            orderService.createOrder(orderId, userId, productId);
            seckillService.markSuccess(userId, productId, orderId);
        } catch (BusinessException exception) {
            if ("请勿重复秒杀".equals(exception.getMessage())) {
                OrderInfo existingOrder = orderService.getByUserIdAndProductId(userId, productId);
                seckillService.markSuccess(userId, productId, existingOrder == null ? orderId : existingOrder.getId());
                return;
            }
            seckillService.rollbackRedisReservation(userId, productId);
            seckillService.markFailed(userId, productId, exception.getMessage());
            throw exception;
        } catch (Exception exception) {
            seckillService.rollbackRedisReservation(userId, productId);
            seckillService.markFailed(userId, productId, "系统繁忙");
            throw exception;
        }
    }
}
