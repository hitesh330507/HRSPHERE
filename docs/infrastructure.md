# HRSphere Local Infrastructure

## Overview

This repository contains Day 1 local infrastructure for HRSphere.

- `postgres` provides the shared PostgreSQL instance for future service databases.
- `redis` provides caching, distributed locks, rate limiting, refresh token storage, and an event bus/pub-sub backbone.

Today only infrastructure is configured; no Spring Boot services or API gateway are included yet.

## Network design

HRSphere uses three bridge networks:

- `gateway-net` — reserved for Nginx / API gateway in later phases.
- `backend-net` — reserved for Spring Boot microservices and inter-service traffic.
- `data-net` — used today for Postgres and Redis only.

The Compose file declares all three networks so future services can join them without editing the network definitions later.

## Services

### PostgreSQL

- Image: `postgres:16-alpine`
- Container: `hrsphere-postgres`
- Data volume: `hrsphere_pg_data`
- Attached to `data-net`
- Environment from `.env`
- Healthcheck: `pg_isready`
- Restart policy: `unless-stopped`

This instance is intentionally single-node for Day 1. When microservices are added later, each service will create its own database in this same Postgres instance, such as `CREATE DATABASE auth_db;`, rather than starting a new Postgres container per service.

### Redis

- Image: `redis:7-alpine`
- Container: `hrsphere-redis`
- Data volume: `hrsphere_redis_data`
- Attached to `data-net`
- Auth required via `REDIS_PASSWORD`
- Persistence enabled with `appendonly yes` (AOF)
- Healthcheck: `redis-cli -a <password> ping`
- Restart policy: `unless-stopped`

Redis is configured for persistence so cached data, locks, and refresh tokens survive restarts. Note that pub/sub traffic is still fire-and-forget by nature; AOF persists stored keys and state, not in-flight pub/sub messages.

## Start / stop

Start the stack:

```bash
docker compose up -d
```

Stop the stack:

```bash
docker compose down
```

Start the optional admin tools:

```bash
docker compose --profile tools up -d
```

## Verify health

Check container status:

```bash
docker compose ps
```

Manual verification:

```bash
docker compose exec postgres pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB"
```

```bash
docker compose exec postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT 1;"
```

```bash
docker compose exec redis redis-cli -a "$REDIS_PASSWORD" ping
```

## Adding a new service database

The canonical workflow for service databases is:

1. Append a new `CREATE DATABASE` statement and a comment to `infra/postgres/databases.sql`.
2. Run `./infra/postgres/apply-databases.sh`.
3. Do not create databases manually in `psql` or elsewhere.

This keeps database provisioning versioned, repeatable, and auditable. The script is safe to re-run: existing databases emit a benign "already exists" warning and the script continues, while newly-added databases are created.

## Defaults and credentials

Default ports:

- Postgres: host `5432` -> container `5432`
- Redis: host `6379` -> container `6379`

Credentials live in `.env`, which is ignored by Git.

## Notes

- No Spring Boot code was changed or added for Day 1.
- `gateway-net` and `backend-net` are currently empty and reserved for Day 2+.
- Future microservices will create databases inside this shared Postgres instance rather than starting new Postgres containers.
