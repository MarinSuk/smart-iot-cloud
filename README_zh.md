# SmartIoT-Cloud 智联开放网关与设备云平台

基于 **JDK 17 + Spring Boot 3** 构建的物联网（IoT）车联网云服务与第三方开放平台网关，采用多数据源隔离架构，实现设备接入、第三方接口管控与后台运维闭环。

## 核心功能

* **多数据源隔离**：实现 MySQL（`master` 主库与 `iot` 设备库）、Redis（鉴权库与业务高频库）、RabbitMQ（`/inner` 内部队列与 `/device` 设备上报队列）的物理与逻辑隔离。
* **双轨安全鉴权**：后台管理端采用 `Bearer Token + Redis` 滑动续期校验；第三方开放平台采用 `AppId + Timestamp + Nonce（防重放） + HMAC-SHA256` 签名校验。
* **高并发配额与缓存防护**：基于 `Redis + Lua` 脚本实现接口调用配额原子扣减；综合运用空值占位、分布式互斥锁（DCL）与随机过期时间解决缓存穿透、击穿与雪崩。
* **高可靠消息与死信延迟**：设备轨迹上报采用 `Confirm/Return` 机制、手动 `ACK` 及“Redis 业务键 + 数据库唯一索引”双重幂等落库；4G 远控指令基于 `队列 TTL + 死信队列（DLX）` 实现 10 秒超时自动失效。
* **OpenAPI 驱动前端生成**：集成 Knife4j 输出标准 OpenAPI 3.0 协议文档，支持结合 AI 编程助手自动生成 Vue 3 前端管理界面。

## 技术栈

* **核心框架**：JDK 17、Spring Boot 3.2.5
* **数据持久层**：MyBatis-Plus 3.5.6、Dynamic-Datasource 4.3.0
* **存储与中间件**：MySQL 8.0、Redis 7.x（多 Lettuce 连接工厂）、RabbitMQ（多 ConnectionFactory + DLX）
* **文档与工具**：Knife4j (OpenAPI 3)、Hutool、Lombok

## 运行环境

* **基础编译环境**：JDK 17+、Maven 3.8+
* **数据库**：MySQL 8.0（需创建 `iot_master` 与 `iot_device_data` 两个库）
* **缓存服务**：Redis（需启用 `DB 0` 与 `DB 1`）
* **消息队列**：RabbitMQ（需创建 `/inner` 与 `/device` 两个 Virtual Host）