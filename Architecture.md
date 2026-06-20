# HRSphere Architecture

## Overview

HRSphere is an **AI-powered Human Resource Management System** built using **Java + Spring Boot Microservices Architecture**.

### Key Highlights

* Microservices Architecture
* API Gateway Pattern
* Event-Driven Communication
* Database-per-Service
* Redis Pub/Sub
* Monitoring with Prometheus & Grafana
* AI-Powered Analytics

---

# Tech Stack

| Layer      | Technologies                       |
| ---------- | ---------------------------------- |
| Backend    | Java, Spring Boot, Spring Security |
| Gateway    | Spring Cloud Gateway, Nginx        |
| Database   | PostgreSQL                         |
| Messaging  | Redis Pub/Sub                      |
| Monitoring | Prometheus, Grafana                |
| AI         | Spring AI                          |

---

# Architecture Overview

> Add Diagram 1 here

```md
![Architecture Overview](High_level_archi.png)
```

### Request Flow

```text
Client → Nginx → API Gateway → Services
                           ↓
                  PostgreSQL + Redis
```

---

# Core Services

| Service              | Responsibility         |
| -------------------- | ---------------------- |
| Auth Service         | Authentication & JWT   |
| Employee Service     | Employee Management    |
| Department Service   | Department Management  |
| Attendance Service   | Attendance Tracking    |
| Leave Service        | Leave Management       |
| Payroll Service      | Salary & Payslips      |
| Project Service      | Project Assignment     |
| Workforce Service    | Capacity Planning      |
| Notification Service | Alerts & Notifications |
| Monitoring Service   | System Health          |
| AI Analytics Service | HR Insights            |

---

# Communication Model

HRSphere uses **Hybrid Communication**.

### Synchronous

* Client → Gateway → Services
* REST APIs

### Asynchronous

* Service → Redis → Service
* Event-driven communication

Example Events:

* `user.created`
* `employee.onboarded`
* `leave.approved`
* `attendance.summary`

---

# Deployment Architecture

> Add Diagram 2 here

```md
![Deployment Diagram](deployment.png)
```

### Deployment Layers

* Gateway Layer
* Application Layer
* Data Layer
* Observability Layer

### Networks

* `gateway-net`
* `backend-net`
* `data-net`

---

# Redis Usage

Redis is used for:

* Event Bus
* Caching
* Rate Limiting
* Distributed Locks
* Refresh Tokens

---

# Monitoring

Monitoring stack:

* Spring Boot Actuator
* Prometheus
* Grafana

Tracks:

* Service Health
* Request Latency
* DB Health
* Redis Health

---

# Future Scope

* Kafka Integration
* Kubernetes Deployment
* Distributed Tracing
* Centralized Logging
* CI/CD Pipelines

---

## Conclusion

HRSphere demonstrates enterprise-grade backend architecture with focus on:

* Scalability
* Reliability
* Security
* Observability
* Distributed Systems
