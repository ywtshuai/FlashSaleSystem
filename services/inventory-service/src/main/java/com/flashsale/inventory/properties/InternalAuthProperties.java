package com.flashsale.inventory.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "flash-sale.internal-auth")
public class InternalAuthProperties {

    private boolean enabled;
    private String token;
    private String headerName = "X-Internal-Token";
    private String serviceNameHeader = "X-Service-Name";
    private List<String> allowedServices = new ArrayList<>();
}
