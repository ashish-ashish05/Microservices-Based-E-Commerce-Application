# 16. Tracking Service Implementation Plan

## 1. Plan Metadata

- Service: Tracking Service
- Build Order: 16
- Assignment: Shipping & Tracking
- Specification: .claude/specs/16-tracking-service.md
- Status: Planned
- Plan Generated: 2026-10-03
- Prerequisite Services: None

## 2. Objective

The Tracking Service will maintain a detailed history of shipment progress, providing a lookup mechanism for users and administrators to see the current status and event history of their orders.

## 3. Existing Context

The Tracking Service is a provider service, primarily called by the Shipping Service (Port 8095) to initialize tracking and add events. It is also consumed via the API Gateway by clients to retrieve tracking history.

## 4. Scope

### In Scope
- Tracking record and TrackingEvent persistence.
- Implementation of the State pattern for status transitions.
- Provision of tracking history lookup by tracking number.
- Implementation of Repository and DTO patterns.
- Global exception handling for 404 and 400 errors.
- Unit, Controller, and Integration tests.

### Out of Scope
- Creating the shipment (handled by Shipping Service).
- Assigning couriers or labels (handled by Shipping Service).
- Sending notifications (handled by Notification Service).

## 5. Architecture

Client
  |
API Gateway
  |
Tracking Service
  |
(H2 Database)

The Tracking Service owns the `Tracking` and `TrackingEvent` entities. It maintains an audit log of every status change.

## 6. Implementation Dependencies

- **Maven Dependencies**:
    - `spring-boot-starter-web`
    - `spring-boot-starter-data-jpa`
    - `spring-boot-starter-validation`
    - `com.h2database:h2`
    - `org.projectlombok:lombok`
    - `spring-boot-starter-actuator`
- **Configuration**:
    - `server.port=8087`
    - `spring.datasource.url=jdbc:h2:mem:tracking-db`

## 7. Data Model Plan

| Entity | Field | Type | Constraint | Notes |
|--------|-------|------|------------|-------|
| Tracking | id | UUID | Primary Key | Internal ID |
| Tracking | shipmentId | UUID | Unique, Indexed | Reference to Shipping Service |
| Tracking | trackingNumber | String | Unique, Indexed | Public tracking number |
| Tracking | currentStatus | String | Not Null | Latest status |
| Tracking | updatedAt | LocalDateTime | Not Null | Last update timestamp |
| TrackingEvent | id | UUID | Primary Key | Event ID |
| TrackingEvent | trackingId | UUID | Foreign Key | Reference to Tracking entity |
| TrackingEvent | status | String | Not Null | Event status |
| TrackingEvent | location | String | - | Location of event |
| TrackingEvent | description | String | - | Detailed event description |
| TrackingEvent | timestamp | LocalDateTime | Not Null | Time of event |

## 8. API Implementation Plan

### Initialize Tracking
- HTTP method: `POST`
- path: `/api/tracking`
- request DTO: `TrackingRequest` (shipmentId, trackingNumber, initialStatus)
- validation: All fields required and non-empty.
- response DTO: `TrackingResponse`
- status codes: `201 Created`, `400 Bad Request`
- authentication/authorization: Internal/Admin
- downstream calls: None
- error behavior: Throw `ValidationException` for malformed requests.

### Get Tracking History
- HTTP method: `GET`
- path: `/api/tracking/{trackingNumber}`
- request DTO: N/A
- validation: Alphanumeric format for `{trackingNumber}`
- response DTO: `TrackingHistoryResponse` (trackingNumber, currentStatus, events)
- status codes: `200 OK`, `404 Not Found`
- authentication/authorization: User/Admin
- downstream calls: None
- error behavior: Throw `TrackingNotFoundException`.

### Add Tracking Event
- HTTP method: `POST`
- path: `/api/tracking/{trackingNumber}/events`
- request DTO: `TrackingEventRequest` (status, location, description, timestamp)
- validation: `status` required.
- response DTO: `201 Created`
- status codes: `201 Created`, `400 Bad Request`, `404 Not Found`
- authentication/authorization: Internal/Admin
- downstream calls: None
- error behavior: Throw `InvalidStatusTransitionException` for illegal state moves.

## 9. Service Layer Plan

1. **Initialize Tracking**:
    - Save the `Tracking` record.
    - Create the first `TrackingEvent` and link it to the record.
    - Set the `currentStatus` of the `Tracking` entity.
2. **Get Tracking History**:
    - Retrieve `Tracking` by `trackingNumber`.
    - Retrieve all associated `TrackingEvent`s sorted by timestamp.
    - Map to `TrackingHistoryResponse`.
3. **Add Tracking Event**:
    - Retrieve `Tracking` by `trackingNumber`.
    - Use the State pattern to verify if the status transition is legal.
    - Save the `TrackingEvent` and update the `currentStatus` of the `Tracking` entity.

## 10. Design Pattern Implementation Plan

### State Pattern (Simplified)
- Where: Tracking status transitions.
- Why: To ensure shipments move through valid states (e.g., cannot go from 'Delivered' back to 'Picked Up').
- Main participants: `TrackingStatus` enum and `TrackingService` transition logic.

### Repository Pattern
- Where: `TrackingRepository` and `TrackingEventRepository`.
- Why: Standard JPA abstraction.
- Main participants: `TrackingRepository` and `TrackingEventRepository` extending `JpaRepository`.

### DTO Pattern
- Where: `TrackingRequest`, `TrackingResponse`, `TrackingHistoryResponse`.
- Why: Decouples internal entities from API contract.
- Main participants: `TrackingRequest`, `TrackingResponse`, `TrackingHistoryResponse`.

## 11. Remote Communication Plan

No remote calls are made by the Tracking Service.

## 12. Exception Handling Plan

- **TrackingNotFoundException**: Thrown when `trackingNumber` is not found. Maps to 404 Not Found.
- **InvalidStatusTransitionException**: Thrown when an event status is logically impossible. Maps to 400 Bad Request.
- **Global Exception Handler**: Standardized JSON response.

## 13. Security Plan

- **Authentication**: Managed by API Gateway (JWT).
- **Authorization**: `GET /api/tracking/{trackingNumber}` is accessible to owners. `POST` endpoints are restricted to internal/admin calls.
- **Sensitive Data**: Internal `shipmentId` is not exposed in public APIs.

## 14. Testing Plan

### Unit Tests
- `TrackingService` business logic for status transitions and history aggregation.
- `TrackingStatus` transition validation.

### Controller Tests
- MockMvc tests for all endpoints, ensuring 404s for missing tracking numbers and 400s for invalid requests.

### Repository Tests
- Verify `TrackingEvent` lookup by `trackingId` sorted by timestamp.

### Integration Tests
- End-to-end flow: Initialize tracking $\rightarrow$ Add events $\rightarrow$ Retrieve history.

### Failure Tests
- Attempt to add an event to a non-existent tracking record.
- Attempt to set an invalid status.

## 15. Files to Create

```text
src/main/java/com/ecommerce/tracking_service/
├── TrackingServiceApplication.java
├── controller/TrackingController.java
├── service/TrackingService.java
├── service/TrackingServiceImpl.java
├── repository/TrackingRepository.java
├── repository/TrackingEventRepository.java
├── entity/Tracking.java
├── entity/TrackingEvent.java
└── dto/TrackingRequest.java
└── dto/TrackingResponse.java
└── dto/TrackingEventRequest.java
└── dto/TrackingHistoryResponse.java
└── exception/TrackingNotFoundException.java
└── exception/GlobalExceptionHandler.java
```

## 16. Files to Modify

None.

## 17. Implementation Sequence

1. Create/update Maven project.
2. Add required dependencies in `pom.xml`.
3. Create `TrackingStatus` enum.
4. Create `Tracking` and `TrackingEvent` entities.
5. Create `TrackingRepository` and `TrackingEventRepository`.
6. Create DTOs.
7. Implement `TrackingService` business logic.
8. Implement `TrackingController`.
9. Implement `TrackingNotFoundException` and integrate with `GlobalExceptionHandler`.
10. Configure `application.properties`.
11. Implement Unit and Integration tests.
12. Run `./mvnw clean test` to verify.

## 18. Verification Plan

- Run `./mvnw clean test`.
- Run `./mvnw help:effective-pom`.

## 19. Risks and Failure Scenarios

- **Duplicate Tracking Numbers**: Handled by unique constraint in the database.
- **Invalid Status Transition**: Handled by `InvalidStatusTransitionException`.

## 20. Acceptance Criteria

- [ ] `POST /api/tracking` initializes a record.
- [ ] `POST /api/tracking/{trackingNumber}/events` adds events and updates `currentStatus`.
- [ ] `GET /api/tracking/{trackingNumber}` returns correct history.
- [ ] Invalid status transitions are rejected.
- [ ] No direct access to other services' databases.
- [ ] `./mvnw clean test` passes.
- [ ] Dependency versions follow `CLAUDE.md`.

## 21. Planning Issues

None.

## 22. Implementation Prompt

> Implement `Tracking Service` according to `.claude/specs/16-tracking-service.md` and this plan. Read the root `CLAUDE.md`. Follow the implementation sequence and acceptance criteria. Do not introduce Kafka/RabbitMQ. Verify the build and tests before completion.
