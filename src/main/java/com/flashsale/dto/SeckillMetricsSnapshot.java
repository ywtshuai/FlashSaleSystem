package com.flashsale.dto;

import lombok.Data;

import java.util.List;

@Data
public class SeckillMetricsSnapshot {

    private long queuedRequests;
    private long producerFailures;
    private long consumerSuccesses;
    private long consumerBusinessFailures;
    private long consumerRetryableFailures;
    private long dltMessages;
    private double averageConsumerLatencyMs;
    private SeckillMetricsEvent lastQueuedEvent;
    private SeckillMetricsEvent lastSuccessEvent;
    private SeckillMetricsEvent lastFailureEvent;
    private List<SeckillMetricsEvent> recentDltEvents;
}
