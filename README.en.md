# Graduation Project Microservice Scaffold

This repository is a reusable full-stack foundation for a new graduation-project topic. Topic-specific business code and documents from the previous topic have been removed. Future work should continue on the `dev` branch.

## Retained foundation

- Backend: Java 21, Spring Boot 4.0.6, Spring Cloud 2025.1.1 and Spring Cloud Alibaba 2025.1.0.0
- Infrastructure: Nacos, Redis, PostgreSQL, RabbitMQ and MinIO
- Modules: `gateway`, `user-service`, `common-core`, `common-web` and `common-redis`
- Authentication: Sa-Token with Redis and gateway-propagated user context
- Data access: MyBatis-Plus with PostgreSQL
- Frontend: Vue 3 management console and UniApp client scaffold
- Local environment: Docker Compose middleware stack

## Current APIs

- `POST /api/user/register`
- `POST /api/user/login`

After the new topic is confirmed, add a new project overview, requirements, domain model, database design and business APIs instead of reusing the previous topic's rules.
