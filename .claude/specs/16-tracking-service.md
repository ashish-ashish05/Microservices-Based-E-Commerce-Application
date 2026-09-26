# 16. Tracking Service Specification

## 1. Document Status

- Service: Tracking Service
- Build Order: 16
- Assignment: Shipping & Tracking
- Status: Proposed
- Scope: Specification only

## 2. Purpose

The Tracking Service is responsible for maintaining and providing the history of a shipment's progress. While the Shipping Service manages the creation and assignment of shipments, the Tracking Service owns the granular tracking events (e.g., "Picked up", "In Transit", "Out for Delivery", "Delivered") and provides a lookup mechanism for users and administrators to see the current status and history of their orders.

## 3. Responsibilities

- Maintain tracking history for a given shipment.
- Store tracking events with timestamps and locations.
- Provide tracking lookup APIs based on tracking number or shipment ID.
- Update shipment status based on tracking events.
- Provide a consolidated tracking summary.

## 4. Non-Responsibilities

- Creating the initial shipment (Shipping Service responsibility).
- Assigning couriers or generating labels (Shipping Service responsibility).
- Managing order payments or inventory (Order/Payment/Inventory Services).
- Sending notifications (Notification Service responsibility).

## 5. Service Boundary

- **Data Ownership:** Owns the `Tracking` and `TrackingEvent` entities.
- **Domain Ownership:** Owns the tracking lifecycle and status history.
- **External Dependencies:** May be called by the Shipping Service to initialize tracking or by the API Gateway/Client to retrieve status.
- **Boundary Justification:** Tracking is a high-read, high-update domain that can grow significantly in data volume compared to the shipment record itself. Separating it allows for independent scaling of tracking lookups.

## 6. Architecture

Client
  |
API Gateway
  |
Tracking Service
  |
(H2 Database)

## 7. Service-to-Service Communication

| Consumer | Provider | Method | Endpoint | Purpose | Failure Consideration |
|----------|----------|--------|----------|---------|-----------------------|
| Shipping Service | Tracking Service | POST | `/api/tracking` | Initialize tracking for a new shipment | Retry on 5xx, Circuit Breaker |
| API Gateway | Tracking Service | GET | `/api/tracking/{trackingNumber}` | Retrieve tracking history for a user | Fallback to "Tracking information unavailable" |

## 8. API Specification

### Initialize Tracking

- Method: `POST`
- Path: `/api/tracking`
- Purpose: Create a tracking record for a new shipment.
- Authentication: Internal/Admin
- Authorization: ADMIN
- Request body:
  ```json
  {
    "shipmentId": "uuid",
    "trackingNumber": "string",
    "initialStatus": "string"
  }
  ```
- Success response: `201 Created` with `TrackingResponse`
- Error responses: `400 Bad Request`

### Get Tracking History

- Method: `GET`
- Path: `/api/tracking/{trackingNumber}`
- Purpose: Retrieve all tracking events for a specific tracking number.
- Authentication: User/Admin
- Authorization: USER/ADMIN (Ownership check via Gateway/Order Service usually)
- Path parameters: `trackingNumber`
- Success response: `200 OK` with `TrackingHistoryResponse`
- Error responses: `404 Not Found`

### Add Tracking Event

- Method: `POST`
- Path: `/api/tracking/{trackingNumber}/events`
- Purpose: Add a new status update to a shipment.
- Authentication: Internal/Admin
- Authorization: ADMIN
- Path parameters: `trackingNumber`
- Request body:
  ```json
  {
    "status": "string",
    "location": "string",
    "description": "string",
    "timestamp": "datetime"
  }
  ```
- Success response: `201 Created`
- Error responses: `404 Not Found`, `400 Bad Request`

## 9. Data Model

### Tracking Entity
| Field | Type | Required | Constraints | Description |
|------|------|----------|-------------|-------------|
| id | UUID | Yes | Primary Key | Internal ID |
| shipmentId | UUID | Yes | Unique, Indexed | Reference to Shipping Service shipment |
| trackingNumber | String | Yes | Unique, Indexed | Public tracking number |
| currentStatus | String | Yes | Not Null | Latest status (e.g., DELIVERED) |
| updatedAt | LocalDateTime | Yes | Not Null | Last update timestamp |

### TrackingEvent Entity
| Field | Type | Required | Constraints | Description |
|------|------|----------|-------------|-------------|
| id | UUID | Yes | Primary Key | Internal ID |
| trackingId | UUID | Yes | Foreign Key | Reference to Tracking entity |
| status | String | Yes | Not Null | Event status |
| location | String | No | - | Location of event |
| description | String | No | - | Detailed event description |
| timestamp | LocalDateTime | Yes | Not Null | Time of event |

## 10. DTOs

### TrackingRequest
- Purpose: Initialize tracking.
- Fields: `shipmentId`, `trackingNumber`, `initialStatus`.
- Validation: All required, non-empty.

### TrackingResponse
- Purpose: Summary of tracking status.
- Fields: `trackingNumber`, `currentStatus`, `updatedAt`.

### TrackingEventRequest
- Purpose: Add new event.
- Fields: `status`, `location`, `description`, `timestamp`.
- Validation: `status` required.

### TrackingHistoryResponse
- Purpose: Full history view.
- Fields: `trackingNumber`, `currentStatus`, `events` (List of `EventDTO`).

## 11. Business Rules

- A tracking number must be unique.
- Tracking events must be stored in chronological order.
- The `currentStatus` of the `Tracking` entity must always match the status of the most recent `TrackingEvent`.
- Tracking cannot be initialized for a non-existent shipment ID (though validated by the calling service).

## 12. Design Patterns

### Pattern: State Pattern (Simplified)
- Where: Tracking status transitions.
- Why: To ensure shipments move through valid states (e.g., cannot go from 'Delivered' back to 'Picked Up').
- Problem it solves: Invalid state transitions in shipment lifecycle.
- Key participants: `TrackingStatus` enum, `TrackingService`.

### Pattern: Repository Pattern
- Where: Data access for `Tracking` and `TrackingEvent`.
- Why: Standard Spring Data JPA abstraction.

## 13. Dependencies

### Required
- `spring-boot-starter-web`: REST APIs.
- `spring-boot-starter-data-jpa`: Persistence.
- `spring-boot-starter-validation`: Request validation.
- `com.h2database:h2`: Development database.
- `org.projectlombok:lombok`: Boilerplate reduction.
- `spring-boot-starter-actuator`: Health checks.

### Optional / Later
- `spring-cloud-starter-openfeign`: If Tracking Service needs to call other services (currently passive).

## 14. Configuration

```properties
server.port=8087
spring.application.name=tracking-service
spring.datasource.url=jdbc:h2:mem:tracking-db
# H2 and JPA defaults as per CLAUDE.md
```

## 15. Security

- **Authentication:** Managed by API Gateway (JWT).
- **Authorization:** 
    - `GET /api/tracking/{trackingNumber}`: Accessible to users who own the shipment (verified by Order/Shipping service via Gateway).
    - `POST` endpoints: Restricted to `ADMIN` or internal service calls.
- **Sensitive Info:** Tracking numbers are public, but internal `shipmentId` should not be exposed in public APIs.

## 16. Exception and Error Handling

- `TrackingNotFoundException`: Thrown when a tracking number does not exist. (404 Not Found)
- `InvalidStatusTransitionException`: Thrown when an event status is logically impossible. (400 Bad Request)
- Use project's `GlobalExceptionHandler` for consistent response shapes.

## 17. Transaction Boundaries

- `addTrackingEvent`: Must be transactional to ensure both the `TrackingEvent` is saved and the `Tracking` entity's `currentStatus` is updated atomically.

## 18. Validation

- `trackingNumber`: Must follow a specific format (e.g., alphanumeric).
- `timestamp`: Cannot be in the future.
- `status`: Must be a valid value from the `TrackingStatus` enum.

## 19. Testing Strategy

### Unit Tests
- `TrackingService`: Business logic for status transitions and history aggregation.
- `TrackingStatus`: Validation of state transitions.

### Controller Tests
- MockMvc tests for all endpoints, ensuring 404s for missing tracking numbers and 400s for invalid requests.

### Repository Tests
- Verify `TrackingEvent` lookup by `trackingId` sorted by timestamp.

### Integration Tests
- Full flow: Initialize tracking -> Add events -> Retrieve history.

### Failure Tests
- Attempt to add an event to a non-existent tracking record.
- Attempt to set an invalid status.

## 20. Observability

- Logs: Log every status change for a shipment with tracking number.
- Metrics: Track number of tracking lookups per minute.

## 21. Implementation Sequence

1. Create/update Maven project (already done).
2. Add required dependencies in `pom.xml`.
3. Create `TrackingStatus` enum.
4. Create `Tracking` and `TrackingEvent` entities.
5. Create `TrackingRepository` and `TrackingEventRepository`.
6. Create DTOs (`TrackingRequest`, `TrackingResponse`, etc.).
7. Implement `TrackingService` with business logic for initialization and event updates.
8. Implement `TrackingController`.
9. Implement `TrackingNotFoundException` and integrate with `GlobalExceptionHandler`.
10. Configure `application.properties`.
11. Implement Unit and Integration tests.
12. Run `./mvnw clean test` to verify.

## 22. Acceptance Criteria

- [ ] `POST /api/tracking` initializes a record.
- [ ] `POST /api/tracking/{trackingNumber}/events` adds events and updates `currentStatus`.
- [ ] `GET /api/tracking/{trackingNumber}` returns correct history.
- [ ] Invalid status transitions are rejected.
- [ ] No direct access to other services' databases.
- [ ] `./mvnw clean test` passes.
- [ ] Dependency versions follow `CLAUDE.md`.

## 23. Out of Scope

- Notification sending (Notification Service).
- Courier API integration (Shipping Service).
- Real-time map tracking (Future scope).

## 24. Decisions and Open Questions

- **Decision:** Use a separate `TrackingEvent` table to maintain a full audit log rather than just updating a status string.
- **Decision:** Tracking Service is passive; it is called by others and does not call other services in this phase.
- **Assumption:** The `trackingNumber` is generated by the Shipping Service and passed to the Tracking Service.

---

**Suggested Branch:** `feature/tracking-service`
*Please switch to this branch before starting implementation.*
