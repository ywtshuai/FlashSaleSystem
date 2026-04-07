package com.flashsale.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "flash-sale.services")
public class FlashSaleServiceProperties {

    private String orderServiceUrl;
    private String inventoryServiceUrl;
    private String orderServiceId;
    private String inventoryServiceId;
}
