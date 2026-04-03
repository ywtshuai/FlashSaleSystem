package com.flashsale.service.impl;

import com.flashsale.dto.SeckillMetricsEvent;
import com.flashsale.dto.SeckillMetricsSnapshot;
import com.flashsale.dto.SeckillOrderMessage;
import com.flashsale.service.SeckillMetricsService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

@Service
public class InMemorySeckillMetricsService implements SeckillMetricsService {

    private static final int MAX_RECENT_DLT_EVENTS = 10;

    private final LongAdder queuedRequests = new LongAdder();
    private final LongAdder producerFailures = new LongAdder();
    private final LongAdder consumerSuccesses = new LongAdder();
    private final LongAdder consumerBusinessFailures = new LongAdder();
    private final LongAdder consumerRetryableFailures = new LongAdder();
    private final LongAdder dltMessages = new LongAdder();
    private final LongAdder totalConsumerLatencyMs = new LongAdder();
    private final AtomicLong latencySamples = new AtomicLong();
    private final ConcurrentLinkedDeque<SeckillMetricsEvent> recentDltEvents = new ConcurrentLinkedDeque<>();

    private volatile SeckillMetricsEvent lastQueuedEvent;
    private volatile SeckillMetricsEvent lastSuccessEvent;
    private volatile SeckillMetricsEvent lastFailureEvent;

    @Override
    public void recordQueued(SeckillOrderMessage message) {
        queuedRequests.increment();
        lastQueuedEvent = buildEvent(message, "QUEUED", "秒杀请求进入消息队列");
    }

    @Override
    public void recordProducerFailure(SeckillOrderMessage message, String reason) {
        producerFailures.increment();
        lastFailureEvent = buildEvent(message, "PRODUCER_FAIL", reason);
    }

    @Override
    public void recordConsumerSuccess(SeckillOrderMessage message, long latencyMs) {
        consumerSuccesses.increment();
        totalConsumerLatencyMs.add(latencyMs);
        latencySamples.incrementAndGet();
        lastSuccessEvent = buildEvent(message, "CONSUMER_SUCCESS", "消费成功，耗时 " + latencyMs + " ms");
    }

    @Override
    public void recordConsumerBusinessFailure(SeckillOrderMessage message, String reason) {
        consumerBusinessFailures.increment();
        lastFailureEvent = buildEvent(message, "CONSUMER_BUSINESS_FAIL", reason);
    }

    @Override
    public void recordConsumerRetryableFailure(SeckillOrderMessage message, String reason) {
        consumerRetryableFailures.increment();
        lastFailureEvent = buildEvent(message, "CONSUMER_RETRYABLE_FAIL", reason);
    }

    @Override
    public void recordDlt(SeckillOrderMessage message, String topic) {
        dltMessages.increment();
        SeckillMetricsEvent event = buildEvent(message, "DLT", "进入死信队列: " + topic);
        lastFailureEvent = event;
        recentDltEvents.addFirst(event);
        while (recentDltEvents.size() > MAX_RECENT_DLT_EVENTS) {
            recentDltEvents.removeLast();
        }
    }

    @Override
    public SeckillMetricsSnapshot snapshot() {
        SeckillMetricsSnapshot snapshot = new SeckillMetricsSnapshot();
        snapshot.setQueuedRequests(queuedRequests.sum());
        snapshot.setProducerFailures(producerFailures.sum());
        snapshot.setConsumerSuccesses(consumerSuccesses.sum());
        snapshot.setConsumerBusinessFailures(consumerBusinessFailures.sum());
        snapshot.setConsumerRetryableFailures(consumerRetryableFailures.sum());
        snapshot.setDltMessages(dltMessages.sum());
        long samples = latencySamples.get();
        snapshot.setAverageConsumerLatencyMs(samples == 0 ? 0D : totalConsumerLatencyMs.sum() * 1.0 / samples);
        snapshot.setLastQueuedEvent(lastQueuedEvent);
        snapshot.setLastSuccessEvent(lastSuccessEvent);
        snapshot.setLastFailureEvent(lastFailureEvent);
        snapshot.setRecentDltEvents(new ArrayList<>(recentDltEvents));
        return snapshot;
    }

    private SeckillMetricsEvent buildEvent(SeckillOrderMessage message, String stage, String detail) {
        return new SeckillMetricsEvent(
                message == null ? null : message.getRequestId(),
                message == null ? null : message.getUserId(),
                message == null ? null : message.getProductId(),
                message == null ? null : message.getOrderId(),
                stage,
                detail,
                System.currentTimeMillis()
        );
    }
}
