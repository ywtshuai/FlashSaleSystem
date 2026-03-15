# Flash Sale System

基于 `Java 17 + Spring Boot 3 + MyBatis-Plus + MySQL 8` 的分布式商品库存与秒杀系统初始化工程。

## 目录结构

```text
src/main/java/com/flashsale
├── common        通用返回模型
├── config        MyBatis-Plus 与 Web 配置
├── controller    RESTful 接口层
├── dto           请求与响应对象
├── entity        数据库实体
├── exception     业务异常
├── handler       全局异常处理
├── interceptor   JWT 鉴权拦截器
├── mapper        MyBatis-Plus Mapper
├── properties    自定义配置属性
├── service       业务接口
├── service/impl  业务实现
└── util          工具类
```

## 启动前准备

1. 安装并启动 MySQL 8.0。
2. 创建数据库：

```sql
CREATE DATABASE flash_sale_system DEFAULT CHARACTER SET utf8mb4;
```

3. 执行 [schema.sql](src/main/resources/schema.sql) 初始化表结构。
4. 检查 [application.yml](src/main/resources/application.yml) 中的数据库用户名、密码和 JWT 密钥配置。

## 启动项目

在项目根目录执行：

```bash
mvn spring-boot:run
```

或者先打包再运行：

```bash
mvn clean package
java -jar target/flash-sale-system-0.0.1-SNAPSHOT.jar
```

## 接口说明

### 1. 用户注册

`POST /api/users/register`

请求体：

```json
{
  "username": "test1",
  "password": "123456"
}
```

### 2. 用户登录

`POST /api/users/login`

请求体：

```json
{
  "username": "test1",
  "password": "123456"
}
```

响应体中的 `token` 为标准 JWT，后续访问受保护接口时需要放入请求头：

```text
Authorization: Bearer <token>
```

### 3. 获取当前登录用户信息

`GET /api/users/me`

请求头：

```text
Authorization: Bearer <token>
```

## 运行测试

```bash
mvn test
```

当前已包含 `JUnit 5 + MockMvc` 基础接口测试，覆盖注册、登录和受保护接口访问。
