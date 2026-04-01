package com.flashsale.service;

import com.flashsale.service.impl.SnowflakeIdGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SnowflakeIdGeneratorTest {

    private final SnowflakeIdGenerator generator = new SnowflakeIdGenerator();

    @Test
    void shouldGenerateIncreasingIds() {
        long first = generator.nextId();
        long second = generator.nextId();
        long third = generator.nextId();

        assertTrue(first > 0);
        assertTrue(second > first);
        assertTrue(third > second);
    }
}
