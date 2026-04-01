# JMeter 压测说明

## 目标
- 验证热点商品详情是否优先命中 Redis
- 验证秒杀接口是否快速返回“排队中”
- 验证最终成功订单数不超过库存且单用户只生成一单

## 建议脚本
- 线程组 1：`GET /api/products/1`
  - 并发用户数：100
  - Ramp-Up：10 秒
  - 循环次数：20
- 线程组 2：`POST /api/seckill/1`
  - 并发用户数：50
  - Ramp-Up：5 秒
  - 每个用户只发 1 次

## 前置条件
- 先通过登录接口拿到多个用户的 Bearer Token
- 确认 Redis 中已存在 `seckill:stock:1`
- 确认 Kafka、MySQL、Redis 和 Nginx 已经通过 `docker-compose up --build` 启动

## 观察点
- Redis 命中率和 QPS
- MySQL 主库写入速率是否平稳
- `order_info` 表订单数是否小于等于初始库存
- 相同用户是否只有一条 `user_id + product_id` 订单记录
