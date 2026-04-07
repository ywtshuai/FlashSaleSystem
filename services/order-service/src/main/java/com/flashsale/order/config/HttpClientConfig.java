package com.flashsale.order.config;

import com.flashsale.order.properties.InternalAuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class HttpClientConfig {

    private final InternalAuthProperties internalAuthProperties;
    private final Environment environment;

    @Bean
    public RestClient.Builder restClientBuilder() {
        RestClient.Builder builder = RestClient.builder();
        if (internalAuthProperties.isEnabled()) {
            builder.defaultHeaders(headers -> {
                headers.set(internalAuthProperties.getHeaderName(), internalAuthProperties.getToken());
                headers.set(internalAuthProperties.getServiceNameHeader(),
                        environment.getProperty("spring.application.name", "order-service"));
            });
        }
        return builder;
    }
}
