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
