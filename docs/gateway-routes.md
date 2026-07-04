# API Gateway Routes

This document tracks every route configured in `api-gateway/src/main/resources/application.yml`.

| Route ID | Path Pattern | Target Service | Date Added |
|----------|--------------|----------------|------------|
| `auth-service-route` | `/api/v1/auth/**` | `auth-service` | 2026-06-21 |

| `employee-service-route` | `/api/v1/employee/**` | `employee-service` | 2026-07-04 |

> Convention: `/api/v1/{service-name}/**` is used for service routing. This is the current route pattern for future gateway routes.

Route details: `auth-service-route` now uses `StripPrefix=2` so `/api/v1/auth/register` forwards to `/auth/register` in the auth-service.

## JWT Validation
All routes except public paths are protected by `JwtAuthenticationFilter` in the gateway.

Public paths (no JWT required):
- `/api/v1/auth/register`
- `/api/v1/auth/login`
- `/api/v1/auth/refresh`
- `/api/v1/*/actuator/**`

Protected routes: all other `/api/v1/**` requests. The gateway returns `401 Unauthorized` with `ApiErrorResponse` JSON when the JWT is missing or invalid, and the request never reaches the downstream service.

On valid JWT requests the gateway injects trusted downstream headers:
- `X-Auth-Username`: authenticated username
- `X-Auth-Roles`: comma-separated role list
- `X-Auth-Validated`: `true`
