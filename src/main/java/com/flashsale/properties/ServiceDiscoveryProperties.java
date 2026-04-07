package com.flashsale.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "flash-sale.discovery")
public class ServiceDiscoveryProperties {

    private boolean enabled;
}
