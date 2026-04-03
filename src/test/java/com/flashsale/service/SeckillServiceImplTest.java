package com.flashsale.service;

import com.flashsale.dto.SeckillRequestResponse;
import com.flashsale.dto.SeckillResultResponse;
import com.flashsale.entity.OrderInfo;
import com.flashsale.exception.BusinessException;
import com.flashsale.service.impl.SeckillServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeckillServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private OrderService orderService;

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private SeckillProducer seckillProducer;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    private SeckillServiceImpl seckillService;

    @Test
    void shouldQueueSeckillRequestWhenLuaSucceeds() {
        seckillService = new SeckillServiceImpl(redisTemplate, inventoryService, orderService, idGenerator, seckillProducer);
        when(redisTemplate.execute(any(), anyList(), anyString(), anyString())).thenReturn(1L);
        when(idGenerator.nextId()).thenReturn(123456L);

        SeckillRequestResponse response = seckillService.execute(1L, 1L);

        assertEquals("QUEUING", response.getStatus());
        assertEquals(123456L, response.getOrderId());
        assertNotNull(response.getRequestId());
        verify(inventoryService).ensureRedisStock(1L);
        verify(seckillProducer).send(any());
    }

    @Test
    void shouldRollbackRedisReservationWhenProducerFails() {
        seckillService = new SeckillServiceImpl(redisTemplate, inventoryService, orderService, idGenerator, seckillProducer);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.execute(any(), anyList(), anyString(), anyString())).thenReturn(1L);
        when(idGenerator.nextId()).thenReturn(123456L);
        doThrow(new RuntimeException("kafka down")).when(seckillProducer).send(any());

        BusinessException exception = assertThrows(BusinessException.class, () -> seckillService.execute(1L, 1L));

        assertEquals("秒杀排队失败，请稍后重试", exception.getMessage());
        verify(valueOperations).set(anyString(), anyString(), any());
        verify(setOperations).remove(anyString(), anyString());
        verify(inventoryService).incrementRedisStock(1L);
    }

    @Test
    void shouldReadSuccessResultFromDatabaseWhenRedisResultMissing() {
        seckillService = new SeckillServiceImpl(redisTemplate, inventoryService, orderService, idGenerator, seckillProducer);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(888L);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(orderService.getByUserIdAndProductId(1L, 1L)).thenReturn(orderInfo);

        SeckillResultResponse response = seckillService.getResult(1L, 1L);

        assertEquals("SUCCESS", response.getStatus());
        assertEquals(888L, response.getOrderId());
    }
}
