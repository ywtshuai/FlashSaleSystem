# Flash Sale System

基于 `Java 17 + Spring Boot 3 + MyBatis-Plus + MySQL 8 + Redis + Kafka` 的分布式秒杀系统课程作业工程。

当前版本已经完成：
- Phase 0：修复测试基线，调整 MyBatis 扫描边界，补齐订单/库存持久层骨架
- Phase 1：落地单体版秒杀核心链路，支持 Redis 预扣、Kafka 异步下单、幂等兜底、Snowflake 订单 ID、结果查询
- Phase 2：补齐课程演示所需的 Docker、docker-compose、Nginx 静态页与 API 反向代理、读写分离骨架和压测说明

## 核心能力

- 用户注册、登录、JWT 鉴权
- 商品详情缓存，具备缓存穿透、击穿、雪崩防护
- 秒杀接口 `POST /api/seckill/{productId}`
- 秒杀结果查询 `GET /api/seckill/result?productId=1`
- 订单查询 `GET /api/orders/{orderId}`
- Redis Lua 原子判重与预扣库存
- Kafka 异步削峰，消费者落库订单与库存
- MySQL 唯一索引 `uk_user_product` 做最终幂等兜底
- `AbstractRoutingDataSource + @ReadOnlyRoute` 读写分离骨架

## 目录结构

```text
src/main/java/com/flashsale
├── common        通用返回模型
├── config        MyBatis、数据源、读写路由、Web 配置
├── controller    用户、商品、秒杀、订单接口
├── dto           登录、秒杀、订单相关 DTO
├── entity        user/product/inventory/order_info 实体
├── exception     业务异常
├── handler       全局异常处理
├── interceptor   JWT 鉴权拦截器
├── mapper        MyBatis-Plus Mapper
├── properties    JWT、Kafka 自定义配置
├── service       业务接口
├── service/impl  用户、商品、库存、订单、秒杀实现
└── util          JWT、密码、Redis Key 等工具类
```

## 数据库准备

1. 启动 MySQL 8、Redis、Kafka
2. 创建数据库：

```sql
CREATE DATABASE flash_sale_system DEFAULT CHARACTER SET utf8mb4;
```

3. 项目启动时会执行 [schema.sql](src/main/resources/schema.sql)
4. 初始化脚本会自动插入一条演示商品：
   - 商品 ID：`1`
   - 商品名：`限量耳机`
   - 初始库存：`20`

## 本地启动

```bash
mvn spring-boot:run
```

或：

```bash
mvn clean package
java -jar target/flash-sale-system-0.0.1-SNAPSHOT.jar
```

## Docker 演示环境

项目根目录已提供：
- [Dockerfile](Dockerfile)
- [docker-compose.yml](docker-compose.yml)
- [Nginx 配置](deploy/nginx/default.conf)
- [静态演示页](deploy/nginx/html/index.html)

启动方式：

```bash
docker-compose up --build
```

启动后默认入口：
- Nginx 演示页：`http://localhost`
- 应用实例 1：`http://localhost:8080`
- MySQL 主库：`localhost:3306`
- MySQL 从库：`localhost:3307`
- Redis：`localhost:6379`
- Kafka：`localhost:9092`

## 主要接口

### 1. 用户注册

`POST /api/users/register`

```json
{
  "username": "test1",
  "password": "123456"
}
```

### 2. 用户登录

`POST /api/users/login`

```json
{
  "username": "test1",
  "password": "123456"
}
```

登录成功后，把返回的 token 放入请求头：

```text
Authorization: Bearer <token>
```

### 3. 商品详情

`GET /api/products/1`

### 4. 发起秒杀

`POST /api/seckill/1`

可能返回：
- `排队中`
- `已售罄`
- `请勿重复秒杀`

### 5. 查询秒杀结果

`GET /api/seckill/result?productId=1`

可能状态：
- `QUEUING`
- `SUCCESS`
- `FAIL`
- `EMPTY`

### 6. 查询订单

`GET /api/orders/{orderId}`

## 关键实现说明

### 秒杀写链路

1. 用户鉴权后请求秒杀接口
2. Redis Lua 脚本原子执行判重和库存预扣
3. 成功后立即返回“排队中”，并发送 Kafka 消息
4. Kafka 消费者异步扣减 MySQL 库存、创建订单
5. Redis 写入秒杀结果，前端轮询获取最终状态

### 幂等与一致性

- Redis 集合防止同用户重复抢同商品
- MySQL `uk_user_product(user_id, product_id)` 做最终兜底
- Kafka 发送失败时回补 Redis 库存和用户占位
- 消费失败时写失败结果并回补 Redis 预扣

### 读写分离

- 查询接口通过 `@ReadOnlyRoute` 默认路由到从库
- 写请求默认走主库
- 当前默认配置里主从都指向同一库，便于本地开发；部署时通过环境变量切换

## 测试

执行：

```bash
mvn test
```

当前已验证：
- `UserControllerTest`
- `SnowflakeIdGeneratorTest`

## 压测说明

压测建议见 [docs/jmeter-guide.md](docs/jmeter-guide.md)。

## 后续演进

当前仓库已经完成课程作业版单体闭环和演示部署。下一步如果要继续推进，可以把订单与库存拆成独立微服务，并把 Kafka 补偿、死信队列和更完整的最终一致性机制继续细化。
