package com.flashsale.service.impl;

import com.flashsale.dto.SeckillOrderMessage;
import com.flashsale.dto.SeckillRequestResponse;
import com.flashsale.dto.SeckillResultResponse;
import com.flashsale.entity.OrderInfo;
import com.flashsale.exception.BusinessException;
import com.flashsale.service.IdGenerator;
import com.flashsale.service.InventoryService;
import com.flashsale.service.OrderService;
import com.flashsale.service.SeckillProducer;
import com.flashsale.service.SeckillService;
import com.flashsale.util.RedisKeyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    private static final String STATUS_QUEUING = "QUEUING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAIL = "FAIL";
    private static final Duration RESULT_TTL = Duration.ofHours(6);
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT = new DefaultRedisScript<>();

    static {
        SECKILL_SCRIPT.setResultType(Long.class);
        SECKILL_SCRIPT.setScriptText("""
                if redis.call('sismember', KEYS[2], ARGV[1]) == 1 then
                    return 2
                end
                local stock = tonumber(redis.call('get', KEYS[1]))
                if stock == nil then
                    return 3
                end
                if stock <= 0 then
                    return 0
                end
                redis.call('decr', KEYS[1])
                redis.call('sadd', KEYS[2], ARGV[1])
                redis.call('set', KEYS[3], 'QUEUING', 'EX', ARGV[2])
                return 1
                """);
    }

    private final StringRedisTemplate redisTemplate;
    private final InventoryService inventoryService;
    private final OrderService orderService;
    private final IdGenerator idGenerator;
    private final SeckillProducer seckillProducer;

    @Override
    public SeckillRequestResponse execute(Long userId, Long productId) {
        inventoryService.ensureRedisStock(productId);

        String stockKey = RedisKeyUtil.seckillStockKey(productId);
        String usersKey = RedisKeyUtil.seckillUsersKey(productId);
        String resultKey = RedisKeyUtil.seckillResultKey(userId, productId);
        Long scriptResult = redisTemplate.execute(
                SECKILL_SCRIPT,
                Arrays.asList(stockKey, usersKey, resultKey),
                String.valueOf(userId),
                String.valueOf(RESULT_TTL.toSeconds())
        );

        if (scriptResult == null) {
            throw new BusinessException("秒杀请求处理失败");
        }
        if (scriptResult == 0L) {
            throw new BusinessException("已售罄");
        }
        if (scriptResult == 2L) {
            throw new BusinessException("请勿重复秒杀");
        }
        if (scriptResult == 3L) {
            throw new BusinessException("库存缓存未就绪");
        }

        long orderId = idGenerator.nextId();
        String requestId = UUID.randomUUID().toString().replace("-", "");

        SeckillOrderMessage message = new SeckillOrderMessage();
        message.setRequestId(requestId);
        message.setUserId(userId);
        message.setProductId(productId);
        message.setOrderId(orderId);
        message.setTimestamp(System.currentTimeMillis());

        try {
            seckillProducer.send(message);
        } catch (Exception exception) {
            markFailed(userId, productId, "消息投递失败");
            rollbackRedisReservation(userId, productId);
            throw new BusinessException("秒杀排队失败，请稍后重试");
        }

        SeckillRequestResponse response = new SeckillRequestResponse();
        response.setRequestId(requestId);
        response.setOrderId(orderId);
        response.setStatus(STATUS_QUEUING);
        response.setMessage("排队中");
        return response;
    }

    @Override
    public SeckillResultResponse getResult(Long userId, Long productId) {
        String resultValue = redisTemplate.opsForValue().get(RedisKeyUtil.seckillResultKey(userId, productId));
        SeckillResultResponse response = new SeckillResultResponse();
        if (resultValue == null) {
            OrderInfo orderInfo = orderService.getByUserIdAndProductId(userId, productId);
            if (orderInfo != null) {
                response.setStatus(STATUS_SUCCESS);
                response.setOrderId(orderInfo.getId());
                response.setMessage("秒杀成功");
            } else {
                response.setStatus("EMPTY");
                response.setMessage("暂无秒杀记录");
            }
            return response;
        }

        if (STATUS_QUEUING.equals(resultValue)) {
            response.setStatus(STATUS_QUEUING);
            response.setMessage("排队中");
            return response;
        }

        if (resultValue.startsWith(STATUS_SUCCESS + ":")) {
            response.setStatus(STATUS_SUCCESS);
            response.setOrderId(Long.parseLong(resultValue.substring((STATUS_SUCCESS + ":").length())));
            response.setMessage("秒杀成功");
            return response;
        }

        response.setStatus(STATUS_FAIL);
        response.setMessage(resultValue.substring((STATUS_FAIL + ":").length()));
        return response;
    }

    public void markSuccess(Long userId, Long productId, Long orderId) {
        redisTemplate.opsForValue().set(
                RedisKeyUtil.seckillResultKey(userId, productId),
                STATUS_SUCCESS + ":" + orderId,
                RESULT_TTL
        );
    }

    public void markFailed(Long userId, Long productId, String reason) {
        redisTemplate.opsForValue().set(
                RedisKeyUtil.seckillResultKey(userId, productId),
                STATUS_FAIL + ":" + reason,
                RESULT_TTL
        );
    }

    public void rollbackRedisReservation(Long userId, Long productId) {
        redisTemplate.opsForSet().remove(RedisKeyUtil.seckillUsersKey(productId), String.valueOf(userId));
        inventoryService.incrementRedisStock(productId);
    }
}
