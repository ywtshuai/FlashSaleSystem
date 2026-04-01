package com.flashsale.service.impl;

import com.flashsale.service.IdGenerator;
import org.springframework.stereotype.Component;

@Component
public class SnowflakeIdGenerator implements IdGenerator {

    private static final long START_TIMESTAMP = 1704067200000L;
    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private final long workerId = 1L & MAX_WORKER_ID;
    private final long datacenterId = 1L & MAX_DATACENTER_ID;
    private long sequence;
    private long lastTimestamp = -1L;

    @Override
    public synchronized long nextId() {
        long timestamp = currentTimestamp();
        if (timestamp < lastTimestamp) {
            throw new IllegalStateException("Clock moved backwards. Refusing to generate id");
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = waitUntilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;
        return ((timestamp - START_TIMESTAMP) << TIMESTAMP_LEFT_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    private long waitUntilNextMillis(long lastTimestamp) {
        long timestamp = currentTimestamp();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTimestamp();
        }
        return timestamp;
    }

    private long currentTimestamp() {
        return System.currentTimeMillis();
    }
}
