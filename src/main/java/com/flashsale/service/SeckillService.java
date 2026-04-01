package com.flashsale.service;

import com.flashsale.dto.SeckillRequestResponse;
import com.flashsale.dto.SeckillResultResponse;

public interface SeckillService {

    SeckillRequestResponse execute(Long userId, Long productId);

    SeckillResultResponse getResult(Long userId, Long productId);
}
