# HRSphere — Testing Strategy

## Test pyramid applied to HRSphere
- **Unit tests**: Service layer logic in isolation, mocked repositories, and mocked inter-service HTTP calls. Fast, run on every build. Cover business rule edge cases (validation, state transitions, calculations).
- **Integration tests**: Full Spring context + real Postgres and Redis via Testcontainers. Cover the full request lifecycle — HTTP → controller → service → repository → real database — for the critical paths of each service. Slower (~10-20s startup per test class due to container spin-up), run less frequently but before every merge/tag.
- **What's NOT covered**: Load/performance testing, contract testing between services (e.g. Pact), full end-to-end tests through the actual gateway/nginx (covered manually via the curl smoke tests documented throughout Days 1-15). This is a deliberate scope boundary for a portfolio project — name it explicitly rather than pretending full coverage exists.

## Cross-service call testing approach
Employee-service, department-service, and leave-service call each other over HTTP. In integration tests, these calls are NOT made against a real running peer service — that would turn a unit-of-work integration test into an unreliable, slow, order-dependent test suite. Instead:
- Use WireMock to simulate the peer service's HTTP responses, including failure scenarios (404, timeout, connection refused).
- This tests "does my service handle department-service saying X correctly" without requiring department-service to actually be running.
- WireMock is the industry-standard choice (actual HTTP calls to a mock server), and is preferred over manual RestTemplate/RestClient bean mocking for realistic transport-level verification.

## Per-service test coverage table
| Service | Unit tests | Integration tests | Cross-service mocking |
|---|---|---|---|
| auth-service | Day 7-10 | Day 10 (Testcontainers) | N/A |
| employee-service | Day 11-12 | Day 16 (Testcontainers) | department-service via WireMock |
| department-service | Day 13 | Day 16 (Testcontainers) | employee-service via WireMock |
| leave-service | Day 15 | Day 17 (Testcontainers) | employee-service via WireMock |
