package com.flashsale.service.impl;

import com.flashsale.properties.ServiceDiscoveryProperties;
import com.flashsale.service.ServiceEndpointResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DiscoveryServiceEndpointResolver implements ServiceEndpointResolver {

    private final ObjectProvider<DiscoveryClient> discoveryClientProvider;
    private final ServiceDiscoveryProperties discoveryProperties;

    @Override
    public String resolve(String serviceId, String fallbackBaseUrl) {
        if (discoveryProperties.isEnabled()) {
            DiscoveryClient discoveryClient = discoveryClientProvider.getIfAvailable();
            if (discoveryClient != null) {
                List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
                if (!instances.isEmpty()) {
                    return instances.get(0).getUri().toString();
                }
            }
        }
        if (StringUtils.hasText(fallbackBaseUrl)) {
            return fallbackBaseUrl;
        }
        throw new IllegalStateException("No available instance for service: " + serviceId);
    }
}
