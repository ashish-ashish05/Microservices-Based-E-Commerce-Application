# 13. Payment Service Implementation Plan

## 1. Plan Metadata

- Service: Payment Service
- Build Order: 13
- Assignment: Payment Processing
- Specification: .claude/specs/13-payment-service.md
- Status: Planned
- Plan Generated: 2026-10-03
- Prerequisite Services: None

## 2. Objective

The Payment Service will handle financial transactions for orders, integrating with an external payment provider (initially mocked) and ensuring that payments are processed idempotently to prevent double-charging.

## 3. Existing Context

The Payment Service is called by the Order Service (Port 8085) to process a payment for a specific order.

## 4. Scope

### In Scope
- Payment attempt and status management (PENDING, SUCCESS, FAILED).
- Integration with a mock payment provider via the Adapter pattern.
- Implementation of the Strategy pattern for different payment methods (Credit Card, PayPal).
- Idempotency enforcement based on the `idempotencyKey` provided by the Order Service.
- Implementation of Repository and DTO patterns.
- Global exception handling for 400, 404, and 409 errors.
- Unit, Controller, and Integration tests.

### Out of Scope
- Order management (handled by Order Service).
- Payment reconciliation (handled by Transaction Service).
- User balance management.
- Integration with real payment gateways.

## 5. Architecture

Client
  |
API Gateway
  |
Order Service (Orchestrator)
  |
Payment Service
  |
[External Payment Provider (Mocked)]

The Payment Service owns the `Payment` entity. It uses a `PaymentProviderAdapter` to abstract the external API.

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
    - `spring.datasource.url=jdbc:h2:mem:payment-db`
    - `payment.provider.api-key=internal-mock-key`

## 7. Data Model Plan

| Entity | Field | Type | Constraint | Notes |
|--------|-------|------|------------|-------|
| Payment | id | UUID | Primary Key | Internal ID |
| Payment | orderId | UUID | Indexed | Reference to Order Service |
| Payment | amount | BigDecimal | > 0 | Transaction amount |
| Payment | currency | String | 3 chars | ISO code |
| Payment | status | Enum | Not Null | PENDING, SUCCESS, FAILED |
| Payment | providerTxnId | String | Unique | ID from the provider |
| Payment | idempotencyKey | String | Unique | Prevents double-charging |
| Payment | createdAt | LocalDateTime | Not Null | Audit |
| Payment | updatedAt | LocalDateTime | Not Null | Audit |

## 8. API Implementation Plan

### Process Payment
- HTTP method: `POST`
- path: `/api/payments`
- request DTO: `PaymentRequest` (orderId, amount, currency, paymentMethod, idempotencyKey)
- validation: `amount` (@Positive), `orderId` (@NotNull), `idempotencyKey` (@NotBlank)
- response DTO: `PaymentResponse` (paymentId, status, transactionId)
- status codes: `201 Created`, `400 Bad Request`, `409 Conflict` (Duplicate key)
- authentication/authorization: Internal (Order Service only)
- downstream calls: Call to `MockPaymentProviderAdapter`.
- error behavior: Return 409 if the idempotency key already exists.

### Get Payment Status
- HTTP method: `GET`
- path: `/api/payments/{paymentId}`
- request DTO: N/A
- validation: UUID format for `{paymentId}`
- response DTO: `PaymentStatusResponse` (paymentId, orderId, amount, status)
- status codes: `200 OK`, `404 Not Found`
- authentication/authorization: Internal
- downstream calls: None
- error behavior: Throw `PaymentNotFoundException`.

## 9. Service Layer Plan

1. **Process Payment**:
    - Check if `idempotencyKey` already exists in the database. If yes, return existing `PaymentResponse`.
    - Create a `Payment` record with status `PENDING`.
    - Use `PaymentStrategy` to determine the processing logic for the `paymentMethod`.
    - Call the `PaymentProviderAdapter` to perform the actual transaction.
    - Update payment status to `SUCCESS` or `FAILED` based on provider response.
    - Return the `PaymentResponse`.
2. **Get Status**:
    - Retrieve `Payment` by ID and map to `PaymentStatusResponse`.

## 10. Design Pattern Implementation Plan

### Adapter Pattern
- Where: `PaymentProviderAdapter` (Interface) and `MockPaymentProviderAdapter` (Implementation).
- Why: To decouple the core payment logic from the mock provider API.
- Main participants: `PaymentProviderAdapter`, `MockPaymentProviderAdapter`.

### Strategy Pattern
- Where: `PaymentStrategy` (Interface) and `CreditCardPaymentStrategy`, `PaypalPaymentStrategy`.
- Why: To handle different payment methods cleanly.
- Main participants: `PaymentStrategy`, `PaymentStrategyFactory`.

### Repository Pattern
- Where: `PaymentRepository`
- Why: Standard JPA abstraction.
- Main participants: `PaymentRepository` extending `JpaRepository`.

## 11. Remote Communication Plan

No remote calls are made to other microservices, only to an external provider mock.

- **Timeout**: Strict 5-second timeout for provider calls.
- **Retry**: No retries for business failures (insufficient funds).
- **Circuit Breaker**: Resilience4j on the `MockPaymentProviderAdapter` call.

## 12. Exception Handling Plan

- **PaymentNotFoundException**: 404 Not Found.
- **DuplicatePaymentException**: 409 Conflict.
- **ProviderUnavailableException**: 502 Bad Gateway.
- **Global Exception Handler**: Standardized JSON response.

## 13. Security Plan

- **Authentication**: Internal service-to-service authentication.
- **Authorization**: Only the Order Service can initiate payments.
- **Sensitive Data**: Do NOT store CVVs or full credit card numbers. Use tokens.
- **Logging**: Mask transaction IDs in logs.

## 14. Testing Plan

### Unit Tests
- `PaymentService` business logic (idempotency, status transitions).
- `PaymentStrategy` implementations.
- `MockPaymentProviderAdapter` behavior.

### Controller Tests
- Request validation and response status codes.

### Repository Tests
- H2 persistence of `Payment` entity and unique constraint on `idempotencyKey`.

### Integration Tests
- End-to-end flow: Request $\rightarrow$ Mock Provider $\rightarrow$ Database $\rightarrow$ Response.

### Failure Tests
- Mock provider timeout $\rightarrow$ check status is `FAILED` or `PENDING`.
- Duplicate request with same idempotency key $\rightarrow$ 409 Conflict.

## 15. Files to Create

```text
src/main/java/com/ecommerce/payment_service/
├── PaymentServiceApplication.java
├── controller/PaymentController.java
├── service/PaymentService.java
├── service/PaymentServiceImpl.java
├── repository/PaymentRepository.java
├── entity/Payment.java
├── entity/PaymentStatus.java
├── dto/PaymentRequest.java
├── dto/PaymentResponse.java
├── dto/PaymentStatusResponse.java
├── adapter/PaymentProviderAdapter.java
├── adapter/MockPaymentProviderAdapter.java
├── strategy/PaymentStrategy.java
├── strategy/CreditCardPaymentStrategy.java
├── strategy/PaypalPaymentStrategy.java
├── strategy/PaymentStrategyFactory.java
└── exception/PaymentNotFoundException.java
└── exception/DuplicatePaymentException.java
└── exception/GlobalExceptionHandler.java
```

## 16. Files to Modify

None.

## 17. Implementation Sequence

1. Update `pom.xml` with required dependencies.
2. Create `Payment` entity and `PaymentStatus` enum.
3. Create `PaymentRepository`.
4. Create Request/Response DTOs.
5. Implement `PaymentProviderAdapter` and `MockPaymentProviderAdapter`.
6. Implement `PaymentStrategy` and its implementations.
7. Implement `PaymentService` business logic (including idempotency).
8. Implement `PaymentController`.
9. Implement global exception handling.
10. Add `application.properties` configuration.
11. Write unit, repository, and controller tests.
12. Implement integration tests.
13. Run `./mvnw clean test`.

## 18. Verification Plan

- Run `./mvnw clean test`.
- Run `./mvnw help:effective-pom`.

## 19. Risks and Failure Scenarios

- **Provider Timeout**: Handled by marking the payment as `PENDING` and returning a timeout error.
- **Double Charging**: Prevented by the `idempotencyKey` unique constraint.
- **Race Conditions**: Handled by `@Transactional` on the payment process.

## 20. Acceptance Criteria

- [ ] `POST /api/payments` creates a payment and returns 201.
- [ ] `GET /api/payments/{id}` returns the correct payment status.
- [ ] Duplicate `idempotencyKey` returns 409 Conflict.
- [ ] Payment status cannot move from `SUCCESS` to `FAILED`.
- [ ] External provider is abstracted via an Adapter.
- [ ] No direct access to Order Service database.
- [ ] H2 database stores payment records.
- [ ] All tests pass via `./mvnw clean test`.
- [ ] No Kafka/RabbitMQ introduced.

## 21. Planning Issues

None.

## 22. Implementation Prompt

> Implement `Payment Service` according to `.claude/specs/13-payment-service.md` and this plan. Read the root `CLAUDE.md`. Follow the implementation sequence and acceptance criteria. Do not introduce Kafka/RabbitMQ. Verify the build and tests before completion.
