# Architecture Decisions

**ADR-001: Three Docker networks created upfront (Day 1)**
Decision: `gateway-net`, `backend-net`, `data-net` declared in the Compose file from Day 1.
Reason: Avoids mid-project network restructuring when services join; declaring networks early prevents reconfiguring running containers later and keeps service-to-service connectivity changes minimal.

**ADR-002: Database provisioning via versioned SQL record (Day 3)**
Decision: All `CREATE DATABASE` statements live in `infra/postgres/databases.sql`, applied via `infra/postgres/apply-databases.sh`.
Reason: Ad-hoc `psql` commands are not reproducible across clones; versioned SQL records keep provisioning repeatable, auditable, and reviewable in the same commit that adds a service.

**ADR-003: Gateway routing convention `/api/v1/{service}/` (Day 4)**
Decision: All service routes follow `/api/v1/{service}/**` with `StripPrefix=3` at the gateway; routes documented in `docs/gateway-routes.md`.
Reason: A consistent URL namespace makes the API surface predictable for clients and simplifies route discovery and documentation; the gateway keeps a single entrypoint for all services.

**ADR-004: Flyway as schema owner, Hibernate set to `validate` (Day 3)**
Decision: `spring.jpa.hibernate.ddl-auto=validate`; schema changes only via Flyway migration files.
Reason: Hibernate auto-DDL can silently drift and cause production inconsistencies. Flyway ensures every schema change is versioned and reviewed.

**ADR-005: Gateway-level JWT validation with shared secret (Day 9)**
Decision: api-gateway validates JWTs locally using the shared `JWT_SECRET` rather than calling auth-service per request.
Reason: Stateless JWT validation is O(1) and does not require auth-service availability for every request. Adding an introspection call would create a synchronous dependency and latency penalty for every gateway route.
Consequence: JWT secret rotation must be coordinated across auth-service and api-gateway, and the shared secret is an explicit cross-service contract.

**ADR-006: Containerized integration testing using Testcontainers with fallback (Day 10)**
Decision: Use Testcontainers with custom setup configurations in JUnit to execute integration tests against real Postgres and Redis instances.
Reason: Emulated databases (like H2 or embedded Redis) diverge from real PostgreSQL and Redis behavior, especially regarding schema definitions and refresh token lifecycles.
Consequence: Tests require a working Docker daemon environment. We introduced soft-check fallbacks to gracefully skip containerized tests if no Docker daemon is accessible on the host machine.

**ADR-007: Metrics-based Observability Pipeline using Micrometer, Prometheus, and Grafana (Day 19)**
Decision: Establish a pull-based metrics monitoring infrastructure with custom instrumentation using Micrometer, scraping via Prometheus, and visualizing via Grafana.
Reason: Building health checks alone is insufficient for tracking microservice performance trends, connection pool depletion, or circuit breaker states. We prioritized metrics and dashboards to provide immediate visibility into system and business behaviors.
Consequence: Adds minor CPU and memory overhead to Spring Boot processes for keeping in-memory metrics, and introduces Prometheus and Grafana containers to local Docker Compose configurations.

