# 14. Transaction Service Specification

## 1. Document Status

- Service: Transaction Service
- Build Order: 14
- Assignment: Payment Integration Phase
- Status: Proposed
- Scope: Specification only

## 2. Purpose

The Transaction Service is responsible for maintaining a durable, audit-friendly record of all payment transactions. While the Payment Service handles the *process* of attempting a payment (interacting with providers, managing retries), the Transaction Service owns the *history* and *state* of those transactions for reconciliation, auditing, and financial tracking.

## 3. Responsibilities

- Store immutable payment transaction records.
- Track the lifecycle state of a transaction (e.g., PENDING, COMPLETED, FAILED, REFUNDED).
- Provide a history of transactions for a given order or user.
- Support reconciliation between payment attempts and final outcomes.
- Maintain an audit trail of payment status changes.

## 4. Non-Responsibilities

- **MUST NOT** process payments directly with external providers (this is the Payment Service's job).
- **MUST NOT** manage shopping carts or order fulfillment.
- **MUST NOT** handle user authentication or authorization.
- **MUST NOT** calculate pricing or discounts.

## 5. Service Boundary

- **Data Ownership:** Owns the `Transaction` entity and its history.
- **Domain Ownership:** Financial record-keeping and transaction auditing.
- **External Dependencies:** Primarily called by the Payment Service to record outcomes.
- **Boundary Justification:** Separating the *transaction record* from the *payment process* allows the system to scale auditing and reconciliation independently of the payment integration logic.

## 6. Architecture

Client
  |
API Gateway
  |
Payment Service
  |
Transaction Service

## 7. Service-to-Service Communication

| Consumer | Provider | Method | Endpoint | Purpose | Failure Consideration |
|----------|----------|--------|----------|---------|-----------------------|
| Payment Service | Transaction Service | POST | `/api/transactions` | Record a new transaction attempt | Critical: Payment should not be considered "processed" without a record. |
| Payment Service | Transaction Service | PUT | `/api/transactions/{id}/status` | Update transaction outcome | Important: Mismatch between provider status and record must be flagged. |
| Order Service | Transaction Service | GET | `/api/transactions/order/{orderId}` | Audit payment status for an order | Non-critical: Can fallback to Order status. |

**Synchronous Communication Policies:**
- **Timeouts:** Strict 2-second read timeout to prevent Payment Service from hanging.
- **Retries:** Idempotent updates should be retried 3 times.
- **Circuit Breaker:** Use Resilience4j to open the circuit if the Transaction Service is unavailable, allowing Payment Service to queue records or fail gracefully.

## 8. API Specification

### Create Transaction

- Method: `POST`
- Path: `/api/transactions`
- Purpose: Initialize a transaction record when a payment attempt begins.
- Authentication: Internal (Payment Service)
- Request body:
  ```json
  {
    "orderId": "UUID",
    "amount": 100.00,
    "currency": "USD",
    "paymentMethod": "CREDIT_CARD",
    "providerTransactionId": "string (optional)"
  }
  ```
- Success response: `201 Created` with Transaction ID.
- Error responses: `400 Bad Request` (validation error).

### Update Transaction Status

- Method: `PUT`
- Path: `/api/transactions/{id}/status`
- Purpose: Update the status of a transaction (e.g., to COMPLETED or FAILED).
- Authentication: Internal (Payment Service)
- Request body:
  ```json
  {
    "status": "COMPLETED",
    "providerReference": "string",
    "remarks": "Payment confirmed by Stripe"
  }
  ```
- Success response: `200 OK`.
- Error responses: `404 Not Found`, `400 Bad Request` (invalid state transition).

### Get Transactions by Order

- Method: `GET`
- Path: `/api/transactions/order/{orderId}`
- Purpose: Retrieve all transaction attempts associated with a specific order.
- Authentication: Internal (Order/Payment Service)
- Success response: `200 OK` with a list of transaction DTOs.

## 9. Data Model

### Transaction Entity

| Field | Type | Required | Constraints | Description |
|------|------|----------|-------------|-------------|
| id | UUID | Yes | Primary Key | Internal transaction identifier |
| orderId | UUID | Yes | Indexed | Reference to the Order |
| amount | BigDecimal | Yes | > 0 | Transaction amount |
| currency | String | Yes | Length 3 | ISO currency code (e.g., USD) |
| status | TransactionStatus | Yes | NOT NULL | PENDING, COMPLETED, FAILED, REFUNDED |
| paymentMethod | String | Yes | | e.g., CREDIT_CARD, PAYPAL |
| providerTransactionId | String | No | Unique | ID provided by external gateway |
| createdAt | LocalDateTime | Yes | | Timestamp of creation |
| updatedAt | LocalDateTime | Yes | | Timestamp of last update |
| remarks | String | No | | Additional audit notes |

## 10. DTOs

### TransactionRequest
- Purpose: Input for creating a transaction.
- Fields: `orderId` (not null), `amount` (positive), `currency` (not null), `paymentMethod` (not null).

### TransactionResponse
- Purpose: Output for transaction details.
- Fields: `id`, `orderId`, `amount`, `currency`, `status`, `createdAt`.
- Security: Do not expose internal provider keys or raw API responses.

### StatusUpdateRequest
- Purpose: Input for updating status.
- Fields: `status` (not null), `providerReference` (optional), `remarks` (optional).

## 11. Business Rules

- A transaction cannot be created with a negative or zero amount.
- A `COMPLETED` transaction cannot be moved back to `PENDING`.
- An order can have multiple transaction attempts (e.g., first failed, second succeeded).
- The `providerTransactionId` must be unique if provided.

## 12. Design Patterns

### Pattern: State Pattern

- Where: Transaction status management.
- Why: To strictly control valid transitions (e.g., `PENDING` $\rightarrow$ `COMPLETED` or `FAILED`).
- Problem it solves: Prevents illegal state changes (e.g., `COMPLETED` $\rightarrow$ `PENDING`).
- Key participants: `TransactionStatus` enum, `TransactionService` transition logic.

### Pattern: Repository Pattern

- Where: Data access layer.
- Why: Standard Spring Data JPA approach for decoupling business logic from H2 persistence.

## 13. Dependencies

### Required
- `spring-boot-starter-web`: REST APIs.
- `spring-boot-starter-data-jpa`: Persistence.
- `spring-boot-starter-validation`: Input validation.
- `com.h2database:h2`: Development database.
- `org.projectlombok:lombok`: Boilerplate reduction.
- `spring-boot-starter-actuator`: Health monitoring.

### Optional / Later
- `spring-cloud-starter-openfeign`: Not required unless it needs to call other services (currently a provider).

## 14. Configuration

```properties
server.port=8086
spring.application.name=transaction-service
spring.datasource.url=jdbc:h2:mem:transaction-db
spring.jpa.hibernate.ddl-auto=update
```

## 15. Security

- **Authentication:** Restricted to internal service-to-service calls (Payment/Order services).
- **Authorization:** Only the Payment Service should be allowed to update transaction statuses.
- **Sensitive Info:** Do not log raw provider response payloads that might contain PII or secrets.

## 16. Exception and Error Handling

- `TransactionNotFoundException`: `404 Not Found`.
- `InvalidStateTransitionException`: `400 Bad Request` when updating status illegally.
- `ValidationException`: `400 Bad Request` for malformed requests.

## 17. Transaction Boundaries

- Local transactions are required for:
    - Creating a transaction record.
    - Updating status and adding audit remarks atomically.

## 18. Validation

- **Request Fields:** Amount must be positive; Currency must be 3 characters.
- **State Transitions:** Only `PENDING` $\rightarrow$ `COMPLETED/FAILED` or `COMPLETED` $\rightarrow$ `REFUNDED`.
- **Business Invariants:** `orderId` must be a valid UUID.

## 19. Testing Strategy

### Unit Tests
- Test `TransactionStatus` transition logic.
- Test business rules for amount and currency validation.

### Controller Tests
- Verify `POST /api/transactions` returns `201`.
- Verify `PUT /api/transactions/{id}/status` returns `200` on success and `400` on invalid transition.

### Repository Tests
- Verify persistence and retrieval of transaction records.
- Verify uniqueness of `providerTransactionId`.

### Integration Tests
- End-to-end flow: Create $\rightarrow$ Update Status $\rightarrow$ Get by Order.

### Failure Tests
- Handle cases where the database is unavailable.
- Verify behavior when an invalid transaction ID is provided.

## 20. Observability

- **Logs:** Log every status change with the associated `orderId` and `transactionId`.
- **Metrics:** Track the count of `COMPLETED` vs `FAILED` transactions.
- **Health:** Standard Spring Actuator `/health` endpoint.

## 21. Implementation Sequence

1. Update Maven project dependencies.
2. Create `Transaction` entity and `TransactionStatus` enum.
3. Create `TransactionRepository`.
4. Create Request/Response DTOs.
5. Implement `TransactionService` with state transition logic.
6. Implement `TransactionController` endpoints.
7. Implement global exception handling for `TransactionNotFoundException` and `InvalidStateTransitionException`.
8. Add unit and integration tests.
9. Run `./mvnw clean test` and verify.

## 22. Acceptance Criteria

- [ ] `POST /api/transactions` correctly initializes a record.
- [ ] `PUT /api/transactions/{id}/status` enforces valid state transitions.
- [ ] `GET /api/transactions/order/{orderId}` returns all attempts for an order.
- [ ] Database constraints (e.g., `providerTransactionId` uniqueness) are enforced.
- [ ] No direct access to Payment or Order databases.
- [ ] All tests pass with `./mvnw clean test`.
- [ ] No Kafka/RabbitMQ introduced.

## 23. Out of Scope

- Direct integration with payment gateways (Stripe, PayPal, etc.).
- User-facing transaction history UI.
- Automatic refund triggers (triggered by Payment Service).

## 24. Decisions and Open Questions

- **Fixed:** Synchronous REST communication.
- **Fixed:** H2 database for development.
- **Assumption:** Payment Service will act as the primary orchestrator for updating transactions.

## 25. Recommended Git Branch

Suggested Branch: `feature/transaction-service`
Please switch to this branch before starting implementation.
