package com.flashsale.order.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "flash-sale.services")
public class ServiceClientProperties {

    private String inventoryServiceId;
    private String inventoryServiceUrl;
}
