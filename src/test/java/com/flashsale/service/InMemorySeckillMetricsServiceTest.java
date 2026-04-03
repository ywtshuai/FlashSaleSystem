package com.flashsale.service;

import com.flashsale.dto.SeckillMetricsSnapshot;
import com.flashsale.dto.SeckillOrderMessage;
import com.flashsale.service.impl.InMemorySeckillMetricsService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InMemorySeckillMetricsServiceTest {

    @Test
    void shouldAggregateSeckillMetrics() {
        InMemorySeckillMetricsService metricsService = new InMemorySeckillMetricsService();
        SeckillOrderMessage message = new SeckillOrderMessage();
        message.setRequestId("req-1");
        message.setUserId(1L);
        message.setProductId(1L);
        message.setOrderId(100L);
        message.setTimestamp(System.currentTimeMillis() - 120L);

        metricsService.recordQueued(message);
        metricsService.recordProducerFailure(message, "producer error");
        metricsService.recordConsumerRetryableFailure(message, "temporary error");
        metricsService.recordConsumerBusinessFailure(message, "business error");
        metricsService.recordConsumerSuccess(message, 120L);
        metricsService.recordDlt(message, "flash-sale-seckill-topic-dlt");

        SeckillMetricsSnapshot snapshot = metricsService.snapshot();

        assertEquals(1L, snapshot.getQueuedRequests());
        assertEquals(1L, snapshot.getProducerFailures());
        assertEquals(1L, snapshot.getConsumerRetryableFailures());
        assertEquals(1L, snapshot.getConsumerBusinessFailures());
        assertEquals(1L, snapshot.getConsumerSuccesses());
        assertEquals(1L, snapshot.getDltMessages());
        assertEquals(120D, snapshot.getAverageConsumerLatencyMs());
        assertEquals(1, snapshot.getRecentDltEvents().size());
    }
}
