# 14. Transaction Service Implementation Plan

## 1. Plan Metadata

- Service: Transaction Service
- Build Order: 14
- Assignment: Payment Integration Phase
- Specification: .claude/specs/14-transaction-service.md
- Status: Planned
- Plan Generated: 2026-10-03
- Prerequisite Services: Payment Service

## 2. Objective

The Transaction Service will maintain a durable, audit-friendly record of all payment transactions, tracking the lifecycle state of each transaction and providing a history of attempts for reconciliation and financial auditing.

## 3. Existing Context

The Transaction Service is a provider service, primarily called by the Payment Service (Port 8087) to record the outcome of payment attempts. It is also used by the Order Service (Port 8085) to audit payment status for specific orders.

## 4. Scope

### In Scope
- Immutable transaction record storage (Create, Read).
- Transaction status management (PENDING, COMPLETED, FAILED, REFUNDED).
- History retrieval by order ID.
- Implementation of the State pattern for status transitions.
- Implementation of Repository and DTO patterns.
- Global exception handling for 404 and 400 errors.
- Unit, Controller, and Integration tests.

### Out of Scope
- Processing payments with external providers (handled by Payment Service).
- Cart or Order management.
- User authentication.

## 5. Architecture

Client
  |
API Gateway
  |
Payment Service
  |
Transaction Service

The Transaction Service owns the `Transaction` entity. It acts as a financial ledger, recording every event that occurs in the Payment Service.

## 6. Implementation Dependencies

- **Maven Dependencies**:
    - `spring-boot-starter-web`
    - `spring-boot-starter-data-jpa`
    - `spring-boot-starter-validation`
    - `com.h2database:h2`
    - `org.projectlombok:lombok`
    - `spring-boot-starter-actuator`
- **Configuration**:
    - `server.port=8086`
    - `spring.datasource.url=jdbc:h2:mem:transaction-db`

## 7. Data Model Plan

| Entity | Field | Type | Constraint | Notes |
|--------|-------|------|------------|-------|
| Transaction | id | UUID | Primary Key | Internal ID |
| Transaction | orderId | UUID | Indexed | Reference to Order Service |
| Transaction | amount | BigDecimal | > 0 | Transaction amount |
| Transaction | currency | String | Length 3 | ISO currency code |
| Transaction | status | TransactionStatus | Not Null | PENDING, COMPLETED, FAILED, REFUNDED |
| Transaction | paymentMethod | String | Not Null | e.g., CREDIT_CARD, PAYPAL |
| Transaction | providerTransactionId | String | Unique | ID from external gateway |
| Transaction | createdAt | LocalDateTime | Not Null | Audit |
| Transaction | updatedAt | LocalDateTime | Not Null | Audit |
| Transaction | remarks | String | - | Audit notes |

## 8. API Implementation Plan

### Create Transaction
- HTTP method: `POST`
- path: `/api/transactions`
- request DTO: `TransactionRequest` (orderId, amount, currency, paymentMethod, providerTransactionId)
- validation: `amount` (@Positive), `orderId` (@NotNull)
- response DTO: `TransactionResponse`
- status codes: `201 Created`, `400 Bad Request`
- authentication/authorization: Internal (Payment Service)
- downstream calls: None
- error behavior: Throw `ValidationException` for invalid request data.

### Update Transaction Status
- HTTP method: `PUT`
- path: `/api/transactions/{id}/status`
- request DTO: `StatusUpdateRequest` (status, providerReference, remarks)
- validation: `status` (@NotNull)
- response DTO: N/A
- status codes: `200 OK`, `404 Not Found`, `400 Bad Request`
- authentication/authorization: Internal (Payment Service)
- downstream calls: None
- error behavior: Throw `InvalidStateTransitionException` for illegal moves.

### Get Transactions by Order
- HTTP method: `GET`
- path: `/api/transactions/order/{orderId}`
- request DTO: N/A
- validation: UUID format for `{orderId}`
- response DTO: `List<TransactionResponse>`
- status codes: `200 OK`, `404 Not Found`
- authentication/authorization: Internal (Order/Payment Service)
- downstream calls: None
- error behavior: Return empty list if no transactions exist for the order.

## 9. Service Layer Plan

1. **Create Transaction**:
    - Validate the request.
    - Save the `Transaction` entity with status `PENDING`.
2. **Update Status**:
    - Retrieve the `Transaction` by ID.
    - Use the State pattern to verify if the transition to the new status is legal.
    - Update the status and save the entity.
3. **Get History**:
    - Retrieve all `Transaction` entities associated with the `orderId`.
    - Map to `TransactionResponse`.

## 10. Design Pattern Implementation Plan

### State Pattern
- Where: `TransactionStatus` transitions.
- Why: To strictly control valid transitions (e.g., `COMPLETED` cannot move back to `PENDING`).
- Main participants: `TransactionStatus` enum and `TransactionService` transition logic.

### Repository Pattern
- Where: `TransactionRepository`
- Why: Standard JPA abstraction.
- Main participants: `TransactionRepository` extending `JpaRepository`.

### DTO Pattern
- Where: `TransactionRequest`, `TransactionResponse`, `StatusUpdateRequest`
- Why: Decouples internal entity from API contract.
- Main participants: `TransactionRequest`, `TransactionResponse`, `StatusUpdateRequest`.

## 11. Remote Communication Plan

No remote calls are made by the Transaction Service.

## 12. Exception Handling Plan

- **TransactionNotFoundException**: Thrown when `transactionId` is not found. Maps to 404 Not Found.
- **InvalidStateTransitionException**: Thrown when an illegal status update is requested. Maps to 400 Bad Request.
- **Global Exception Handler**: Standardized JSON response.

## 13. Security Plan

- **Authentication**: Restricted to internal service-to-service calls.
- **Authorization**: Only the Payment Service should be allowed to update statuses.
- **Sensitive Data**: No raw provider response payloads are logged.

## 14. Testing Plan

### Unit Tests
- `TransactionService` state transition logic.
- Validation of amount and currency.

### Controller Tests
- Verify `POST /api/transactions` returns 201.
- Verify `PUT /api/transactions/{id}/status` returns 200 on success and 400 on invalid transition.

### Repository Tests
- Verify persistence and retrieval of transaction records.
- Verify uniqueness of `providerTransactionId`.

### Integration Tests
- End-to-end flow: Create $\rightarrow$ Update Status $\rightarrow$ Get by Order.

## 15. Files to Create

```text
src/main/java/com/ecommerce/transaction_service/
├── TransactionServiceApplication.java
├── controller/TransactionController.java
├── service/TransactionService.java
├── service/TransactionServiceImpl.java
├── repository/TransactionRepository.java
├── entity/Transaction.java
├── entity/TransactionStatus.java
├── dto/TransactionRequest.java
├── dto/TransactionResponse.java
├── dto/StatusUpdateRequest.java
└── exception/TransactionNotFoundException.java
└── exception/InvalidStateTransitionException.java
└── exception/GlobalExceptionHandler.java
```

## 16. Files to Modify

None.

## 17. Implementation Sequence

1. Update Maven project dependencies.
2. Create `Transaction` entity and `TransactionStatus` enum.
3. Create `TransactionRepository`.
4. Create Request/Response DTOs.
5. Implement `TransactionService` with state transition logic.
6. Implement `TransactionController` endpoints.
7. Implement global exception handling.
8. Add unit and integration tests.
9. Run `./mvnw clean test` and verify.

## 18. Verification Plan

- Run `./mvnw clean test`.
- Run `./mvnw help:effective-pom`.

## 19. Risks and Failure Scenarios

- **Invalid State Transition**: A request to move a transaction to an illegal state. Handled by `InvalidStateTransitionException`.
- **Duplicate Provider ID**: Attempting to record a transaction with a duplicate `providerTransactionId`. Handled by unique constraint.

## 20. Acceptance Criteria

- [ ] `POST /api/transactions` correctly initializes a record.
- [ ] `PUT /api/transactions/{id}/status` enforces valid state transitions.
- [ ] `GET /api/transactions/order/{orderId}` returns all attempts for an order.
- [ ] Database constraints are enforced.
- [ ] No direct access to Payment or Order databases.
- [ ] All tests pass with `./mvnw clean test`.
- [ ] No Kafka/RabbitMQ introduced.

## 21. Planning Issues

None.

## 22. Implementation Prompt

> Implement `Transaction Service` according to `.claude/specs/14-transaction-service.md` and this plan. Read the root `CLAUDE.md`. Follow the implementation sequence and acceptance criteria. Do not introduce Kafka/RabbitMQ. Verify the build and tests before completion.
