package com.flashsale.service.impl;

import com.flashsale.dto.SeckillOrderMessage;
import com.flashsale.properties.FlashSaleKafkaProperties;
import com.flashsale.service.SeckillProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaSeckillProducer implements SeckillProducer {

    private final KafkaTemplate<String, SeckillOrderMessage> kafkaTemplate;
    private final FlashSaleKafkaProperties kafkaProperties;

    @Override
    public void send(SeckillOrderMessage message) {
        kafkaTemplate.send(kafkaProperties.getSeckillTopic(), message.getRequestId(), message).join();
    }
}
