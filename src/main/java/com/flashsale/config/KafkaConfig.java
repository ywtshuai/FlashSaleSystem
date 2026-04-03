package com.flashsale.config;

import com.flashsale.properties.FlashSaleKafkaProperties;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    private final FlashSaleKafkaProperties kafkaProperties;

    @Bean
    public NewTopic seckillTopic() {
        return TopicBuilder.name(kafkaProperties.getSeckillTopic())
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic seckillDltTopic() {
        return TopicBuilder.name(kafkaProperties.getSeckillDltTopic())
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> new TopicPartition(kafkaProperties.getSeckillDltTopic(), record.partition())
        );
        FixedBackOff backOff = new FixedBackOff(
                kafkaProperties.getRetryInterval(),
                Math.max(0, kafkaProperties.getMaxAttempts() - 1L)
        );
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);
        return errorHandler;
    }
}
