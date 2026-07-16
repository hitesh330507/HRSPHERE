# HRSphere Observability Pipeline

This document details the architecture, design, metrics, and dashboards for the HRSphere microservices observability pipeline introduced in Day 19.

## Overview

The HRSphere observability pipeline is built using:
- **Spring Boot Actuator**: Native endpoints to expose health, info, and application metrics.
- **Micrometer**: An application metrics facade, configured to use the Prometheus registry.
- **Prometheus**: Metrics collection server configured to scrape all 5 services at regular intervals.
- **Grafana**: Visualization platform with auto-provisioned dashboards and datasources.

```mermaid
graph LR
    subgraph Spring Boot Microservices
        Auth[auth-service] -->|Actuator| P1[/actuator/prometheus]
        Emp[employee-service] -->|Actuator| P2[/actuator/prometheus]
        Dept[department-service] -->|Actuator| P3[/actuator/prometheus]
        Leave[leave-service] -->|Actuator| P4[/actuator/prometheus]
        Gate[api-gateway] -->|Actuator| P5[/actuator/prometheus]
    end

    subgraph Monitoring Infrastructure
        Prometheus[(Prometheus)]
        Grafana[Grafana Dashboard]
    end

    Prometheus -->|Scrapes every 15s| P1
    Prometheus -->|Scrapes every 15s| P2
    Prometheus -->|Scrapes every 15s| P3
    Prometheus -->|Scrapes every 15s| P4
    Prometheus -->|Scrapes every 15s| P5

    Grafana -->|Queries| Prometheus
```

---

## Infrastructure Ports

The following ports are exposed on the host machine in the development environment:

| Service | Host Port | Internal Port | Access URL |
| :--- | :--- | :--- | :--- |
| **Prometheus** | `9090` | `9090` | `http://localhost:9090` |
| **Grafana** | `3001` | `3000` | `http://localhost:3001` |

> [!NOTE]
> Grafana is configured with auto-provisioning. The default admin credentials are set in the `.env` file (default: `admin` / `admin`).

---

## Enabled Actuator Endpoints

All Spring Boot microservices have the following actuator endpoints enabled and exposed via HTTP web interface:
- `/actuator/health`: System health status (including db connection, redis connectivity, and circuit breaker status)
- `/actuator/info`: Application information
- `/actuator/metrics`: Raw list of metrics
- `/actuator/prometheus`: Scrape endpoint formatted for Prometheus ingestion

---

## Custom Business Metrics

To track business value and application performance, we have instrumented custom metrics:

### 1. Leave Service
- `leave_requests_applied_total` (Counter): Cumulative count of leave applications, tagged by `leaveType`.
- `leave_requests_approved_total` (Counter): Cumulative count of leave requests approved, tagged by `leaveType`.
- `leave_requests_rejected_total` (Counter): Cumulative count of leave requests rejected, tagged by `leaveType`.
- `leave_request_review_duration_seconds` (Timer): Duration from leave request application to reviewer decision.

### 2. Employee Service
- `employees_active_total` (Gauge): Current count of active employees in the system (excludes terminated/deleted records).
- `employees_created_total` (Counter): Cumulative count of new employees created.
- `employees_terminated_total` (Counter): Cumulative count of employees terminated.

### 3. Authentication Service
- `auth_login_success_total` (Counter): Cumulative count of successful login attempts.
- `auth_login_failure_total` (Counter): Cumulative count of failed login attempts.

---

## Grafana Dashboard: "HRSphere Overview"

The **HRSphere Overview** dashboard is auto-provisioned in Grafana at startup. It consists of the following rows and panels:

### Row 1: System Traffic & Latency
- **HTTP Request Rate**: Tracks request throughput (requests/sec) per microservice.
- **HTTP Latency (p95)**: Tracks 95th percentile request latencies using percentile-histograms.

### Row 2: Service Health & Resources
- **System CPU Usage**: Tracks host-level CPU utilization per service.
- **JVM Heap Memory Usage**: In-depth monitoring of memory allocation and garbage collection impact.
- **HikariCP Connection Pools**: Shows active and idle database connections to preempt database bottlenecks.

### Row 3: Circuit Breakers
- **Circuit Breaker State Panel**: A unified stat grid displaying the state of API Gateway circuit breakers (0 = Closed, 1 = Open, 2 = Half-Open).

### Row 4: Business Performance Metrics
- **Leave Requests (Applied vs Approved vs Rejected)**: Multi-series line chart monitoring leave request volume.
- **Leave Request Review Duration**: Average and max duration of request processing.
- **Active Employees (Current Headcount)**: A big-number gauge representing current staff count.
- **Employee Lifecycle Actions**: Chart showing employee creations and terminations.
- **Authentication Activity**: Tracks successful vs. failed logins to detect credential stuffing attacks.

---

## How to Verify locally

1. Run the stack:
   ```bash
   docker compose up -d --build
   ```
2. Run database migration checks:
   ```bash
   ./infra/postgres/apply-databases.sh
   ```
3. Generate load:
   ```bash
   ./infra/scripts/generate-demo-traffic.sh
   ```
4. Access Grafana at `http://localhost:3001` (admin / admin), click on "Dashboards" in the sidebar, select the "HRSphere Overview" dashboard, and verify metrics are recording in real time.
