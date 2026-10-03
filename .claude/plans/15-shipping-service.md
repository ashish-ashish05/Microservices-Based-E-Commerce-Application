# 15. Shipping Service Implementation Plan

## 1. Plan Metadata

- Service: Shipping Service
- Build Order: 15
- Assignment: Logistics & Fulfillment
- Specification: .claude/specs/15-shipping-service.md
- Status: Planned
- Plan Generated: 2026-10-03
- Prerequisite Services: Order Service

## 2. Objective

The Shipping Service will manage the physical delivery of orders by creating shipments, assigning shipping methods, and integrating with a mock courier provider to generate tracking numbers.

## 3. Existing Context

The Shipping Service is called by the Order Service (Port 8085) as the final step in the fulfillment orchestration. It provides tracking information to the Tracking Service (Port 8087).

## 4. Scope

### In Scope
- Shipment and ShippingMethod persistence.
- Implementation of the Adapter pattern for courier integration.
- Integration with Tracking Service via OpenFeign to initialize tracking.
- Implementation of the Strategy pattern for shipping cost/label generation.
- Global exception handling for 400, 404, and 502 errors.
- Unit, Controller, and Integration tests.

### Out of Scope
- Order lifecycle management (handled by Order Service).
- Payment processing.
- Real-time courier API integration (strictly mocked).
- User profile management.

## 5. Architecture

Client
  |
API Gateway
  |
Order Service
  |
Shipping Service
  |
+--> Mock Courier Provider
+--> Tracking Service (Synchronous REST)

The Shipping Service owns the `Shipment` and `ShippingMethod` entities. It acts as a bridge between the order and the physical delivery.

## 6. Implementation Dependencies

- **Maven Dependencies**:
    - `spring-boot-starter-web`
    - `spring-boot-starter-data-jpa`
    - `spring-boot-starter-validation`
    - `spring-cloud-starter-openfeign`
    - `spring-cloud-starter-circuitbreaker-resilience4j`
    - `com.h2database:h2`
    - `org.projectlombok:lombok`
    - `spring-boot-starter-actuator`
- **Prerequisite Services**:
    - Tracking Service (Running on port 8087)
- **Configuration**:
    - `server.port=8095`
    - `spring.datasource.url=jdbc:h2:mem:shipping-db`
    - `courier.api.base-url=http://mock-courier-api`

## 7. Data Model Plan

| Entity | Field | Type | Constraint | Notes |
|--------|-------|------|------------|-------|
| Shipment | id | UUID | Primary Key | Unique shipment ID |
| Shipment | orderId | UUID | Unique, Indexed | Reference to Order Service |
| Shipment | shippingMethodId | UUID | Foreign Key | Reference to ShippingMethod |
| Shipment | trackingNumber | String | Unique | Assigned by courier |
| Shipment | status | String | Not Null | PENDING, SHIPPED, DELIVERED, FAILED |
| Shipment | address | String | Not Null | Destination address |
| Shipment | createdAt | LocalDateTime | Not Null | Audit |
| Shipment | updatedAt | LocalDateTime | Not Null | Audit |
| ShippingMethod | id | UUID | Primary Key | Unique method ID |
| ShippingMethod | name | String | Not Null | e.g., "Express", "Standard" |
| ShippingMethod | cost | Double | >= 0 | Shipping cost |
| ShippingMethod | estimatedDays | Integer | > 0 | Delivery estimate |

## 8. API Implementation Plan

### Create Shipment
- HTTP method: `POST`
- path: `/api/shipments`
- request DTO: `ShipmentRequest` (orderId, shippingAddress, shippingMethodId, weight)
- validation: `shippingAddress` (@NotBlank), `weight` (@Positive)
- response DTO: `ShipmentResponse` (shipmentId, trackingNumber, status)
- status codes: `201 Created`, `400 Bad Request`, `404 Not Found` (Invalid method), `409 Conflict` (Duplicate order)
- authentication/authorization: Internal (Order Service only)
- downstream calls:
    1. Call `MockCourierAdapter` to get a tracking number.
    2. Call `TrackingServiceClient` to initialize tracking history.
- error behavior: Throw `DuplicateShipmentException` if `orderId` already has a shipment.

### Get Shipment Details
- HTTP method: `GET`
- path: `/api/shipments/{shipmentId}`
- request DTO: N/A
- validation: UUID format for `{shipmentId}`
- response DTO: `ShipmentResponse`
- status codes: `200 OK`, `404 Not Found`
- authentication/authorization: User (own) or ADMIN
- downstream calls: None
- error behavior: Throw `ShipmentNotFoundException`.

## 9. Service Layer Plan

1. **Create Shipment**:
    - Verify `shippingMethodId` exists in the database.
    - Check if a shipment already exists for the `orderId` (idempotency).
    - Call `MockCourierAdapter` to generate a tracking number.
    - Save the `Shipment` entity.
    - Call `TrackingServiceClient` to initialize the tracking record.
2. **Get Shipment**:
    - Retrieve `Shipment` by ID and map to `ShipmentResponse`.

## 10. Design Pattern Implementation Plan

### Adapter Pattern
- Where: `CourierAdapter` (Interface) and `MockCourierAdapter` (Implementation).
- Why: To decouple the system from specific courier API formats.
- Main participants: `CourierAdapter`, `MockCourierAdapter`.

### Strategy Pattern
- Where: Shipping cost calculation or label generation.
- Why: To support different calculations based on `ShippingMethod`.
- Main participants: `ShippingCostStrategy` and its implementations.

## 11. Remote Communication Plan

| Caller | Provider | Endpoint | Purpose | Failure Handling |
|--------|----------|----------|---------|------------------|
| Shipping | Tracking | `/api/tracking` | Initialize tracking | Retry on 5xx, Circuit Breaker |

- **Timeout**: 2-second read timeout.
- **Retry**: Idempotent updates to Tracking Service are retried.
- **Circuit Breaker**: Resilience4j used for the courier integration point.

## 12. Exception Handling Plan

- **ShipmentNotFoundException**: 404 Not Found.
- **InvalidShippingMethodException**: 400 Bad Request.
- **CourierIntegrationException**: 502 Bad Gateway.
- **DuplicateShipmentException**: 409 Conflict.
- **Global Exception Handler**: Standardized JSON response.

## 13. Security Plan

- **Authentication**: Valid JWT required for `GET` endpoints.
- **Authorization**: `ADMIN` or order owner.
- **Sensitive Data**: Mask customer addresses in debug logs.

## 14. Testing Plan

### Unit Tests
- Business rules for shipment status transitions.
- Courier Adapter mapping logic.

### Controller Tests
- Validate `ShipmentRequest` constraints and response status codes.

### Repository Tests
- Test unique constraint on `orderId` in `Shipment` entity.

### Integration Tests
- End-to-end flow: Create shipment $\rightarrow$ Mock Courier $\rightarrow$ Save Tracking Number.

### Failure Tests
- Simulate Courier API timeout and verify Resilience4j fallback.
- Attempt to create duplicate shipments for the same order.

## 15. Files to Create

```text
src/main/java/com/ecommerce/shipping_service/
├── ShippingServiceApplication.java
├── controller/ShippingController.java
├── service/ShippingService.java
├── service/ShippingServiceImpl.java
├── repository/ShippingRepository.java
├── repository/ShippingMethodRepository.java
├── entity/Shipment.java
├── entity/ShippingMethod.java
├── dto/ShipmentRequest.java
├── dto/ShipmentResponse.java
├── adapter/CourierAdapter.java
├── adapter/MockCourierAdapter.java
├── client/TrackingServiceClient.java
└── exception/ShipmentNotFoundException.java
└── exception/GlobalExceptionHandler.java
```

## 16. Files to Modify

None.

## 17. Implementation Sequence

1. Update Maven project with required dependencies.
2. Create `Shipment` and `ShippingMethod` entities.
3. Create `ShipmentRepository` and `ShippingMethodRepository`.
4. Create request/response DTOs.
5. Implement `CourierAdapter` and `MockCourierAdapter`.
6. Implement `ShippingService` business logic.
7. Implement `ShippingController`.
8. Implement OpenFeign client for `Tracking Service`.
9. Implement Resilience4j circuit breaker for courier integration.
10. Implement global exception handling.
11. Add unit, controller, and integration tests.
12. Run Maven verification.

## 18. Verification Plan

- Run `./mvnw clean test`.
- Run `./mvnw help:effective-pom`.

## 19. Risks and Failure Scenarios

- **Courier API Down**: Handled by circuit breaker and fallback to "Pending Shipment" status.
- **Duplicate Shipments**: Prevented by unique constraint on `orderId`.
- **Invalid Address**: Handled by validation errors.

## 20. Acceptance Criteria

- [ ] `POST /api/shipments` creates a shipment and assigns a tracking number from mock courier.
- [ ] Duplicate shipment requests for the same `orderId` are rejected.
- [ ] Circuit breaker triggers when mock courier API is unavailable.
- [ ] Shipment status transitions are validated.
- [ ] Shipping methods are looked up from the database.
- [ ] Integration with Tracking Service works via OpenFeign.
- [ ] `./mvnw clean test` passes.
- [ ] No Kafka/RabbitMQ introduced.

## 21. Planning Issues

None.

## 22. Implementation Prompt

> Implement `Shipping Service` according to `.claude/specs/15-shipping-service.md` and this plan. Read the root `CLAUDE.md`. Inspect the Tracking Service to understand its contract. Follow the implementation sequence and acceptance criteria. Do not introduce Kafka/RabbitMQ. Verify the build and tests before completion.
