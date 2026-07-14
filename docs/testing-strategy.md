# HRSphere — Testing Strategy

This document outlines the testing architecture, strategies, and coverage matrix across the HRSphere microservice platform.

---

## 1. Testing Architecture & Principles

We employ a strict **Testing Pyramid** approach to ensure high confidence, fast execution times, and reliable builds.

```
          / \
         /   \     Integration Tests (Spring Context + Testcontainers)
        /     \    Verifies web request flow, DB interactions, Pub/Sub.
       /-------\
      /         \  Unit Tests (JUnit 5 + Mockito)
     /           \ Verifies domain/service logic, validation, edge cases.
    /_____________\
```

### 1.1 Scope Boundaries
*   **Unit Tests**: Execute in milliseconds in complete isolation using Mockito for database repositories and outgoing HTTP client components. Focus on validation constraints, calculations, and conditional state machines.
*   **Integration Tests**: Run in full Spring Boot container contexts with real PostgreSQL and Redis databases managed via **Testcontainers**. Focus on database persistence, transactional boundaries, API endpoints, serialization/deserialization, and Redis Pub/Sub events.
*   **Cross-Service Boundaries**: Managed via **WireMock** stubs. Services do not depend on running peer service containers during integration testing to prevent slow, flaky, or order-dependent test runs.
*   **Manual System Verification**: Performed via API Gateway routes through an automated cURL/Python smoke testing suite (see `smoke_test.py` or Day progress logs).

---

## 2. Test Execution & Docker Handling

Because headless CI environments and some developer setups may lack access to a Docker daemon or have API incompatibilities (e.g., Docker client/daemon version mismatches), we use the `@Testcontainers(disabledWithoutDocker = true)` annotation. 

*   When a valid Docker environment is present, the integration tests execute against real spun-up Postgres and Redis instances.
*   When Docker is missing or disabled, the integration tests are safely and gracefully **skipped**, allowing the codebase to build and run all unit tests without failing the Maven build.

---

## 3. Integration Patterns

### 3.1 Downstream Service Stubbing (WireMock)
To mock downstream dependencies like `employee-service` validation from `leave-service` or `department-service`, we inject a dynamic mock base URL. The application reads these peer URLs from configurable properties (e.g., `employee-service.base-url`), which are dynamically re-routed to the local WireMock port during test setup.

### 3.2 Asynchronous Event Verification (Awaitility)
To test Redis Pub/Sub event propagation without introducing flaky `Thread.sleep` calls, we use `Awaitility`. We subscribe a test-managed `RedisMessageListenerContainer` to the target event channel, capture incoming payloads in an atomic list, and use polling assertions:
```java
await().atMost(5, SECONDS).untilAsserted(() -> {
    assertThat(eventList).hasSize(1);
    assertThat(eventList.get(0).employeeId()).isEqualTo(employeeId);
});
```

---

## 4. Test Inventory & Coverage Matrix

| Service | Unit Tests | Integration Tests | Downstream Mocking | Key Coverage / Lifecycle Tested |
| :--- | :---: | :---: | :--- | :--- |
| **auth-service** | 15 | 15 | None (N/A) | User registration, login flow, token verification, JWT expiration, refresh token lifecycle, and role inheritance. |
| **employee-service** | 10 | 9 | `department-service` (WireMock) | Employee profile onboarding, MapStruct DTO mappings, profile updates (`/me`), department validation, and `user.created` event consumption. |
| **department-service** | 8 | 5 | `employee-service` (WireMock) | Department CRUD, validation of existing employees, and API gateway headers validation. |
| **leave-service** | 7 | 15 | `employee-service` (WireMock) | Leave request application, state transition validations, lazy balance allocation, balance deductions, cancellation refunds, and `leave.approved` event publishing. |

**Total Project Suites:**
*   **Unit Tests:** 40 test cases
*   **Integration Tests:** 44 test cases (totaling 84 automated test assertions)
*   **End-to-End Smoke Verification:** Automated user-lifecycle validation via API Gateway and Nginx.

---

## 5. Gateway Hardening Preparation

With Day 16 and 17 testing complete, the architecture is fully verified:
1.  **Peer-to-Peer Resiliency**: Verified downstream error fallbacks return standard JSON responses when peers are down/503.
2.  **State Consistency**: Verified transactional rollbacks and balance refunds on cancellation.
3.  **Eventing Reliability**: Verified Redis Pub/Sub publishes exact event schemas.
4.  **Network Independence**: No hardcoded internal URLs remain; all peer communication endpoints are injected via environment properties.

The platform is in a verified, stable state and ready for the **Day 18 Gateway Hardening Phase** (security enhancements, rate-limiting, and path filtering).
