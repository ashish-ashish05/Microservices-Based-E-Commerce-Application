# 15. Shipping Service Specification

## 1. Document Status

- Service: Shipping Service
- Build Order: 15
- Assignment: Logistics & Fulfillment
- Status: Proposed
- Scope: Specification only

## 2. Purpose

The Shipping Service is responsible for managing the physical delivery of orders. It handles the creation of shipments, assignment of shipping methods, generation of shipment labels/details, and tracking of shipment status. It acts as the bridge between the e-commerce system and mock courier providers.

## 3. Responsibilities

- Create shipments for orders.
- Assign appropriate shipping methods based on order requirements.
- Generate shipment and label information.
- Track and update shipment status.
- Integrate with mock courier providers.
- Provide shipment details to the Order Service and Tracking Service.

## 4. Non-Responsibilities

- **Order Management:** Does not manage the order lifecycle or payment status (owned by Order Service).
- **Payment Processing:** Does not handle payments (owned by Payment Service).
- **Inventory Management:** Does not track stock levels (owned by Inventory Service).
- **Notification Sending:** Does not send emails/SMS to customers (owned by Notification Service).
- **User Profiles:** Does not manage customer addresses or profiles (owned by User Service).

## 5. Service Boundary

- **Data Ownership:** Owns all shipment records, shipping method definitions, and courier integration mappings.
- **Domain Ownership:** Logistics and Fulfillment.
- **External Dependencies:** Relies on Order Service for shipment requests and Tracking Service for status updates.
- **Boundary Rationale:** Separating shipping allows for different scaling needs (logistics can be heavy on external API calls) and enables switching courier providers without affecting order logic.

## 6. Architecture

Client
  |
API Gateway
  |
Order Service
  |
Shipping Service
  |
(Mock Courier Provider)

## 7. Service-to-Service Communication

| Consumer | Provider | Method | Endpoint | Purpose | Failure Consideration |
|----------|----------|--------|----------|---------|-----------------------|
| Order Service | Shipping Service | POST | `/api/shipments` | Create a new shipment | Circuit Breaker: Fallback to "Pending Shipment" status in Order Service |
| Shipping Service | Tracking Service | POST | `/api/tracking` | Update shipment tracking history | Retry: Ensure tracking is eventually recorded |

**Synchronous Communication Policies:**
- **Timeouts:** Strict timeouts (e.g., 2s) for courier API mocks to prevent blocking Order Service.
- **Retry:** Retries only for idempotent updates to the Tracking Service.
- **Circuit Breaker:** Applied to the courier integration point.
- **Idempotency:** Shipment creation must be idempotent based on `orderId`.

## 8. API Specification

### Create Shipment

- Method: `POST`
- Path: `/api/shipments`
- Purpose: Initiates a shipment for a completed order.
- Authentication: Internal Service only (via Gateway/Order Service).
- Authorization: `ADMIN` or `SYSTEM`.
- Request body:
  ```json
  {
    "orderId": "UUID",
    "shippingAddress": "String",
    "shippingMethodId": "UUID",
    "weight": "Double"
  }
  ```
- Success response: `201 Created` with `ShipmentResponse` DTO.
- Error responses: `400 Bad Request` (invalid address), `404 Not Found` (invalid method).

### Get Shipment Details

- Method: `GET`
- Path: `/api/shipments/{shipmentId}`
- Purpose: Retrieve details of a specific shipment.
- Authentication: JWT.
- Authorization: `ADMIN` or Owner of the order.
- Success response: `200 OK` with `ShipmentResponse` DTO.
- Error responses: `404 Not Found`.

## 9. Data Model

### Shipment Entity
| Field | Type | Required | Constraints | Description |
|------|------|----------|-------------|-------------|
| id | UUID | Yes | Primary Key | Unique shipment ID |
| orderId | UUID | Yes | Unique, Indexed | Reference to the order |
| shippingMethodId | UUID | Yes | Foreign Key | Reference to ShippingMethod |
| trackingNumber | String | No | Unique | Assigned by courier |
| status | String | Yes | Not Null | PENDING, SHIPPED, DELIVERED, FAILED |
| address | String | Yes | Not Null | Destination address |
| createdAt | LocalDateTime | Yes | Not Null | Audit field |
| updatedAt | LocalDateTime | Yes | Not Null | Audit field |

### ShippingMethod Entity
| Field | Type | Required | Constraints | Description |
|------|------|----------|-------------|-------------|
| id | UUID | Yes | Primary Key | Unique method ID |
| name | String | Yes | Not Null | e.g., "Express", "Standard" |
| cost | Double | Yes | >= 0 | Shipping cost |
| estimatedDays | Integer | Yes | > 0 | Delivery estimate |

## 10. DTOs

### ShipmentRequest
- Purpose: Request to create a shipment.
- Fields: `orderId` (UUID, required), `shippingAddress` (String, required), `shippingMethodId` (UUID, required), `weight` (Double).

### ShipmentResponse
- Purpose: Return shipment details.
- Fields: `shipmentId` (UUID), `trackingNumber` (String), `status` (String), `estimatedDelivery` (LocalDateTime).

## 11. Business Rules

- A shipment cannot be created for the same `orderId` more than once (Idempotency).
- Shipments cannot be created if the shipping method is inactive.
- Tracking numbers must be unique across all shipments.
- A shipment status cannot move backward (e.g., DELIVERED cannot move to SHIPPED).

## 12. Design Patterns

### Pattern: Adapter Pattern

- Where: Courier Integration Layer.
- Why: To decouple the system from specific courier API formats (FedEx, UPS, DHL).
- Problem it solves: Different courier providers have different request/response formats.
- Key participants: `CourierAdapter` (Interface), `MockCourierAdapter` (Implementation).
- Alternatives considered: Direct API calls (creates high coupling).

### Pattern: Strategy Pattern

- Design for different shipping cost calculation or label generation strategies based on the `ShippingMethod`.

## 13. Dependencies

### Required
- `spring-boot-starter-web`: REST APIs.
- `spring-boot-starter-data-jpa`: Persistence.
- `spring-boot-starter-validation`: Request validation.
- `spring-boot-starter-actuator`: Health checks.
- `spring-cloud-starter-openfeign`: Communication with Tracking Service.
- `spring-cloud-starter-circuitbreaker-resilience4j`: Resilience for courier API mocks.
- `com.h2database:h2`: Development DB.
- `org.projectlombok:lombok`: Boilerplate reduction.

### Optional / Later
- `spring-doc-openapi`: API documentation.

## 14. Configuration

```properties
server.port=8095
spring.application.name=shipping-service
spring.datasource.url=jdbc:h2:mem:shipping-db
# Courier API Mock Settings
courier.api.base-url=http://mock-courier-api
courier.api.timeout=2000
```

## 15. Security

- **Authentication:** All endpoints except internal creation require valid JWT.
- **Authorization:** `ADMIN` can manage all shipments; `USER` can only view their own shipments.
- **Sensitive Data:** Do not log full customer addresses in debug logs.

## 16. Exception and Error Handling

- `ShipmentNotFoundException`: 404 Not Found.
- `InvalidShippingMethodException`: 400 Bad Request.
- `CourierIntegrationException`: 502 Bad Gateway (handled via circuit breaker).
- `DuplicateShipmentException`: 409 Conflict.

## 17. Transaction Boundaries

- `createShipment`: Local transaction to save the shipment record and update initial status.
- `updateTracking`: Local transaction to update shipment status.

## 18. Validation

- `orderId`: Must be a valid UUID.
- `shippingAddress`: Must not be blank.
- `weight`: Must be positive.
- `shippingMethodId`: Must exist in the database.

## 19. Testing Strategy

### Unit Tests
- Business rules for shipment status transitions.
- Courier Adapter mapping logic.

### Controller Tests
- Validate `ShipmentRequest` constraints.
- Test 404 and 409 response codes.

### Repository Tests
- Test unique constraint on `orderId`.

### Integration Tests
- End-to-end flow: Create shipment -> Call Mock Courier -> Save Tracking Number.

### Failure Tests
- Simulate Courier API timeout/500 and verify Resilience4j fallback.
- Attempt to create duplicate shipments for the same order.

## 20. Observability

- **Logs:** Log every shipment creation request and every courier API response.
- **Health:** Actuator `/health` endpoint.
- **Metrics:** Count of shipments created per hour, Courier API failure rate.

## 21. Implementation Sequence

1. Update Maven project with required dependencies.
2. Create `Shipment` and `ShippingMethod` entities.
3. Create `ShipmentRepository` and `ShippingMethodRepository`.
4. Create request/response DTOs.
5. Implement `CourierAdapter` interface and `MockCourierAdapter`.
6. Implement `ShippingService` (business logic).
7. Implement `ShippingController`.
8. Implement OpenFeign client for `Tracking Service`.
9. Implement Resilience4j circuit breaker for courier integration.
10. Implement global exception handling.
11. Add unit, controller, and integration tests.
12. Run Maven verification.

## 22. Acceptance Criteria

- [ ] `POST /api/shipments` creates a shipment and assigns a tracking number from the mock courier.
- [ ] Duplicate shipment requests for the same `orderId` are rejected.
- [ ] Circuit breaker triggers when the mock courier API is unavailable.
- [ ] Shipment status transitions are validated.
- [ ] Shipping methods are looked up from the database.
- [ ] Integration with Tracking Service works via OpenFeign.
- [ ] `./mvnw clean test` passes.
- [ ] No Kafka/RabbitMQ introduced.

## 23. Out of Scope

- Real courier API integrations (only mocks).
- Complex shipping route optimization.
- Multi-package shipping (single shipment per order for now).

## 24. Decisions and Open Questions

- **Decision:** Using synchronous calls to Tracking Service; if it fails, the shipment is still created, but the tracking record is queued/retried.
- **Assumption:** `orderId` is provided by the Order Service and is unique.

## 25. Recommended Git Branch

Suggested Branch: `feature/shipping-service`
Please switch to this branch before starting implementation.
