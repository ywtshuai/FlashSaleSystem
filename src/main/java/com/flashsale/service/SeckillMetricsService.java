package com.flashsale.service;

import com.flashsale.dto.SeckillMetricsSnapshot;
import com.flashsale.dto.SeckillOrderMessage;

public interface SeckillMetricsService {

    void recordQueued(SeckillOrderMessage message);

    void recordProducerFailure(SeckillOrderMessage message, String reason);

    void recordConsumerSuccess(SeckillOrderMessage message, long latencyMs);

    void recordConsumerBusinessFailure(SeckillOrderMessage message, String reason);

    void recordConsumerRetryableFailure(SeckillOrderMessage message, String reason);

    void recordDlt(SeckillOrderMessage message, String topic);

    SeckillMetricsSnapshot snapshot();
}
