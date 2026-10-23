# Shipment Tracking Service

A **Java 21 / Spring Boot 3** REST API for booking shipments and tracking them through their delivery lifecycle,
built the way a logistics backend is run in production: layered architecture, a state machine for statuses,
Flyway migrations, optimistic locking, RFC 7807 errors, actuator health probes and Docker/Kubernetes deployment.

![CI](https://github.com/PradeepG01/shipment-tracking-service/actions/workflows/ci.yml/badge.svg)

## Features

* **Create shipments** with validated Indian pincodes; a tracking number and estimated delivery date are generated
  (transit days from pincode region/district, Sundays skipped).
* **Status lifecycle as a finite state machine** (`ShipmentStatus`): `CREATED → PICKED_UP → IN_TRANSIT →
  OUT_FOR_DELIVERY → DELIVERED`, with `DELIVERY_FAILED` retry, `RETURNED` and `CANCELLED`. Illegal jumps return **409**.
* **Full tracking history** of events with location and notes, loaded with one query (`@EntityGraph`, no N+1).
* **Pagination and filtering** by status.
* **Optimistic locking** (`@Version`) so concurrent scanner updates cannot overwrite each other.
* **Consistent errors** as `application/problem+json` (RFC 7807) with field-level validation messages.
* **Operations:** Spring Boot Actuator health/readiness/liveness, Prometheus metrics, graceful shutdown.

## Tech stack

Java 21 · Spring Boot 3.5 (Web, Data JPA, Validation, Actuator) · Hibernate · Flyway · PostgreSQL (H2 for local/tests)
· JUnit 5 · MockMvc · AssertJ · Maven · Docker · GitHub Actions · Kubernetes

## Project structure

```
src/main/java/com/pradeep/shipments
├── domain/    Shipment, TrackingEvent (JPA entities), ShipmentStatus (state machine), repository
├── service/   ShipmentService (transactions, business rules), DeliveryEstimator
└── web/       ShipmentController, DTO records, GlobalExceptionHandler
src/main/resources/db/migration   Flyway SQL migrations
```

## Run

```bash
mvn spring-boot:run                       # in-memory H2
docker compose up --build                 # with PostgreSQL (profile "postgres")
```

```bash
curl -X POST localhost:8080/api/v1/shipments -H 'Content-Type: application/json' \
  -d '{"senderName":"Kumar Traders","recipientName":"Asha Rao","originPincode":"560001","destinationPincode":"110001","weightKg":2.5}'

curl -X PATCH localhost:8080/api/v1/shipments/LQ0123456789/status -H 'Content-Type: application/json' \
  -d '{"status":"PICKED_UP","location":"Bengaluru hub"}'

curl 'localhost:8080/api/v1/shipments?status=IN_TRANSIT&page=0&size=20'
```

## API

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/shipments` | Book a shipment → `201 Created` + `Location` header |
| `GET` | `/api/v1/shipments/{trackingNumber}` | Shipment with full tracking history |
| `PATCH` | `/api/v1/shipments/{trackingNumber}/status` | Record a status change (validated against the state machine) |
| `GET` | `/api/v1/shipments?status=&page=&size=` | Paginated list, optional status filter |
| `GET` | `/actuator/health` | Health (readiness/liveness probes for Kubernetes) |

## Tests

```bash
mvn verify
```

Unit tests cover the state machine and delivery-date rules; integration tests boot the full application with
MockMvc and H2 (via the same Flyway migrations used in production) and cover the happy path, invalid transitions,
validation errors, 404s, pagination and health.
