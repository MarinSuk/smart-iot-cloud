# SmartIoT-Cloud: Intelligent Open Gateway & Device Cloud Platform

An Internet of Things (IoT) connected-vehicle cloud service and third-party OpenAPI gateway built with **JDK 17 + Spring Boot 3**. Adopting a multi-datasource isolation architecture, it achieves a complete closed loop of device ingestion, third-party API governance, and enterprise admin operations.

## Core Features

* **Multi-Datasource Isolation**: Implements physical and logical isolation across MySQL (`master` primary DB and `iot` device DB), Redis (authentication instance and high-frequency business instance), and RabbitMQ (`/inner` internal queues and `/device` telemetry ingestion queues).
* **Dual-Track Security Authentication**: Secures the admin portal with `Bearer Token + Redis` sliding expiration, while protecting third-party OpenAPIs via `AppId + Timestamp + Nonce (anti-replay) + HMAC-SHA256` signature verification.
* **High-Concurrency Quota & Cache Defense**: Enforces atomic API quota deduction using `Redis + Lua` scripts; mitigates cache penetration, breakdown, and avalanche through null placeholders, distributed mutex locks (DCL), and randomized TTL jitter.
* **Reliable Messaging & DLX Delayed Timeout**: Ensures idempotent device track ingestion via `Confirm/Return` callbacks, manual `ACK`, and dual-layer idempotency (`Redis business key + DB unique index`); handles 10-second 4G remote command expiration using `Queue TTL + Dead Letter Exchange (DLX)`.
* **OpenAPI-Driven Frontend Generation**: Integrates Knife4j to export standard OpenAPI 3.0 specifications, enabling AI coding assistants to automatically generate Vue 3 admin interfaces.

## Tech Stack

* **Core Framework**: JDK 17, Spring Boot 3.2.5
* **ORM & Multi-Source**: MyBatis-Plus 3.5.6, Dynamic-Datasource 4.3.0
* **Storage & Middleware**: MySQL 8.0, Redis 7.x (Multi-Lettuce Connection Factories), RabbitMQ (Multi-ConnectionFactory + DLX)
* **API Docs & Utilities**: Knife4j (OpenAPI 3), Hutool, Lombok

## Environment Requirements

* **Build & Runtime**: JDK 17+, Maven 3.8+
* **Database**: MySQL 8.0 (requires two databases: `iot_master` and `iot_device_data`)
* **Cache**: Redis (requires `DB 0` and `DB 1` enabled)
* **Message Broker**: RabbitMQ (requires two Virtual Hosts: `/inner` and `/device`)