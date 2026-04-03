package com.flashsale.service;

import com.flashsale.dto.SeckillOrderMessage;
import com.flashsale.entity.OrderInfo;
import com.flashsale.exception.BusinessException;
import com.flashsale.service.impl.SeckillOrderConsumer;
import com.flashsale.service.impl.SeckillServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeckillOrderConsumerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private SeckillServiceImpl seckillService;

    @Mock
    private SeckillMetricsService seckillMetricsService;

    @InjectMocks
    private SeckillOrderConsumer consumer;

    @Test
    void shouldMarkSuccessWhenDuplicateOrderAlreadyExists() {
        SeckillOrderMessage message = buildMessage();
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(9001L);
        when(orderService.existsByUserIdAndProductId(1L, 1L)).thenReturn(true);
        when(orderService.getByUserIdAndProductId(1L, 1L)).thenReturn(orderInfo);

        consumer.consume(message);

        verify(seckillService).markSuccess(1L, 1L, 9001L);
        verify(inventoryService, never()).deductDatabaseStock(1L);
    }

    @Test
    void shouldCompensateImmediatelyForBusinessException() {
        SeckillOrderMessage message = buildMessage();
        when(orderService.existsByUserIdAndProductId(1L, 1L)).thenReturn(false);
        doThrow(new BusinessException("数据库库存不足")).when(inventoryService).deductDatabaseStock(1L);

        consumer.consume(message);

        verify(seckillService).rollbackRedisReservation(1L, 1L);
        verify(seckillService).markFailed(1L, 1L, "数据库库存不足");
    }

    @Test
    void shouldThrowUnexpectedExceptionToTriggerRetry() {
        SeckillOrderMessage message = buildMessage();
        when(orderService.existsByUserIdAndProductId(1L, 1L)).thenReturn(false);
        doThrow(new RuntimeException("temporary db issue")).when(inventoryService).deductDatabaseStock(1L);

        assertThrows(RuntimeException.class, () -> consumer.consume(message));

        verify(seckillService, never()).rollbackRedisReservation(1L, 1L);
        verify(seckillService, never()).markFailed(1L, 1L, "系统繁忙");
    }

    @Test
    void shouldHandleDltMessageWithCompensation() {
        SeckillOrderMessage message = buildMessage();

        consumer.handleDlt(message, "flash-sale-seckill-topic-dlt");

        verify(seckillService).rollbackRedisReservation(1L, 1L);
        verify(seckillService).markFailed(1L, 1L, "系统繁忙，已进入死信队列:flash-sale-seckill-topic-dlt");
    }

    private SeckillOrderMessage buildMessage() {
        SeckillOrderMessage message = new SeckillOrderMessage();
        message.setRequestId("req-1");
        message.setUserId(1L);
        message.setProductId(1L);
        message.setOrderId(10001L);
        return message;
    }
}
