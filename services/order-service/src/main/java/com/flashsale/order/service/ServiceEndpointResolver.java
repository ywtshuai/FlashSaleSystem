package com.flashsale.order.service;

public interface ServiceEndpointResolver {

    String resolve(String serviceId, String fallbackBaseUrl);
}
