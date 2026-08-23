# Graduation Project Microservice Scaffold

This repository is a reusable full-stack foundation for a new graduation-project topic. Topic-specific business code and documents from the previous topic have been removed. Future work should continue on the `dev` branch.

## Retained foundation

- Backend: Java 21, Spring Boot 4.0.6, Spring Cloud 2025.1.1 and Spring Cloud Alibaba 2025.1.0.0
- Infrastructure: Nacos, Redis, PostgreSQL, RabbitMQ and Alibaba Cloud OSS
- Services: `gateway`, `user-service`, `file-service`, `notification-service` and `audit-service`
- Shared modules: core, web, security, data, Redis, OpenAPI, observability and messaging
- Authentication: Sa-Token with Redis and gateway-propagated user context
- Data access: MyBatis-Plus with PostgreSQL
- Frontend: development is paused; only the Vue 3 management scaffold remains
- Local environment: Docker Compose middleware stack

## Current backend capabilities

- `/api/user/**`: authentication, accounts, user administration and RBAC
- `/api/files/**`: private files stored in Alibaba Cloud OSS
- `/api/notifications/**`: in-app notifications and unread state
- `/api/audits/**`: personal operation audit queries
- Transactional outbox, publisher confirms, idempotent consumers, retries and dead-letter queues

See `back/README.md` and `项目文档/README.md` for architecture, configuration, API and deployment details. Add topic-specific services instead of placing domain data in the user service.
