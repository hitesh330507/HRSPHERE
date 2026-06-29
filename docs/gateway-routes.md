# API Gateway Routes

This document tracks every route configured in `api-gateway/src/main/resources/application.yml`.

| Route ID | Path Pattern | Target Service | Date Added |
|----------|--------------|----------------|------------|
| `auth-service-route` | `/api/v1/auth/**` | `auth-service` | 2026-06-21 |

> Convention: `/api/v1/{service-name}/**` is used for service routing. This is the current route pattern for future gateway routes.

Route details: `auth-service-route` now uses `StripPrefix=2` so `/api/v1/auth/register` forwards to `/auth/register` in the auth-service.
