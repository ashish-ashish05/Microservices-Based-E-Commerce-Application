# 13. Payment Service Specification

## 1. Document Status

- Service: Payment Service
- Build Order: 13
- Assignment: Payment Processing
- Status: Proposed
- Scope: Specification only

## 2. Purpose

The Payment Service is responsible for processing financial transactions for orders. It acts as the gateway between the e-commerce platform and external payment providers, ensuring that payments are captured securely, tracked accurately, and handled idempotently.

## 3. Responsibilities

- Create and manage payment attempts.
- Process payments through a payment provider (initially mocked).
- Track payment status (PENDING, SUCCESS, FAILED).
- Support idempotent payment requests to prevent double-charging.
- Handle payment failures and report them back to the Order Service.
- Provide a history of payment attempts for a given order.

## 4. Non-Responsibilities

- **Order Management**: Does not manage order items, shipping, or order status (handled by Order Service).
- **Payment Reconciliation**: Long-term audit and reconciliation are handled by the Transaction Service.
- **User Balance Management**: Does not manage user wallets or credits unless explicitly added as a requirement.
- **JWT Generation**: Does not handle authentication or token issuance.

## 5. Service Boundary

- **Data Ownership**: Owns `Payment` and `PaymentAttempt` entities.
- **Domain Ownership**: Owns the payment lifecycle and provider integration logic.
- **External Dependencies**: Depends on external payment gateways (via Adapter pattern).
- **Boundary Rationale**: Separating payment logic ensures that sensitive payment processing is isolated and can be scaled or updated (e.g., switching providers) without affecting the rest of the system.

## 6. Architecture

Client
  |
API Gateway
  |
Order Service (Orchestrator)
  |
Payment Service
  |
[External Payment Provider (Mocked)]

## 7. Service-to-Service Communication

| Consumer | Provider | Method | Endpoint | Purpose | Failure Consideration |
|----------|----------|--------|----------|---------|-----------------------|
| Order Service | Payment Service | POST | `/api/payments` | Initiate payment for an order | Timeout $\rightarrow$ Check status; Retry with Idempotency Key |
| Order Service | Payment Service | GET | `/api/payments/{paymentId}` | Check payment status | Circuit breaker on provider failure |

**Synchronous Communication Details:**
- **Timeouts**: Strict timeouts for provider calls to prevent Order Service from hanging.
- **Retry**: Retries allowed only for network errors; business failures (insufficient funds) are not retried.
- **Circuit Breaker**: Used for the external payment provider integration.
- **Idempotency**: The `orderId` or a provided `idempotencyKey` must be used to ensure a payment is not processed twice for the same order.

## 8. API Specification

### Process Payment

- Method: `POST`
- Path: `/api/payments`
- Purpose: Initiate a payment process for a specific order.
- Authentication: Internal (Service-to-Service)
- Authorization: Order Service only
- Request body:
  ```json
  {
    "orderId": "uuid",
    "amount": 150.00,
    "currency": "USD",
    "paymentMethod": "CREDIT_CARD",
    "idempotencyKey": "unique-request-id"
  }
  ```
- Success response: `201 Created`
  ```json
  {
    "paymentId": "uuid",
    "status": "PENDING",
    "transactionId": "provider-txn-id",
    "createdAt": "timestamp"
  }
  ```
- Error responses:
  - `400 Bad Request`: Validation error.
  - `409 Conflict`: Duplicate idempotency key.
  - `502 Bad Gateway`: Payment provider unavailable.

### Get Payment Status

- Method: `GET`
- Path: `/api/payments/{paymentId}`
- Purpose: Retrieve the current status of a payment.
- Authentication: Internal
- Authorization: Order Service only
- Success response: `200 OK`
  ```json
  {
    "paymentId": "uuid",
    "orderId": "uuid",
    "amount": 150.00,
    "status": "SUCCESS",
    "updatedAt": "timestamp"
  }
  ```
- Error responses:
  - `404 Not Found`: Payment ID does not exist.

## 9. Data Model

### Payment Entity

| Field | Type | Required | Constraints | Description |
|------|------|----------|-------------|-------------|
| id | UUID | Yes | PK | Unique payment identifier |
| orderId | UUID | Yes | Indexed | Reference to the order |
| amount | BigDecimal | Yes | > 0 | Transaction amount |
| currency | String | Yes | 3 chars | ISO currency code |
| status | Enum | Yes | Not Null | PENDING, SUCCESS, FAILED |
| providerTxnId | String | No | Unique | ID from the external provider |
| idempotencyKey | String | Yes | Unique | Prevents duplicate payments |
| createdAt | LocalDateTime | Yes | Not Null | Creation timestamp |
| updatedAt | LocalDateTime | Yes | Not Null | Last update timestamp |

## 10. DTOs

- `PaymentRequest`: Validates amount > 0 and presence of `orderId` and `idempotencyKey`.
- `PaymentResponse`: Returns the `paymentId` and current `status`.
- `PaymentStatusResponse`: Detailed view of the payment state.

## 11. Business Rules

- A payment cannot be transitioned from `SUCCESS` to `FAILED` or vice versa.
- Payments must be idempotent based on the `idempotencyKey`.
- Any payment attempt that exceeds the provider timeout is marked as `FAILED` or `PENDING` for manual/async reconciliation.
- Amounts must be positive.

## 12. Design Patterns

### Pattern: Adapter Pattern

- Where: Integration with external payment providers.
- Why: Decouples the core payment logic from specific provider APIs (e.g., Stripe, PayPal).
- Problem it solves: Avoids vendor lock-in and allows easy mocking for tests.
- Key participants: `PaymentProviderAdapter` (Interface), `MockPaymentProviderAdapter` (Implementation).

### Pattern: Strategy Pattern

- Where: Handling different payment methods (Credit Card, PayPal, etc.).
- Why: Different payment methods require different processing logic.
- Problem it solves: Avoids large if-else blocks in the service layer.
- Key participants: `PaymentStrategy` (Interface), `CreditCardPaymentStrategy`, `PaypalPaymentStrategy`.

## 13. Dependencies

### Required
- `spring-boot-starter-web`: REST APIs.
- `spring-boot-starter-data-jpa`: Persistence.
- `spring-boot-starter-validation`: Request validation.
- `com.h2database:h2`: Development database.
- `org.projectlombok:lombok`: Boilerplate reduction.
- `spring-boot-starter-actuator`: Health checks.
- `spring-boot-starter-test`: Unit and integration tests.

### Optional / Later
- `spring-cloud-starter-circuitbreaker-resilience4j`: For provider resilience.

## 14. Configuration

```properties
server.port=8085
spring.application.name=payment-service
spring.datasource.url=jdbc:h2:mem:payment-db
payment.provider.api-key=internal-mock-key
payment.provider.timeout=5000
```

## 15. Security

- **Authentication**: Service-to-service authentication (internal).
- **Authorization**: Only the Order Service should be able to initiate payments.
- **Sensitive Data**: Do NOT store full credit card numbers or CVVs in the database. Use tokens provided by the payment gateway.
- **Logging**: Mask payment IDs or transaction IDs in logs if they are considered sensitive.

## 16. Exception and Error Handling

| Exception | Trigger | HTTP Status | Response Shape |
|-----------|---------|-------------|----------------|
| `PaymentNotFoundException` | Invalid paymentId | 404 | `{ "error": "Payment not found" }` |
| `DuplicatePaymentException` | Idempotency key conflict | 409 | `{ "error": "Duplicate payment request" }` |
| `ProviderUnavailableException`| Provider API down | 502 | `{ "error": "Payment provider unavailable" }` |
| `InvalidPaymentRequestException`| Validation failure | 400 | `{ "error": "Invalid request data" }` |

## 17. Transaction Boundaries

- **Payment Creation**: Local transaction to save the initial `Payment` record with `PENDING` status.
- **Payment Update**: Local transaction to update status after provider response.
- **Distributed Note**: Payment Service does not participate in a global transaction with Order Service; consistency is managed via synchronous status checks and compensation (handled by Order Service).

## 18. Validation

- `orderId`: Not null, valid UUID.
- `amount`: Not null, positive value.
- `currency`: Not null, 3-letter ISO code.
- `idempotencyKey`: Not null, alphanumeric.

## 19. Testing Strategy

### Unit Tests
- Business logic in `PaymentService`.
- Idempotency logic.
- Payment status transition rules.

### Controller Tests
- Request validation (e.g., negative amount $\rightarrow$ 400).
- JSON contract verification.

### Repository Tests
- H2 persistence of Payment entity.
- Unique constraint on `idempotencyKey`.

### Integration Tests
- End-to-end flow: Request $\rightarrow$ Mock Provider $\rightarrow$ Database $\rightarrow$ Response.

### Failure Tests
- Mock provider timeout $\rightarrow$ check status is `FAILED` or `PENDING`.
- Duplicate request with same idempotency key $\rightarrow$ 409 Conflict.

## 20. Observability

- **Logs**: Log every payment attempt and the final status change.
- **Metrics**: Track number of successful vs failed payments.
- **Health**: `/actuator/health` checking DB connectivity.

## 21. Implementation Sequence

1. Update `pom.xml` with required dependencies.
2. Create `Payment` entity and `PaymentStatus` enum.
3. Create `PaymentRepository`.
4. Create Request/Response DTOs.
5. Implement `PaymentProviderAdapter` interface and `MockPaymentProviderAdapter`.
6. Implement `PaymentStrategy` for different payment methods.
7. Implement `PaymentService` business logic (including idempotency).
8. Implement `PaymentController`.
9. Implement global exception handling.
10. Add `application.properties` configuration.
11. Write unit, repository, and controller tests.
12. Implement integration tests for the full payment flow.
13. Run `./mvnw clean test`.

## 22. Acceptance Criteria

- [ ] `POST /api/payments` creates a payment and returns 201.
- [ ] `GET /api/payments/{id}` returns the correct payment status.
- [ ] Duplicate `idempotencyKey` returns 409 Conflict.
- [ ] Payment status cannot move from `SUCCESS` to `FAILED`.
- [ ] External provider is abstracted via an Adapter.
- [ ] No direct access to Order Service database.
- [ ] H2 database stores payment records.
- [ ] All tests pass via `./mvnw clean test`.
- [ ] No Kafka/RabbitMQ introduced.
- [ ] Java 17 and Spring Boot 3.2.5 used.

## 23. Out of Scope

- Integration with real payment providers (Stripe, PayPal) - strictly mocked.
- Handling of refunds or chargebacks.
- Complex subscription billing.

## 24. Decisions and Open Questions

- **Decision**: Use synchronous polling/status checks rather than webhooks for this learning project.
- **Decision**: Idempotency is enforced at the database level via a unique constraint on `idempotencyKey`.

## 25. Recommended Git Branch

Suggested Branch: `feature/payment-service`
Please switch to this branch before starting the implementation.
