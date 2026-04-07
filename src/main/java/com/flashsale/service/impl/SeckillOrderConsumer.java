package com.flashsale.service.impl;

import com.flashsale.dto.SeckillOrderMessage;
import com.flashsale.exception.BusinessException;
import com.flashsale.entity.OrderInfo;
import com.flashsale.service.InventoryService;
import com.flashsale.service.SeckillMetricsService;
import com.flashsale.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.kafka.support.KafkaHeaders;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "flash-sale.microservice", name = "enabled", havingValue = "false", matchIfMissing = true)
public class SeckillOrderConsumer {

    private final OrderService orderService;
    private final InventoryService inventoryService;
    private final SeckillServiceImpl seckillService;
    private final SeckillMetricsService seckillMetricsService;

    @KafkaListener(topics = "#{@flashSaleKafkaProperties.seckillTopic}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional(rollbackFor = Exception.class)
    public void consume(SeckillOrderMessage message) {
        Long userId = message.getUserId();
        Long productId = message.getProductId();
        Long orderId = message.getOrderId();
        log.info("Consume seckill message requestId={}, userId={}, productId={}, orderId={}",
                message.getRequestId(), userId, productId, orderId);
        try {
            if (orderService.existsByUserIdAndProductId(userId, productId)) {
                OrderInfo existingOrder = orderService.getByUserIdAndProductId(userId, productId);
                seckillService.markSuccess(userId, productId, existingOrder == null ? orderId : existingOrder.getId());
                seckillMetricsService.recordConsumerSuccess(message, System.currentTimeMillis() - message.getTimestamp());
                return;
            }
            inventoryService.deductDatabaseStock(productId);
            orderService.createOrder(orderId, userId, productId);
            seckillService.markSuccess(userId, productId, orderId);
            seckillMetricsService.recordConsumerSuccess(message, System.currentTimeMillis() - message.getTimestamp());
        } catch (BusinessException exception) {
            if ("请勿重复秒杀".equals(exception.getMessage())) {
                OrderInfo existingOrder = orderService.getByUserIdAndProductId(userId, productId);
                seckillService.markSuccess(userId, productId, existingOrder == null ? orderId : existingOrder.getId());
                seckillMetricsService.recordConsumerSuccess(message, System.currentTimeMillis() - message.getTimestamp());
                return;
            }
            log.warn("Seckill business failure requestId={}, userId={}, productId={}, reason={}",
                    message.getRequestId(), userId, productId, exception.getMessage());
            seckillService.rollbackRedisReservation(userId, productId);
            seckillService.markFailed(userId, productId, exception.getMessage());
            seckillMetricsService.recordConsumerBusinessFailure(message, exception.getMessage());
        } catch (Exception exception) {
            log.error("Seckill retryable failure requestId={}, userId={}, productId={}",
                    message.getRequestId(), userId, productId, exception);
            seckillMetricsService.recordConsumerRetryableFailure(message, exception.getMessage());
            throw exception;
        }
    }

    @DltHandler
    public void handleDlt(SeckillOrderMessage message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("Seckill message moved to DLT requestId={}, userId={}, productId={}, topic={}",
                message.getRequestId(), message.getUserId(), message.getProductId(), topic);
        seckillService.rollbackRedisReservation(message.getUserId(), message.getProductId());
        seckillService.markFailed(message.getUserId(), message.getProductId(), "系统繁忙，已进入死信队列:" + topic);
        seckillMetricsService.recordDlt(message, topic);
    }
}
