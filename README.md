# Talabat — Food Delivery / On-Demand Delivery Platform

> A production-grade, microservices-based food ordering and delivery platform built on event-driven choreography, polyglot persistence, and full-stack observability.

![Java](https://img.shields.io/badge/Java-25-007396?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-6DB33F?logo=springboot&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)
![Kubernetes](https://img.shields.io/badge/Kubernetes-MiniKube-326CE5?logo=kubernetes&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-336791?logo=postgresql&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-7-47A248?logo=mongodb&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)
![Neo4j](https://img.shields.io/badge/Neo4j-5-008CC1?logo=neo4j&logoColor=white)
![Cassandra](https://img.shields.io/badge/Cassandra-5-1287B1?logo=apachecassandra&logoColor=white)
![Elasticsearch](https://img.shields.io/badge/Elasticsearch-8.19-005571?logo=elasticsearch&logoColor=white)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3-FF6600?logo=rabbitmq&logoColor=white)

---

## Table of Contents

- [Overview](#overview)
- [Milestone Evolution](#milestone-evolution)
- [Key Features](#key-features)
- [System Architecture](#system-architecture)
- [The Services](#the-services)
- [Tech Stack](#tech-stack)
- [Polyglot Persistence](#polyglot-persistence)
- [Inter-Service Communication](#inter-service-communication)
- [The Order Lifecycle Saga](#the-order-lifecycle-saga)
- [Design Patterns](#design-patterns)
- [Authentication & Security](#authentication--security)
- [Observability](#observability)
- [Project Structure](#project-structure)
- [Installation & Setup](#installation--setup)
- [Running the Project](#running-the-project)
- [Kubernetes Deployment](#kubernetes-deployment)
- [Feature Inventory](#feature-inventory)
- [Future Improvements](#future-improvements)
- [Contributors](#contributors)
- [License](#license)

---

## Overview

**Talabat** is a distributed food delivery system that connects customers, restaurants, and delivery riders through a fully decoupled set of microservices. It models the real-world operational complexity of a regional food-ordering marketplace — high-throughput order intake, multi-party state coordination, real-time delivery tracking, recommendation analytics, and resilient payment checkout — while remaining cleanly deployable with a single `docker compose up` or a full Kubernetes rollout.

The project tackles three core engineering problems:

1. **Coordinating long-running business transactions** (cart → checkout → payment → restaurant → delivery) across independent services without distributed locks or two-phase commit.
2. **Serving heterogeneous read/write patterns** — transactional orders, document-shaped event logs, graph-based recommendations, time-series tracking, and full-text search — by pairing each workload with the database best suited to it.
3. **Operating a polyglot distributed system in production** with first-class observability, container orchestration, and contract-driven inter-service communication.

The platform is the product of a three-milestone build, each milestone adding a distinct architectural layer on top of the previous one.

---

## Milestone Evolution

The system was built incrementally. Understanding the three milestones explains why the codebase is shaped the way it is.

| Milestone | Theme | What it added |
|-----------|-------|---------------|
| **M1** | Core services foundation | 5 Spring Boot services as a Maven multi-module project, 10 JPA entities, full CRUD, and 45 features (9 per service) — all sharing **one** PostgreSQL database, with cross-service reads done via native SQL `JOIN`s. |
| **M2** | Polyglot persistence, auth, patterns | 5 NoSQL databases (MongoDB, Redis, Elasticsearch, Neo4j, Cassandra), JWT authentication with BCrypt, Redis caching on all read endpoints, 7 GoF design patterns, and 15 more features (3 per service). Still one shared PostgreSQL. |
| **M3** | True microservices | **Database isolation** (one PostgreSQL per service), **OpenFeign** for synchronous reads, **RabbitMQ** for asynchronous events, a **Spring Cloud Gateway**, a shared **`contracts`** module, the **choreography saga**, and full **Kubernetes** deployment. |

This README describes the platform in its **final M3 state**.

---

## Key Features

- **End-to-End Order Lifecycle** — cart construction, cost estimation, confirmation, the deliver-saga, payment, and delivery tracking.
- **Choreography Saga** — the multi-service checkout flow is coordinated by events, not a central orchestrator; each service reacts to the previous step and reverses its own work on failure.
- **Database Isolation** — every service owns its own PostgreSQL instance. No service can open a JDBC connection to another service's database.
- **Polyglot Persistence** — PostgreSQL, MongoDB, Redis, Neo4j, Cassandra, and Elasticsearch, each used where it genuinely fits.
- **Event-Driven Communication** — RabbitMQ topic exchanges carry asynchronous events; OpenFeign handles synchronous read-time lookups.
- **Restaurant Recommendations** — Neo4j collaborative-filtering surfaces restaurants based on similar users' ordering patterns.
- **Centralized API Gateway** — Spring Cloud Gateway validates JWTs once at the edge, injects identity headers, and routes traffic.
- **Distributed Tracing via Correlation IDs** — every request is tagged at the edge and propagated through Feign and AMQP headers for end-to-end log correlation.
- **Full Observability Stack** — Loki log aggregation, Prometheus metrics, and Grafana dashboards (one per service), all provisioned out of the box.
- **Contract-First Integration** — a shared `contracts` module publishes DTOs, Feign interfaces, and event records, eliminating producer/consumer drift.
- **Kubernetes-Native Deployment** — every service, database, queue, and observability component ships as Deployments, StatefulSets, ConfigMaps, Secrets, PVCs, and Services across two namespaces.

---

## System Architecture

The platform follows a **microservices + event-driven + polyglot-persistence** architecture. Each bounded context owns its own database and is independently deployable.

```
                          ┌────────────────────────┐
                          │   Client / Frontend    │
                          └───────────┬────────────┘
                                      │ HTTPS
                          ┌───────────▼────────────┐
                          │   API Gateway          │
                          │  (Spring Cloud Gateway │
                          │   + JWT global filter) │
                          └───────────┬────────────┘
                                      │  X-User-Id · X-User-Role · X-Correlation-ID
      ┌───────────────┬───────────────┼───────────────┬───────────────┐
      ▼               ▼               ▼               ▼               ▼
┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐
│   User    │  │Restaurant │  │   Order   │  │ Delivery  │  │ Checkout  │
│  Service  │  │  Service  │  │  Service  │  │  Service  │  │  Service  │
└─────┬─────┘  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘
      │              │              │              │              │
┌─────▼─────┐  ┌─────▼─────┐  ┌─────▼─────┐  ┌─────▼─────┐  ┌─────▼─────┐
│   user-   │  │restaurant-│  │  order-   │  │ delivery- │  │ checkout- │
│ postgres  │  │ postgres  │  │ postgres  │  │ postgres  │  │ postgres  │
└───────────┘  └───────────┘  └───────────┘  └───────────┘  └───────────┘

         ┌──────────────────────────────────────────────────┐
         │  RabbitMQ — Event Backbone (topic exchanges)     │
         │  order.events · payment.events · delivery.events │
         │  user.events · restaurant.events                 │
         └──────────────────────────────────────────────────┘

   Shared NoSQL (logical isolation — one instance, separate ownership):
   MongoDB (event logs)   Redis (cache)   Elasticsearch (search)
   Neo4j (recommendations)            Cassandra (delivery tracking)

         ┌──────────────────────────────────────────────────┐
         │  Observability — Loki · Prometheus · Grafana     │
         │  (deployed in a separate `monitoring` namespace) │
         └──────────────────────────────────────────────────┘
```

### Communication Patterns

| Pattern | Use case | Transport |
|---------|----------|-----------|
| Synchronous read-time | Order needs the current restaurant menu / user profile | OpenFeign over HTTP |
| Asynchronous write-time | Order completed → notify checkout, delivery, analytics | RabbitMQ topic exchanges |
| Choreography saga | Multi-step checkout with compensating rollback | RabbitMQ events, no central orchestrator |
| Edge → service | Authenticated client traffic | Spring Cloud Gateway |

---

## The Services

Six Maven modules build the application; a seventh (`contracts`) holds shared types.

| Service | Module | Internal port | Database | Responsibility |
|---------|--------|---------------|----------|----------------|
| User Service | `user-service` | 8080 | `talabatdb-users` | Accounts, profiles, preferences, authentication provider |
| Restaurant Service | `restaurant-service` | 8080 | `talabatdb-restaurants` | Restaurants, menus, ratings, full-text search |
| Order Service | `order-service` | 8080 | `talabatdb-orders` | Order lifecycle, the saga state machine, recommendations |
| Delivery Service | `delivery-service` | 8080 | `talabatdb-deliveries` | Rider dispatch, tracking, time-series telemetry |
| Checkout Service | `checkout-service` | 8080 | `talabatdb-checkout` | Payment processing, refunds, promotions |
| API Gateway | `api-gateway` | 8080 | — | Single entry point, JWT validation, routing |

All services run on port `8080` internally; differentiation happens at the Docker host-port mapping and the Kubernetes Service layer. The gateway is exposed externally on NodePort `30080`.

---

## Tech Stack

### Backend

- **Java 25** with **Spring Boot 4.0**
- **Spring Cloud 2025.1.1** — Gateway (reactive / WebFlux), OpenFeign
- **Spring Data** — JPA, MongoDB, Redis, Neo4j, Elasticsearch, Cassandra
- **Spring Security** with JWT (JJWT 0.12.x), HMAC-SHA256, BCrypt password hashing
- **Spring AMQP** for RabbitMQ integration
- **Micrometer + Spring Boot Actuator** for metrics

### Databases

| Database | Version | Used by | Role |
|----------|---------|---------|------|
| PostgreSQL | 17 | All 5 services (isolated instances) | ACID transactional state |
| MongoDB | latest | All 5 services | Event logs / audit trails (Observer pattern) |
| Redis | latest | All 5 services | Cache layer, `allkeys-lru`, capped at 256 MB |
| Elasticsearch | 8.19.12 | Restaurant Service | Full-text restaurant search |
| Neo4j | latest | Order Service | Graph-based recommendations |
| Cassandra | latest | Delivery Service | Time-series delivery tracking events |

### Messaging

- **RabbitMQ 3** with the management plugin — topic exchanges, dead-letter queues, retry, at-least-once delivery semantics.

### DevOps & Infrastructure

- **Docker** & **Docker Compose** for local orchestration
- **Kubernetes** manifests — Namespaces, Deployments, StatefulSets, ConfigMaps, Secrets, PVCs, Services
- **Minikube** scripts for local cluster bring-up
- **Maven** multi-module reactor build (parent `pom.xml` at root)

### Observability

- **Loki** — centralized log aggregation; each service ships JSON logs via the Loki4J Logback appender
- **Prometheus** — scrapes `/actuator/prometheus` from each service every 15s
- **Grafana** — provisioned datasources and five dashboards (one per service)

---

## Polyglot Persistence

The system intentionally uses **polyglot persistence**: each storage engine is matched to a workload it is genuinely good at.

| Database | Why it was chosen |
|----------|-------------------|
| **PostgreSQL** | ACID guarantees for transactional state. In M3, each service has its own isolated instance — preventing cross-service schema coupling and limiting blast radius. |
| **MongoDB** | Flexible document storage for per-service event logs and audit trails written through the Observer pattern. |
| **Redis** | Hot cache for read-heavy endpoints, with TTLs per data type and wildcard invalidation on writes. |
| **Neo4j** | Graph traversal for collaborative-filtering recommendations (`(User)-[:ORDERED_FROM]->(Restaurant)`). |
| **Cassandra** | Wide-column, write-optimized storage for high-volume delivery-tracking time-series. |
| **Elasticsearch** | Full-text, relevance-ranked search across restaurant names and descriptions. |

---

## Inter-Service Communication

M3 replaces the M1/M2 cross-service SQL `JOIN`s with two complementary mechanisms.

### Synchronous — OpenFeign

Declarative HTTP clients handle read-time dependencies (e.g. Order Service fetching a restaurant's status before confirming an order). Every Feign call:

- forwards the `X-Correlation-ID` header via a `RequestInterceptor`,
- is wrapped in `try/catch` so a downstream failure never crashes the caller,
- is declared as an interface in the shared `contracts` module.

### Asynchronous — RabbitMQ

Write-time side effects fan out over **topic exchanges**. Producers declare only the exchange they publish to; consumers declare the queue, its **dead-letter queue**, and the binding. With `default-requeue-rejected: false` and `max-attempts: 3`, an exhausted retry routes the message to the DLQ automatically — no manual ack/nack code.

Because delivery is **at-least-once**, every listener is **idempotent** (a state-check guard or a per-event-id audit table).

### Event Map

| Producer | Exchange | Routing key | Consumers |
|----------|----------|-------------|-----------|
| order-service | `order.events` | `order.placed` | restaurant, delivery |
| order-service | `order.events` | `order.completed` | user, restaurant, delivery, checkout |
| order-service | `order.events` | `order.cancelled` | user, restaurant, delivery, checkout |
| delivery-service | `delivery.events` | `delivery.created` | order |
| checkout-service | `payment.events` | `payment.initiated` / `completed` / `failed` / `refunded` | order |
| user-service | `user.events` | `user.registered` / `user.deactivated` | observability only |
| restaurant-service | `restaurant.events` | `restaurant.status-changed` / `restaurant.rated` | observability only |

Observability-only events are picked up from the publisher's log output by Loki rather than consumed as RabbitMQ messages.

---

## The Order Lifecycle Saga

When a business transaction spans multiple services, there is no distributed rollback. The platform uses a **choreography saga** to reach eventual consistency:

- **Forward path** — each service listens for the previous step's success event and executes its part.
- **Compensation path** — on failure, the failing service publishes a failure event; every service that already committed reverses its local change on receipt.

The saga is triggered by `PUT /api/orders/{id}/deliver` (ADMIN-only).

```
1.  TRIGGER          PUT /api/orders/{id}/deliver
2.  PRE-CHECKS       Order Service runs three Feign pre-checks (sync):
                       → restaurant-service : restaurant must be OPEN
                       → user-service       : user must be ACTIVE
                       → delivery-service   : an active Delivery must exist
3.  COMPLETING       Order status PREPARING → COMPLETING; publish order.completed
4.  FAN-OUT          order.completed consumed by:
                       → checkout : create PENDING Payment; publish payment.initiated
                       → delivery : finalize the Delivery; publish delivery.created
                       → user / restaurant : update local stats
5.  PAYMENT_PENDING  Order consumes payment.initiated → status PAYMENT_PENDING
6a. SUCCESS          POST /api/payments/process → payment.completed → Order PAID
6b. FAILURE          payment fails → payment.failed → Order PAYMENT_FAILED
7.  COMPENSATION     Order publishes order.cancelled; user/restaurant stats
                     reversed, Delivery CANCELLED, Payment REFUNDED
8.  REFUNDED         Order status → REFUNDED
```

M3 adds five saga-related statuses to the Order enum: `COMPLETING`, `PAYMENT_PENDING`, `PAID`, `PAYMENT_FAILED`, `REFUNDED`.

---

## Design Patterns

Seven Gang-of-Four patterns are applied at well-defined locations across the codebase — three creational, three behavioral, one structural.

| Pattern | Category | Where it lives |
|---------|----------|----------------|
| Strategy | Behavioral | Refund logic — `FullRefundWithDelivery`, `FoodOnlyRefund`, `NoRefund` strategies selected at runtime |
| Observer | Behavioral | MongoDB event logging — a classical `EntityObserver` chain, not Spring's `@EventListener` |
| Chain of Responsibility | Behavioral | JWT authentication handler chain inside the filter |
| Builder | Creational | Dashboard and analytics DTOs with many fields |
| Singleton | Creational | `JwtConfigurationManager` — a true GoF singleton, not a Spring bean |
| Factory | Creational | `EventFactory` produces the right `MongoEvent` subtype per service |
| Adapter | Structural | NoSQL query results → service DTOs (Mongo, ES, Neo4j, Cassandra adapters) |

---

## Authentication & Security

- **JWT-based authentication** — tokens are issued by the User Service (`/api/auth/register`, `/api/auth/login`) and signed with HMAC-SHA256. The token payload carries the user's email (`sub`), numeric id (`uid`), and `role`.
- **Edge validation** — the API Gateway decodes the JWT once via a reactive `GlobalFilter`, then injects `X-User-Id` and `X-User-Role` downstream so individual services never re-validate tokens.
- **Correlation tracing** — an `X-Correlation-ID` is generated at the gateway (or honored if supplied) and propagated through Feign and AMQP headers.
- **Role-based access** — Spring Security per service enforces `CUSTOMER` and `ADMIN` roles; only the role-management endpoint is ADMIN-only.
- **Password handling** — BCrypt hashing; the plaintext password is never stored or returned in any API response.
- **Secret management** — Kubernetes `Secret` objects hold the JWT secret and per-service database credentials; no plaintext credentials in committed manifests.
- **Database isolation** — each service's own PostgreSQL instance limits the blast radius of any single compromise.

---

## Observability

The observability stack is wired in by default and runs in its own `monitoring` Kubernetes namespace, isolated from the application `talabat` namespace.

- **Logs (push)** — each service runs the **Loki4J** Logback appender, emitting structured JSON logs (with `correlationId`, entity IDs, and `routingKey` MDC fields) to Loki.
- **Metrics (pull)** — **Prometheus** scrapes each service's `/actuator/prometheus` endpoint every 15 seconds.
- **Dashboards** — **Grafana** auto-provisions a Loki and a Prometheus datasource plus **five dashboards** (one per service), each with at least three LogQL panels (error rate, correlation-ID trace, RabbitMQ event audit, saga transitions, …) and three PromQL panels (HTTP rate, latency percentiles, JVM health, connection pool, cache hit ratio, …).
- **Health** — Spring Boot Actuator `/actuator/health` plus per-container Docker/Kubernetes liveness and readiness probes.

A single request through the gateway should surface as a log line in Loki within ~5s and as an incremented Prometheus counter within ~15s — both visible on the corresponding service dashboard.

---

## Project Structure

```
talabat-m3/
├── pom.xml                  # Parent POM — 7 modules
├── docker-compose.yml       # Full local stack
├── README.md
├── contracts/               # Shared DTOs, Feign interfaces, event records
│   └── src/main/java/.../contracts/
│       ├── dto/
│       ├── feign/
│       └── events/
├── api-gateway/             # Spring Cloud Gateway (reactive) + JWT global filter
├── user-service/            # Auth provider, profiles, roles
├── restaurant-service/      # Restaurants, menus, full-text search
├── order-service/           # Orders, saga state machine, recommendations
│   └── src/main/java/.../order/
│       ├── adapter/         # Mongo / Neo4j adapters
│       ├── messaging/       # RabbitMQ publishers & consumers
│       ├── saga/            # Saga participation logic
│       └── security/        # Per-service JWT validation
├── delivery-service/        # Rider dispatch & tracking
├── checkout-service/        # Payment processing + refund strategies
├── k8s/                     # Kubernetes manifests
│   ├── namespaces/          # talabat + monitoring
│   ├── secrets/
│   ├── configmaps/
│   ├── pvcs/
│   ├── statefulsets/        # Databases
│   ├── deployments/         # Services
│   ├── services/
│   ├── api-gateway/
│   └── monitoring/          # loki / prometheus / grafana
└── automation/              # Minikube, saga-test, and helper scripts
```

### The `contracts` Module

`contracts` is a plain Maven JAR that all five services depend on. It holds the Feign client interfaces, the DTOs they exchange, and the event payload records. Putting these shared symbols **one level above** the services is structural — if event records lived inside a publisher service, every consumer would need a Maven dependency on that publisher, producing cyclic dependencies Maven refuses to build. The Maven reactor builds `contracts` first, then the five services (in any order), then `api-gateway`.

---

## Installation & Setup

### Prerequisites

- **Java 25** (or a compatible JDK)
- **Maven 3.9+** (or the bundled `./mvnw`)
- **Docker** & **Docker Compose**
- *(Optional)* **Minikube** + **kubectl** for the Kubernetes path

### Environment Variables

Create a `.env` file at the project root:

```dotenv
JWT_SECRET=<base64-encoded-256-bit-secret>
JWT_EXPIRATION=86400000
```

> The JWT secret must be a Base64-encoded value of at least 32 bytes (256 bits) — JJWT's HS256 signer rejects shorter keys with a `WeakKeyException`.

### Build the Multi-Module Project

```bash
./mvnw clean install -DskipTests
```

This builds `contracts`, then every service module, in the correct reactor order.

---

## Running the Project

### Local — Docker Compose

```bash
docker compose up --build
```

This brings up the full stack: five PostgreSQL instances, MongoDB, Redis, Neo4j, Cassandra, Elasticsearch, RabbitMQ, all five services, the API Gateway, and the observability stack.

| Component | URL |
|-----------|-----|
| API Gateway | <http://localhost:30080> |
| Grafana | <http://localhost:3000> (admin / admin) |
| Prometheus | <http://localhost:9090> |
| RabbitMQ Management | <http://localhost:15672> (guest / guest) |
| Kibana | <http://localhost:5601> |
| Neo4j Browser | <http://localhost:7474> |

All client traffic should go through the API Gateway — individual service ports are not meant to be hit directly.

---

## Kubernetes Deployment

```bash
# 1. Build images and load them into Minikube
./automation/load_images_to_minikube.sh

# 2. Apply manifests in dependency order
kubectl apply -f k8s/namespaces/
kubectl apply -f k8s/secrets/
kubectl apply -f k8s/pvcs/
kubectl apply -f k8s/statefulsets/      # databases first
kubectl wait --for=condition=ready pod -l app=order-postgres -n talabat --timeout=120s
kubectl apply -f k8s/configmaps/
kubectl apply -f k8s/deployments/       # services after databases
kubectl apply -f k8s/services/
kubectl apply -f k8s/api-gateway/

# 3. Observability stack
kubectl apply -f k8s/monitoring/

# 4. Verify
kubectl get pods -A
```

Access the platform at `http://$(minikube ip):30080` and Grafana at `http://$(minikube ip):30030`. Every service runs as a `Deployment` with liveness/readiness probes on `/actuator/health`; every database runs as a `StatefulSet` with a `PersistentVolumeClaim`. Only the gateway is exposed externally (NodePort) — all other services use `ClusterIP`.

---

## Feature Inventory

The platform exposes **60 features** plus full CRUD on every entity, accumulated across the three milestones.

### By milestone

| Milestone | Features | Highlights |
|-----------|----------|------------|
| M1 | 45 (9 per service) | Search, JSONB queries, report DTOs, transactional multi-step operations |
| M2 | 15 (3 per service) | Registration/login, full-text search, recommendation graph, analytics dashboards, strategy-based refunds |
| M3 | refactors | The same features, with cross-service SQL replaced by Feign + RabbitMQ, plus the deliver-saga |

### Representative features per service

- **User** — order summary, account deactivation (saga-aware), top customers by spending, dietary-preference search, activity feed.
- **Restaurant** — cuisine/rating search, full-text Elasticsearch search, revenue summary, post-order rating, performance dashboard.
- **Order** — cost estimate, confirm-and-assign, the deliver-saga trigger, cancel-with-compensation, interaction recording, recommendations.
- **Delivery** — nearby deliveries, batch updates, tracking timeline, analytics dashboard, saga finalization of the delivery row.
- **Checkout** — process payment (idempotent), apply offers, revenue-by-cuisine, payment-method breakdown, strategy-based refund with delivery-fee handling.

---

## Future Improvements

- **Service mesh** (Istio / Linkerd) for mTLS, traffic shifting, and richer telemetry.
- **OpenTelemetry tracing** end-to-end, replacing correlation-ID-only tracing.
- **Transactional outbox** for guaranteed event publication from PostgreSQL-backed services.
- **Schema registry** for RabbitMQ message contracts (Avro / Protobuf).
- **Production CI/CD** with image scanning, SBOM generation, and progressive delivery.
- **Token revocation** (revocation list or short expiry + refresh tokens) to close the role-change staleness gap.
- **Multi-region active-active** deployment with Cassandra / PostgreSQL replication.

---

## Contributors

Built collaboratively by **Team 05** as a capstone-scale engineering project for *Architecture of Massively Scalable Applications*, German University in Cairo.

| Name | ID |
|------|-----|
| Mohamad Elsayed Rageh | 55-3936 |
| Youssef Nasser Mohamed | 55-7165 |
| Khaled Ashmawy | 55-25730 |
| Ali Khaled Koheil | 55-24778 |
| Mariam Mohamed | 55-16508 |
| Mohamed Gamal | 55-12064 |
| Mahmoud Hegazy | 55-13845 |
| Marawan Salah Abdelrahman | 55-20836 |
| Omar Mohamed Farouk | 56-29265 |
| Hazem Essam Brekaa | 55-15850 |
| Omar Tamer Samy Ahmed Riad | 55-16204 |
| Habiba Moustafa Elguindy | 55-11063 |
| Mohamed Elsaeed Elmenshawy | 55-12559 |
| Demiana Reffaat Fahmy | 55-15053 |
| Fareeda Saad | 55-11289 |

---

## License

Released for educational and portfolio purposes. All trademarks — including "Talabat" — belong to their respective owners; this codebase is an independent engineering exercise and is not affiliated with any commercial entity.
