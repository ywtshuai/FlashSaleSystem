package com.flashsale.service;

import com.flashsale.dto.SeckillOrderMessage;

public interface SeckillProducer {

    void send(SeckillOrderMessage message);
}
