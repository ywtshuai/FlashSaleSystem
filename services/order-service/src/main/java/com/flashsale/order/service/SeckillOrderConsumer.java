package com.flashsale.order.service;

import com.flashsale.order.dto.SeckillOrderMessage;
import com.flashsale.order.entity.OrderInfo;
import com.flashsale.order.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SeckillOrderConsumer {

    private final OrderApplicationService orderService;
    private final InventoryRemoteClient inventoryRemoteClient;
    private final SeckillRedisStatusService redisStatusService;

    @KafkaListener(topics = "${flash-sale.kafka.seckill-topic}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional(rollbackFor = Exception.class)
    public void consume(SeckillOrderMessage message) {
        Long userId = message.getUserId();
        Long productId = message.getProductId();
        Long orderId = message.getOrderId();
        try {
            if (orderService.existsByUserIdAndProductId(userId, productId)) {
                OrderInfo existingOrder = orderService.getByUserIdAndProductId(userId, productId);
                redisStatusService.markSuccess(userId, productId, existingOrder == null ? orderId : existingOrder.getId());
                return;
            }
            inventoryRemoteClient.deduct(productId);
            orderService.createOrder(orderId, userId, productId);
            redisStatusService.markSuccess(userId, productId, orderId);
        } catch (BusinessException exception) {
            if ("请勿重复秒杀".equals(exception.getMessage())) {
                OrderInfo existingOrder = orderService.getByUserIdAndProductId(userId, productId);
                redisStatusService.markSuccess(userId, productId, existingOrder == null ? orderId : existingOrder.getId());
                return;
            }
            redisStatusService.rollbackReservation(userId, productId);
            redisStatusService.markFailed(userId, productId, exception.getMessage());
        } catch (Exception exception) {
            log.error("Retryable exception while consuming order message requestId={}", message.getRequestId(), exception);
            throw exception;
        }
    }

    @DltHandler
    public void handleDlt(SeckillOrderMessage message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        redisStatusService.rollbackReservation(message.getUserId(), message.getProductId());
        redisStatusService.markFailed(message.getUserId(), message.getProductId(), "系统繁忙，已进入死信队列:" + topic);
    }
}
