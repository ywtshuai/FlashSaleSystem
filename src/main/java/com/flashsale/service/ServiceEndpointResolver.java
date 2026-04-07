package com.flashsale.service;

public interface ServiceEndpointResolver {

    String resolve(String serviceId, String fallbackBaseUrl);
}
