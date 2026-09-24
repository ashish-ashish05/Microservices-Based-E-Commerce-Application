# 12. Order Service Specification

## 1. Document Status

- Service: Order Service
- Build Order: 12
- Assignment: Order Management and Orchestration
- Status: Proposed
- Scope: Specification only

## 2. Purpose

The Order Service is the central orchestrator for the checkout process. It transforms a user's shopping cart into a formal order and coordinates the necessary distributed steps (inventory reservation, payment processing, and shipping) to fulfill that order. It manages the order lifecycle from creation to completion or cancellation.

## 3. Responsibilities

- Create orders from Cart data.
- Manage Order items and total amounts.
- Maintain the Order lifecycle (OrderStatus).
- Coordinate synchronous orchestration of:
    - Inventory reservation.
    - Payment processing.
    - Shipping creation.
- Handle compensation (undoing previous steps) when a step in the synchronous chain fails.
- Provide order history and status lookups for users.
- Cancel orders and release associated resources.

## 4. Non-Responsibilities

- **Cart Management:** It does not manage the active shopping cart; it consumes it.
- **Payment Processing:** It does not process credit cards; it calls the Payment Service.
- **Inventory Tracking:** It does not track stock levels; it requests reservations from the Inventory Service.
- **Shipping Logistics:** It does not handle courier logistics; it requests shipments from the Shipping Service.
- **JWT Generation:** It does not handle authentication tokens.

## 5. Service Boundary

- **Data Ownership:** Owns `Order`, `OrderItem`, and `OrderStatus`.
- **Domain Ownership:** Owns the "Order Fulfillment" domain.
- **External Dependencies:** Dependent on Cart, Inventory, Payment, and Shipping services.
- **Boundary Logic:** The boundary exists to encapsulate the complex orchestration logic (Saga-style) required to ensure consistency across multiple microservices without using a message broker.

## 6. Architecture

Client
  |
API Gateway
  |
Order Service
  |-------------------------------------------------------------+
  |                      |                    |                 |
Cart Service    Inventory Service    Payment Service    Shipping Service

## 7. Service-to-Service Communication

| Consumer | Provider | Method | Endpoint | Purpose | Failure Consideration |
|----------|----------|--------|----------|---------|-----------------------|
| Order | Cart | GET | `/carts/{cartId}` | Retrieve items for order creation | Circuit Breaker; Fallback: Order creation failed |
| Order | Inventory | POST | `/inventory/reserve` | Reserve stock for items | Timeout/Retry; Compensation: Release stock if payment fails |
| Order | Payment | POST | `/payments/process` | Process payment for order | Idempotency; Compensation: Release inventory if payment fails |
| Order | Shipping | POST | `/shipping/create` | Create shipment for paid order | Circuit Breaker; Fallback: Mark order as 'Payment-Success/Shipping-Pending' |

**Synchronous Orchestration Policy:**
- **Timeouts:** All remote calls must have strict read/connect timeouts to prevent thread exhaustion.
- **Circuit Breakers:** Used for all downstream services to avoid cascading failures.
- **Compensation:** If `Payment` fails, `Inventory.release` must be called. If `Shipping` fails, the system must decide between retrying or marking for manual intervention.

## 8. API Specification

### Create Order

- Method: `POST`
- Path: `/orders`
- Purpose: Converts a cart into an order and triggers the fulfillment workflow.
- Authentication: JWT Required
- Authorization: Role `USER`
- Request body: `{ "cartId": "uuid" }`
- Success response: `201 Created` with `OrderResponse` DTO.
- Error responses: `400 Bad Request` (Empty cart), `422 Unprocessable Entity` (Insufficient stock), `500 Internal Server Error`.

### Get Order Details

- Method: `GET`
- Path: `/orders/{orderId}`
- Purpose: Retrieve current status and items of an order.
- Authentication: JWT Required
- Authorization: `USER` (Own order) or `ADMIN`
- Success response: `200 OK` with `OrderResponse` DTO.
- Error responses: `404 Not Found`.

### Cancel Order

- Method: `POST`
- Path: `/orders/{orderId}/cancel`
- Purpose: Cancel an order and release reserved inventory.
- Authentication: JWT Required
- Authorization: `USER` (Own order) or `ADMIN`
- Success response: `200 OK`.
- Error responses: `400 Bad Request` (Cannot cancel shipped order).

## 9. Data Model

### Order Entity
| Field | Type | Required | Constraints | Description |
|------|------|----------|-------------|-------------|
| id | UUID | Yes | PK | Unique order identifier |
| userId | UUID | Yes | Index | Reference to User Service |
| totalAmount | BigDecimal | Yes | > 0 | Final price of the order |
| status | OrderStatus | Yes | Not Null | CURRENT_STATUS (PENDING, PAID, SHIPPED, CANCELLED, FAILED) |
| createdAt | LocalDateTime | Yes | Not Null | Timestamp of creation |
| updatedAt | LocalDateTime | Yes | Not Null | Last update timestamp |

### OrderItem Entity
| Field | Type | Required | Constraints | Description |
|------|------|----------|-------------|-------------|
| id | UUID | Yes | PK | Item identifier |
| orderId | UUID | Yes | FK | Reference to Order entity |
| productId | UUID | Yes | Index | Reference to Product Service |
| quantity | Integer | Yes | > 0 | Quantity ordered |
| unitPrice | BigDecimal | Yes | > 0 | Price at time of order |

## 10. DTOs

### OrderRequest
- Purpose: Trigger order creation.
- Fields: `cartId` (UUID, required).

### OrderResponse
- Purpose: Provide order summary.
- Fields: `orderId` (UUID), `status` (String), `totalAmount` (BigDecimal), `items` (List of OrderItemResponse), `createdAt` (LocalDateTime).

### OrderItemResponse
- Purpose: Details of items in an order.
- Fields: `productId` (UUID), `quantity` (Integer), `unitPrice` (BigDecimal).

## 11. Business Rules

- An order cannot be created if the cart is empty.
- Order total must match the calculated price from the Cart/Pricing services.
- Inventory must be reserved *before* payment is processed.
- An order cannot transition to `SHIPPED` unless it is `PAID`.
- A `SHIPPED` order cannot be cancelled via the standard API.
- All reservation failures must result in an immediate `FAILED` order status.

## 12. Design Patterns

### Pattern: State Pattern
- Where: `OrderStatus` transitions.
- Why: Orders have a strict lifecycle.
- Problem it solves: Avoids complex `if-else` or `switch` blocks when determining valid transitions (e.g., can't go from `CANCELLED` to `SHIPPED`).
- Key participants: `OrderState` interface, Concrete state classes (`PendingState`, `PaidState`, etc.).

### Pattern: Command Pattern
- Where: Order Actions (Create, Cancel, Ship).
- Why: Encapsulates the request to perform an action.
- Problem it solves: Allows for easier implementation of undo/compensation logic.
- Key participants: `OrderCommand`, `OrderCommandExecutor`.

### Pattern: Orchestrator (Facade)
- Where: `OrderFulfillmentService`.
- Why: Coordinates multiple remote services.
- Problem it solves: Prevents business logic from leaking into the Controller and centralizes the "Saga" logic.
- Key participants: `OrderFulfillmentService`, Feign Clients.

## 13. Dependencies

### Required
- `spring-boot-starter-web`: REST APIs.
- `spring-boot-starter-data-jpa`: Order persistence.
- `spring-boot-starter-validation`: Input validation.
- `spring-cloud-starter-openfeign`: Communication with Cart, Inventory, Payment, Shipping.
- `spring-cloud-starter-circuitbreaker-resilience4j`: Handling downstream failures.
- `com.h2database:h2`: Local development DB.
- `org.projectlombok:lombok`: Boilerplate reduction.

### Optional / Later
- `spring-boot-starter-mail`: To send order confirmation emails via Notification Service.

## 14. Configuration

```properties
server.port=8085
spring.application.name=order-service
# Remote Service URLs
services.cart.url=http://localhost:8086
services.inventory.url=http://localhost:8083
services.payment.url=http://localhost:8087
services.shipping.url=http://localhost:8088
```

## 15. Security

- **Authentication:** All endpoints require a valid JWT provided by the API Gateway.
- **Authorization:** 
    - Create/Get/Cancel: `USER` (must match `userId` in Order) or `ADMIN`.
    - Admin overrides: `ADMIN` can view all orders.
- **Sensitive Info:** Order details are sensitive; ensure logs do not print full PII of the user.

## 16. Exception and Error Handling

- `OrderNotFoundException`: `404 Not Found`
- `InsufficientStockException`: `422 Unprocessable Entity` (triggered by Inventory Service).
- `PaymentFailedException`: `402 Payment Required` or `422`.
- `InvalidOrderStateException`: `400 Bad Request` (e.g., cancelling a shipped order).
- `DownstreamServiceException`: `503 Service Unavailable` (Circuit breaker open).

## 17. Transaction Boundaries

- **Local Transaction:** Saving the `Order` and `OrderItem` entities in the Order DB must be atomic.
- **Distributed Workflow:** The orchestration across Inventory $\rightarrow$ Payment $\rightarrow$ Shipping is NOT a single transaction. Consistency is achieved through synchronous compensation (e.g., if Payment fails, call Inventory to release stock).

## 18. Validation

- `cartId` must be a valid UUID.
- `quantity` in order items must be $> 0$.
- Order total must be positive.
- State transitions must follow: `PENDING` $\rightarrow$ `PAID` $\rightarrow$ `SHIPPED`.

## 19. Testing Strategy

### Unit Tests
- State transition logic (State Pattern).
- Command execution logic.
- Business rules for order totals.

### Controller Tests
- Validation of `OrderRequest`.
- Status codes for `404` and `400` scenarios.

### Repository Tests
- Persistence of `Order` and `OrderItem` relationship.

### Integration Tests
- End-to-end flow: Create Order $\rightarrow$ Mocked Remote Calls $\rightarrow$ Status `PAID`.

### Contract / Remote Tests
- OpenFeign clients mapping for all 4 dependent services.

### Failure Tests
- **Payment Failure:** Verify `Inventory.release` is called.
- **Inventory Timeout:** Verify order is marked `FAILED` and user is notified.
- **Shipping Down:** Verify order remains in `PAID` state with a retry flag.

## 20. Observability

- **Logs:** Log every state transition (`Order {id} transitioned from PENDING to PAID`).
- **Metrics:** Order creation rate, Payment failure rate, Average fulfillment time.
- **Correlation:** Pass `X-Correlation-ID` from Gateway through to all downstream services.

## 21. Implementation Sequence

1. Define `OrderStatus` enum and `Order`, `OrderItem` entities.
2. Implement `OrderRepository`.
3. Create Request/Response DTOs.
4. Implement `OrderState` and concrete state classes (State Pattern).
5. Create OpenFeign clients for Cart, Inventory, Payment, and Shipping services.
6. Implement `OrderFulfillmentService` (The Orchestrator) with the synchronous flow:
    - Call Cart $\rightarrow$ Save Order as `PENDING`.
    - Call Inventory (Reserve).
    - Call Payment (Process).
    - Call Shipping (Create).
7. Implement compensation logic (e.g., `releaseInventory` on payment failure).
8. Implement `OrderController` with endpoints for Create, Get, and Cancel.
9. Implement global exception handling for domain exceptions.
10. Configure `application.properties` and Resilience4j circuit breakers.
11. Write unit tests for state transitions.
12. Write integration tests for the fulfillment flow using `@MockBean` for Feign clients.
13. Run `./mvnw clean test`.

## 22. Acceptance Criteria

- [ ] `POST /orders` successfully orchestrates Inventory $\rightarrow$ Payment $\rightarrow$ Shipping.
- [ ] Failed payment triggers inventory release.
- [ ] State Pattern prevents invalid transitions (e.g., `CANCELLED` $\rightarrow$ `SHIPPED`).
- [ ] All remote calls use OpenFeign and are wrapped in Circuit Breakers.
- [ ] Database constraints for `Order` and `OrderItem` are enforced.
- [ ] No direct database access to other services.
- [ ] `GET /orders/{id}` returns correct status and items.
- [ ] `./mvnw clean test` passes.
- [ ] Dependency versions match `CLAUDE.md`.

## 23. Out of Scope

- Email notification implementation (handled by Notification Service).
- Asynchronous order processing (Kafka/RabbitMQ).
- Complex shipping calculations (handled by Shipping Service).

## 24. Decisions and Open Questions

- **Decision:** Synchronous orchestration is used per `CLAUDE.md`.
- **Assumption:** Inventory reservation is idempotent.
- **Question:** Should the Order Service handle automatic retries for Shipping, or mark it for manual admin intervention? (Proposed: Mark as `PAID` and log error for admin).

---

**Suggested Branch:** `feature/order-service`
Please switch to this branch before starting implementation.
