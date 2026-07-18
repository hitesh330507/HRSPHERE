# HRSphere — Interview Talking Points & Prep Guide

This document contains key talking points, Q&As, and architecture walk-throughs designed for interview preparation based on what was actually built during the 10-day sprint of **HRSphere**.

---

## 1. "Walk me through the architecture"

### 30-Second Elevator Pitch
> **Answer:** "HRSphere is an event-driven HR management system built on a Java and Spring Boot microservices architecture. It consists of 5 core services: an API Gateway, an Authentication service using JWT and Redis refresh tokens, and domain services for Employees, Departments, and Leaves. All traffic enters through an Nginx reverse proxy to a Spring Cloud Gateway which handles rate limiting, stateless JWT validation, and Resilience4j circuit breaking before routing to downstream services. The data layer enforces database-per-service using logical Postgres schemas, and services communicate asynchronously via Redis Pub/Sub for events like user creation and leave approval, and synchronously via Resilience4j-wrapped REST endpoints for peer lookups."

### 2-Minute Deep-Dive
> **Answer:** "If we zoom in on the system, it's organized into four distinct layers: Ingress, Gateway, Application, and Data.
> 
> 1. **Ingress & Gateway Layer:** Client requests hit Nginx on port 8000, which routes to a Spring Cloud Gateway on port 8080. The Gateway acts as the security and resilience guard. It validates JWT tokens statelessly using a shared secret. If valid, it injects trusted headers (`X-Auth-Username`, `X-Auth-Roles`, and `X-Auth-Validated`) and routes the request. It also uses Redis to enforce rate limiting and hosts Resilience4j circuit breakers with custom fallback bodies in case downstream services fail.
> 2. **Application Layer:** We have 4 backend services: `auth-service` (8081), `employee-service` (8082), `department-service` (8083), and `leave-service` (8084). These services do not re-validate JWTs; they trust the Gateway headers. They communicate with each other synchronously using `RestTemplate` wrapped in Resilience4j circuit breakers for peer lookup validations.
> 3. **Data Layer:** We enforce strict database isolation matching the database-per-service pattern. All services share a single PostgreSQL container (port 5432) but connect to isolated databases (`auth_db`, `employee_db`, `department_db`, `leave_db`) using separate credentials. Migrations are managed independently in each service codebase using Flyway.
> 4. **Asynchronous Event Backbone:** We use Redis Pub/Sub for async decoupling. For instance, when a user registers, `auth-service` publishes a `user.created` event, which the `employee-service` consumes idempotently to provision a default profile.
> 5. **Observability Loop:** All services expose Micrometer Prometheus metrics via Spring Boot Actuator. A Prometheus container scrapes these metrics, which are visualized on an auto-provisioned Grafana dashboard."

---

## 2. "Why microservices instead of a monolith for this?"

> **Answer:** "In a real-world enterprise, HR systems have highly divergent scalability and security profiles:
> - **Authentication & Profiles** are high-read, critical-path modules that need to be highly available and lightweight.
> - **Leave Management** involves complex stateful transitions and workflow approvals that are write-heavy during specific periods (e.g., end of the year or quarters) but relatively idle otherwise.
> - **Security Isolation:** Payroll (which would be added in a full build) contains highly sensitive salary data and needs much tighter security boundaries and access controls than general department lists.
> 
> Monoliths make it difficult to scale these modules independently or restrict database-level access to sensitive tables. With microservices, we enforce strict domain boundaries at the database level (`database-per-service`), scale services independently, and deploy updates to the leave workflow without risking auth uptime. While microservices introduce operational complexity (like network latency, distributed transactions, and partial failures), we mitigated these using an API Gateway, local JWT verification, and circuit breakers."

---

## 3. "Walk me through what happens when someone applies for leave"

> **Answer:** "The request follows a clean lifecycle from the client to the database and event bus:
> 
> 1. **Client Request:** An employee sends a `POST /api/v1/leave/requests` request with the `leaveTypeId`, `startDate`, `endDate`, and `reason` in the JSON body, carrying their JWT access token in the `Authorization` header.
> 2. **Gateway Ingress:** Nginx routes the request to the API Gateway. The Gateway's rate limiter checks the user's IP/username against the Redis store. If allowed, the `JwtAuthenticationFilter` validates the token signature statelessly.
> 3. **Header Propagation:** The Gateway injects the user's role (`ROLE_EMPLOYEE`) and username (`john_doe`) into the downstream headers (`X-Auth-Username`, `X-Auth-Roles`) and forwards the request to the `leave-service`.
> 4. **Cross-Service Verification:** The `leave-service` needs to verify that the employee exists and is active. It performs a synchronous REST call to `employee-service/employees/lookup?authUsername=john_doe`. This call is wrapped in a Resilience4j circuit breaker.
> 5. **Business Logic & Persistence:** Once the employee is validated, the `leave-service` checks if the employee has sufficient balance for the selected leave type. If valid, it deducts the pending days, creates a `LeaveRequest` entity with `PENDING` status, and persists it to `leave_db` via Flyway-managed schema.
> 6. **Approval & Event Publishing:** Later, an HR manager approves the request via `PATCH /api/v1/leave/requests/{id}/review` (carrying `ROLE_HR`). The `leave-service` transitions the request status to `APPROVED`, saves it, and publishes a `leave.approved` event containing the employee ID, leave type, and duration to the Redis `leave-approved-channel` event bus for asynchronous downstream consumption (like payroll calculations or notifications)."

---

## 4. "What happens if a service goes down?"
*(This is a key answer demonstrating nuanced engineering decisions)*

> **Answer:** "We implemented two different resilience strategies based on whether the dependency is **hard** or **soft**:
> 
> - **Soft Dependency (Department Validation during Employee Creation):** When creating an employee, we query the `department-service` to verify the department ID. If the `department-service` is down, the Resilience4j circuit breaker trips and executes a fallback that degrades gracefully: it logs the failure and bypasses validation, allowing the employee creation to proceed rather than failing the transaction. We prioritized employee onboarding completion over strict, real-time validation of department categories.
> - **Hard Dependency (Employee Lookup during Leave Application):** When applying for leave, the `leave-service` must lookup the employee's ID. If the `employee-service` is down, we cannot proceed because we cannot map the leave record to a non-existent employee. The circuit breaker trips and the fallback throws a `ServiceUnavailableException`, returning an HTTP 503 error to the client. This prevents data corruption or orphan leave requests.
> - **Gateway Level Protection:** If any service fails entirely, the API Gateway's Resilience4j circuit breaker handles the timeout and immediately returns a structured JSON `ApiErrorResponse` (`503 Service Unavailable`) detailing which service is offline, preventing thread pool exhaustion at the gateway."

---

## 5. "How do you handle authentication across services?"

> **Answer:** "We use **Gateway-level JWT validation with Downstream Header Trust**. 
> - The client authenticates with the `auth-service` and receives a short-lived Access Token (15 min) and a Redis-backed Refresh Token (7 days).
> - For subsequent requests, the API Gateway intercepts the request and validates the JWT signature and expiration locally using a shared `JWT_SECRET`. We chose local validation to avoid a synchronous network hop to `auth-service` for every single API request, which would create a severe bottleneck and single point of failure.
> - If valid, the Gateway injects three headers: `X-Auth-Username`, `X-Auth-Roles`, and `X-Auth-Validated: true`.
> - Downstream services trust these headers completely. We configure Spring Security in downstream services to permit all traffic but install a filter that parses these headers to build the SecurityContext. To prevent spoofing, downstream services can be configured with `REQUIRE_TRUST_HEADERS=true`, which rejects any incoming requests that do not originate from the Gateway's internal IP subnet."

---

## 6. "Tell me about your testing approach"

> **Answer:** "We followed a strict test pyramid combining unit and integration tests, totaling **117 test cases** across **34 test files**:
> 
> 1. **Unit Tests:** Focus on mapping logic (MapStruct), validation constraints, utility methods, and business services using Mockito to mock repositories.
> 2. **Integration Tests (Testcontainers + WireMock):** We did not use in-memory databases like H2 because their syntax diverges from PostgreSQL (especially regarding JSON fields, UUIDs, and transactions). Instead, we used **Testcontainers** to boot up real Docker containers for PostgreSQL and Redis during the Maven test phase.
> 3. **Outage Simulation:** In `LeaveLifecycleIntegrationTest`, we used **WireMock** to stub the `employee-service` endpoint. This allowed us to write tests that mock a slow response (timeout) or a `500 Internal Server Error` from the employee service to verify that the Resilience4j circuit breaker trips and returns a `503 Service Unavailable` as expected.
> 4. **Asynchronous Verification:** We used **Awaitility** to block and assert that when a leave request is approved, the `leave.approved` event is published to the Redis channel within a 2-second window."

---

## 7. "What would you do differently with more time?"
*(Frame this as architectural foresight, not a list of weaknesses)*

> **Answer:** "Given more time, I would focus on three specific architectural enhancements:
> 
> 1. **Distributed Message Broker (Kafka):** While Redis Pub/Sub is excellent for lightweight local development, it is fire-and-forget and lacks persistence. In production, I would migrate to Apache Kafka to guarantee at-least-once delivery, enable event replayability, and support partition-based scaling for consumers.
> 2. **Distributed Tracing (Sleuth/Brave + Zipkin):** Currently, correlation IDs are injected at the gateway and logged. While grepping log files works for 5 services, in a large system it becomes unmanageable. I would integrate Micrometer Tracing to automatically propagate span and trace IDs and push them to a centralized collector like Zipkin or Jaeger.
> 3. **API Composition / BFF (Backend For Frontend):** Currently, the frontend would have to make multiple REST calls or handle cross-service joins. I would implement a GraphQL or Spring Cloud Gateway-based aggregation layer to handle API composition (e.g., joining Employee and Leave data into a single response) to reduce client-side round-trips."

---

## 8. Numbers to Memorize Cold

- **Microservices count:** 5 (`auth-service`, `employee-service`, `department-service`, `leave-service`, `api-gateway`)
- **Containers in Compose:** 9 (`nginx`, `api-gateway`, `auth-service`, `employee-service`, `department-service`, `leave-service`, `postgres`, `redis`, `prometheus`, `grafana`)
- **REST Endpoints:** 34 total across the 5 services
- **Total Test Cases:** 117 tests (run via `./mvnw clean install` in ~2 mins)
- **Shared Ports:**
  - Nginx Entrypoint: `8000`
  - API Gateway: `8080`
  - Auth Service: `8081`
  - Employee Service: `8082`
  - Department Service: `8083`
  - Leave Service: `8084`
  - PostgreSQL: `5432`
  - Redis: `6379`
  - Prometheus: `9090`
  - Grafana Dashboard: `3001`
