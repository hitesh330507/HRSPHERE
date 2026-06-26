# HRSphere

Running the Project
-------------------

Prerequisites:
- Docker (Desktop or Engine) and `docker compose` available on PATH
- Java 21 and Maven, or use the included Maven Wrapper (`./mvnw`)

Quick start (fresh clone):

1. Copy the example env and edit secrets locally:

```bash
cp .env.example .env
# Edit .env and set secure passwords before continuing
```

2. Start the full stack (single command):

```bash
docker compose up -d
```

3. (Optional) Apply database provisioning records if needed:

```bash
./infra/postgres/apply-databases.sh
```

Notes: `apply-databases.sh` is idempotent and can be re-run safely; it executes `psql` inside the running `postgres` container and skips existing databases.

4. Verify build (from repo root):

```bash
./mvnw clean install
```

Health check examples (replace 8000 with `NGINX_PORT` from `.env` if different):

```bash
# A) Gateway health through Nginx
curl -s localhost:8000/actuator/health | jq .

# B) Full chain: Nginx → Gateway → auth-service
curl -s localhost:8000/api/v1/auth/actuator/health | jq .

# C) Direct auth-service health (bypass gateway)
curl -s localhost:8081/actuator/health | jq .
```

If you need to stop and remove volumes for a cold start:

```bash
docker compose down -v
```

For more infra details see `docs/infrastructure.md` and routing in `docs/gateway-routes.md`.
