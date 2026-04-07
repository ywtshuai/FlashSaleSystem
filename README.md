# Flash Sale System

基于 `Java 17 + Spring Boot 3 + MyBatis-Plus + MySQL 8 + Redis + Kafka` 的分布式秒杀系统课程作业工程。

当前版本已经完成：
- Phase 0：修复测试基线，调整 MyBatis 扫描边界，补齐订单/库存持久层骨架
- Phase 1：落地单体版秒杀核心链路，支持 Redis 预扣、Kafka 异步下单、幂等兜底、Snowflake 订单 ID、结果查询
- Phase 2：补齐课程演示所需的 Docker、docker-compose、Nginx 静态页与 API 反向代理、读写分离骨架和压测说明
- Continue：补齐 Kafka 重试 / 死信队列骨架、主从复制初始化脚本、秒杀主链路单元测试、Testcontainers 端到端集成测试、Kafka 观测指标
- Next：完成 `user-product-service + order-service + inventory-service` 微服务拆分第一版
- Current：微服务拆分第一版已完成基础联调骨架，三个服务均可独立打包
- Current+：已落地微服务间统一鉴权与 Eureka 注册发现，并保留 URL 直连回退

## 当前架构

### 1. user-product-service
- 保留用户注册、登录、JWT 鉴权
- 保留商品详情缓存查询
- 保留秒杀入口、结果查询、观测接口
- 负责 Redis 预扣、发送 Kafka 消息
- 微服务模式下通过 HTTP 调用 `order-service` 和 `inventory-service`

### 2. order-service
- 独立消费 Kafka 秒杀消息
- 创建订单记录
- 更新 Redis 秒杀结果
- 对外提供内部订单查询接口

### 3. inventory-service
- 独立维护库存表
- 提供库存查询和库存扣减内部接口

### 4. discovery-server
- 提供 Eureka 注册中心
- 为 `user-product-service`、`order-service`、`inventory-service` 提供注册与发现

## 仓库结构

```text
.
├── src/                        user-product-service 主工程
├── services/discovery-server   注册中心
├── services/order-service      订单微服务
├── services/inventory-service  库存微服务
├── deploy/                     Nginx、MySQL 等部署配置
├── docs/                       压测与说明文档
└── docker-compose.yml          三服务联调编排
```

## 主要接口

### 对外接口
- `POST /api/users/register`
- `POST /api/users/login`
- `GET /api/products/{id}`
- `POST /api/seckill/{productId}`
- `GET /api/seckill/result?productId=1`
- `GET /api/orders/{orderId}`
- `GET /api/admin/observability/seckill`

### 内部接口

`inventory-service`
- `GET /internal/inventories/{productId}`
- `GET /internal/inventories/{productId}/stock`
- `POST /internal/inventories/{productId}/deduct`

`order-service`
- `GET /internal/orders/{orderId}`
- `GET /internal/orders/exist?userId=...&productId=...`
- `GET /internal/orders/by-user-product?userId=...&productId=...`

## 运行模式

### 单体兼容模式
- 默认模式
- `flash-sale.microservice.enabled=false`
- root 工程内部仍可本地完成订单/库存处理，方便开发和测试

### 微服务模式
- `flash-sale.microservice.enabled=true`
- root 工程变为 `user-product-service`
- root 自己的 Kafka 消费器会关闭
- `order-service` 独立消费 Kafka
- `inventory-service` 独立处理库存

### 服务鉴权与注册发现
- 内部接口 `/internal/**` 已接入统一鉴权拦截器
- 服务间调用统一携带 `X-Internal-Token` 与 `X-Service-Name`
- 当 `FLASH_SALE_DISCOVERY_ENABLED=true` 时，服务会注册到 `discovery-server`
- 远程调用会优先按服务名发现实例，找不到时回退到配置中的直连 URL

## 当前验证状态

截至 2026-04-07，已验证：
- root 工程 `mvn test` 通过
- root 工程 `mvn clean package -DskipTests` 通过
- `services/order-service` 可独立执行 `mvn clean package -DskipTests`
- `services/inventory-service` 可独立执行 `mvn clean package -DskipTests`
- `services/discovery-server` 可独立执行 `mvn clean package -DskipTests`

这意味着当前仓库已经具备：
- 单体兼容模式继续开发和测试的能力
- 微服务模式下按服务分别构建镜像和部署的能力

## Docker 演示环境

启动：

```bash
docker-compose up --build
```

主要入口：
- Nginx：`http://localhost`
- discovery-server：`http://localhost:8761`
- user-product-service：`http://localhost:8080`
- order-service：`http://localhost:8081`
- inventory-service：`http://localhost:8082`

## 测试

执行：

```bash
mvn test
```

当前 root 工程已验证：
- `UserControllerTest`
- `SnowflakeIdGeneratorTest`
- `SeckillServiceImplTest`
- `SeckillOrderConsumerTest`
- `InMemorySeckillMetricsServiceTest`
- `SeckillFlowIntegrationTest`

说明：
- `SeckillFlowIntegrationTest` 基于 Testcontainers 启动 MySQL、Redis、Kafka
- 如果环境没有 Docker，该测试会自动跳过

## 微服务拆分说明

- root 工程当前仍保留本地实现，作为单体兼容模式
- 当微服务开关打开时，`OrderService` 和 `InventoryService` 会自动切换到远程实现
- 这样可以同时保留本地开发效率和课程作业展示所需的微服务版本
- `order-service` 当前负责消费 Kafka、创建订单、更新 Redis 秒杀结果
- `inventory-service` 当前负责数据库库存查询和乐观锁扣减
- `discovery-server` 当前负责服务注册与实例发现
- 内部接口统一由共享 token 进行服务间鉴权
- 当前三服务默认仍共用 `flash_sale_system` 数据库，后续再演进为独立库

## 后续演进

当前仓库已经完成第一版微服务拆分。下一步更适合继续推进的是：
- 主从复制自动化绑定脚本
- 订单服务 / 库存服务独立数据库
- 微服务独立测试集与跨服务集成测试
