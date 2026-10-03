# 12. Order Service Implementation Plan

## 1. Plan Metadata

- Service: Order Service
- Build Order: 12
- Assignment: Order Management and Orchestration
- Specification: .claude/specs/12-order-service.md
- Status: Planned
- Plan Generated: 2026-10-03
- Prerequisite Services: Cart Service, Inventory Service, Payment Service, Shipping Service

## 2. Objective

The Order Service will act as the central orchestrator for the checkout process, converting a shopping cart into an order and managing a distributed synchronous workflow (Inventory $\rightarrow$ Payment $\rightarrow$ Shipping) with associated compensation logic.

## 3. Existing Context

The Order Service is the most complex service in terms of coordination. It depends on the Cart Service (Port 8086) to retrieve order items, the Inventory Service (Port 8083) to reserve stock, the Payment Service (Port 8087) to process payment, and the Shipping Service (Port 8095) to create shipments.

## 4. Scope

### In Scope
- Order and OrderItem persistence.
- Implementation of the Order lifecycle (State Pattern).
- Synchronous orchestration of fulfillment (Inventory $\rightarrow$ Payment $\rightarrow$ Shipping).
- Implementation of compensation logic (Saga-style synchronous compensation).
- Command Pattern for order actions (Create, Cancel).
- Global exception handling for distributed failures.
- Unit, Controller, and Integration tests.

### Out of Scope
- Cart management (handled by Cart Service).
- Actual payment processing (handled by Payment Service).
- Actual shipping logistics (handled by Shipping Service).
- Email notifications (handled by Notification Service).

## 5. Architecture

Client
  |
API Gateway
  |
Order Service
  |-------------------------------------------------------------+
  |                      |                    |                 |
Cart Service    Inventory Service    Payment Service    Shipping Service

The Order Service owns the `Order` and `OrderItem` entities. It manages the distributed state of the order and ensures consistency through compensation.

## 6. Implementation Dependencies

- **Maven Dependencies**:
    - `spring-boot-starter-web`
    - `spring-boot-starter-data-jpa`
    - `spring-boot-starter-validation`
    - `spring-cloud-starter-openfeign`
    - `spring-cloud-starter-circuitbreaker-resilience4j`
    - `com.h2database:h2`
    - `org.projectlombok:lombok`
- **Prerequisite Services**:
    - Cart Service (Port 8086)
    - Inventory Service (Port 8083)
    - Payment Service (Port 8087)
    - Shipping Service (Port 8095)
- **Configuration**:
    - `server.port=8085`
    - `spring.datasource.url=jdbc:h2:mem:order-db`

## 7. Data Model Plan

| Entity | Field | Type | Constraint | Notes |
|--------|-------|------|------------|-------|
| Order | id | UUID | Primary Key | Unique Order ID |
| Order | userId | UUID | Indexed | Reference to User Service |
| Order | totalAmount | BigDecimal | > 0 | Final price |
| Order | status | OrderStatus | Not Null | PENDING, PAID, SHIPPED, CANCELLED, FAILED |
| Order | createdAt | LocalDateTime | Not Null | Audit |
| Order | updatedAt | LocalDateTime | Not Null | Audit |
| OrderItem | id | UUID | Primary Key | Unique Item ID |
| OrderItem | orderId | UUID | FK | Reference to Order |
| OrderItem | productId | UUID | Indexed | Reference to Product Service |
| OrderItem | quantity | Integer | > 0 | Number of items |
| OrderItem | unitPrice | BigDecimal | > 0 | Price at time of order |

## 8. API Implementation Plan

### Create Order
- HTTP method: `POST`
- path: `/orders`
- request DTO: `OrderRequest` (cartId)
- validation: `cartId` (@NotNull)
- response DTO: `OrderResponse`
- status codes: `201 Created`, `400 Bad Request` (Empty cart), `422 Unprocessable Entity` (Insufficient stock), `500 Internal Server Error`
- authentication/authorization: User
- downstream calls:
    1. `GET /carts/{cartId}` (Cart Service)
    2. `POST /inventory/reserve` (Inventory Service)
    3. `POST /payments/process` (Payment Service)
    4. `POST /api/shipments` (Shipping Service)
- error behavior: Trigger compensation (Release Inventory) if Payment fails.

### Get Order Details
- HTTP method: `GET`
- path: `/orders/{orderId}`
- request DTO: N/A
- validation: UUID format for `{orderId}`
- response DTO: `OrderResponse`
- status codes: `200 OK`, `404 Not Found`
- authentication/authorization: User (own) or ADMIN
- downstream calls: None
- error behavior: Throw `OrderNotFoundException`.

### Cancel Order
- HTTP method: `POST`
- path: `/orders/{orderId}/cancel`
- request DTO: N/A
- validation: UUID format for `{orderId}`
- response DTO: N/A
- status codes: `200 OK`, `400 Bad Request` (Shipped order)
- authentication/authorization: User (own) or ADMIN
- downstream calls: `PUT /inventory/{productId}/release` (Inventory Service)
- error behavior: Release all items if order is cancelled.

## 9. Service Layer Plan

1. **Order Creation Workflow (Orchestrator)**:
    - Retrieve cart items.
    - Create `Order` as `PENDING`.
    - **Step 1: Reserve Inventory**: Call Inventory Service. If failure $\rightarrow$ Order `FAILED`.
    - **Step 2: Process Payment**: Call Payment Service. If failure $\rightarrow$ Call Inventory Service to release stock $\rightarrow$ Order `FAILED`.
    - **Step 3: Create Shipment**: Call Shipping Service. If failure $\rightarrow$ Mark order as `PAID` but Shipping Pending (log error).
    - Mark order as `PAID` or `SHIPPED`.
2. **Order Cancellation**:
    - Verify order status (cannot cancel `SHIPPED`).
    - Call Inventory Service to release all reserved items.
    - Update status to `CANCELLED`.

## 10. Design Pattern Implementation Plan

### State Pattern
- Where: `OrderStatus` transitions.
- Why: To strictly manage the lifecycle of an order.
- Main participants: `OrderState` interface and concrete states (`PendingState`, `PaidState`, `ShippedState`, `CancelledState`, `FailedState`).

### Command Pattern
- Where: `OrderAction` commands.
- Why: To encapsulate the logic for order creation, cancellation, and shipping.
- Main participants: `OrderCommand` and `OrderCommandExecutor`.

### Orchestrator (Facade)
- Where: `OrderFulfillmentService`.
- Why: Centralizes the synchronous coordination of four different services.
- Main participants: `OrderFulfillmentService`, Feign Clients.

## 11. Remote Communication Plan

| Caller | Provider | Endpoint | Purpose | Failure Handling |
|--------|----------|----------|---------|------------------|
| Order | Cart | `/carts/{cartId}` | Get cart items | Circuit Breaker $\rightarrow$ 400 Bad Request |
| Order | Inventory | `/inventory/reserve` | Reserve items | Timeout $\rightarrow$ Release Inventory |
| Order | Payment | `/payments/process` | Process payment | Circuit Breaker $\rightarrow$ Release Inventory |
| Order | Shipping | `/api/shipments` | Create shipment | Circuit Breaker $\rightarrow$ Mark as "Paid/Shipping Pending" |

- **Timeout**: Strict 2-second read timeout.
- **Retry**: Idempotent requests only.
- **Circuit Breaker**: Resilience4j on all calls.

## 12. Exception Handling Plan

- **OrderNotFoundException**: 404 Not Found.
- **InsufficientStockException**: 422 Unprocessable Entity.
- **PaymentFailedException**: 402 Payment Required or 422.
- **InvalidOrderStateException**: 400 Bad Request.
- **Global Exception Handler**: Standardized JSON response.

## 13. Security Plan

- **Authentication**: Valid JWT required.
- **Authorization**: User (Own order) or ADMIN.
- **Sensitive Info**: Log order transitions, but mask PII in logs.

## 14. Testing Plan

### Unit Tests
- State transition logic (State Pattern).
- Command execution logic.
- Business rules for order totals.

### Controller Tests
- Request validation and status codes.

### Repository Tests
- `Order` and `OrderItem` persistence.

### Integration Tests
- End-to-end flow: Create Order $\rightarrow$ Mocked Remote Calls $\rightarrow$ Status `PAID`.

### Contract / Remote Tests
- OpenFeign client mapping for all 4 dependent services.

### Failure Tests
- **Payment Failure**: Verify `Inventory.release` is called.
- **Inventory Timeout**: Verify order is marked `FAILED`.
- **Shipping Down**: Verify order remains in `PAID` state.

## 15. Files to Create

```text
src/main/java/com/ecommerce/order_service/
├── OrderServiceApplication.java
├── controller/OrderController.java
├── service/OrderService.java
├── service/OrderServiceImpl.java
├── service/OrderFulfillmentService.java
├── repository/OrderRepository.java
├── repository/OrderItemRepository.java
├── entity/Order.java
├── entity/OrderItem.java
├── entity/OrderStatus.java
├── dto/OrderRequest.java
├── dto/OrderResponse.java
├── dto/OrderItemResponse.java
├── state/OrderState.java
├── state/PendingState.java
├── state/PaidState.java
├── state/ShippedState.java
├── state/CancelledState.java
├── state/FailedState.java
├── command/OrderCommand.java
├── command/CreateOrderCommand.java
├── command/CancelOrderCommand.java
├── client/CartServiceClient.java
├── client/InventoryServiceClient.java
├── client/PaymentServiceClient.java
├── client/ShippingServiceClient.java
└── exception/OrderNotFoundException.java
└── exception/GlobalExceptionHandler.java
```

## 16. Files to Modify

None.

## 17. Implementation Sequence

1. Define `OrderStatus` enum and `Order`, `OrderItem` entities.
2. Implement `OrderRepository` and `OrderItemRepository`.
3. Create Request/Response DTOs.
4. Implement `OrderState` and concrete state classes.
5. Create OpenFeign clients for all 4 dependent services.
6. Implement `OrderFulfillmentService` (the orchestrator).
7. Implement compensation logic for failures.
8. Implement `OrderController`.
9. Implement global exception handling.
10. Configure `application.properties` and Resilience4j.
11. Write unit and integration tests.
12. Run `./mvnw clean test`.

## 18. Verification Plan

- Run `./mvnw clean test`.
- Run `./mvnw help:effective-pom`.

## 19. Risks and Failure Scenarios

- **Downstream Service Timeout**: Can lead to thread exhaustion. Handled by strict timeouts and circuit breakers.
- **Distributed Inconsistency**: Payment succeeds but Shipping fails. Handled by marking for manual intervention.
- **Race Conditions**: Multiple requests to create order from same cart. Handled by `@Transactional`.

## 20. Acceptance Criteria

- [ ] `POST /orders` orchestrates Inventory $\rightarrow$ Payment $\rightarrow$ Shipping.
- [ ] Failed payment triggers inventory release.
- [ ] State Pattern prevents invalid transitions.
- [ ] All remote calls use OpenFeign and are wrapped in Circuit Breakers.
- [ ] Database constraints for `Order` and `OrderItem` are enforced.
- [ ] `GET /orders/{id}` returns correct status and items.
- [ ] `./mvnw clean test` passes.
- [ ] Dependency versions follow `CLAUDE.md`.

## 21. Planning Issues

None.

## 22. Implementation Prompt

> Implement `Order Service` according to `.claude/specs/12-order-service.md` and this plan. Read the root `CLAUDE.md`. Inspect the Cart, Inventory, Payment, and Shipping services to understand their contracts. Follow the implementation sequence and acceptance criteria. Do not introduce Kafka/RabbitMQ. Verify the build and tests before completion.
