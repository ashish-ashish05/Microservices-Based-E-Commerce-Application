# 10. Coupon Service Specification

## 1. Document Status

- Service: Coupon Service
- Build Order: 10
- Assignment: Coupon Management
- Status: Proposed
- Scope: Specification only

## 2. Purpose

The Coupon Service manages the lifecycle and validation of discount coupons. It provides the business logic to determine if a coupon is valid for a given request, calculates the resulting discount, and ensures that usage limits (if any) are enforced.

## 3. Responsibilities

- Create and manage coupons (code, discount value, type, expiry, usage limits).
- Validate coupon codes.
- Check coupon expiry dates.
- Check coupon eligibility (e.g., minimum order value, specific user types).
- Calculate the discount amount/percentage for a valid coupon.
- Enforce and track coupon usage limits (total uses, uses per user).

## 4. Non-Responsibilities

- Apply the discount to the final order total (this is a Pricing Service/Cart Service responsibility).
- Manage payment transactions.
- Store user profile data.
- Handle product inventory.

## 5. Service Boundary

- **Data Ownership**: Owns all coupon-related data (Coupon entity, Usage tracking).
- **Domain Ownership**: Owns the rules governing "what makes a coupon valid".
- **External Dependencies**: May be called by the Cart Service or Pricing Service to validate a code before applying a discount.
- **Boundary Logic**: The service provides a "Validation Result" (Valid/Invalid + Discount Value); the calling service decides how to apply that value to the total.

## 6. Architecture

Client
  |
API Gateway
  |
Cart Service / Pricing Service
  |
Coupon Service

## 7. Service-to-Service Communication

The Coupon Service is primarily a provider.

| Consumer | Provider | Method | Endpoint | Purpose | Failure Consideration |
|----------|----------|--------|----------|---------|-----------------------|
| Cart Service | Coupon Service | POST | `/api/coupons/validate` | Validate code and get discount | Timeout/Fallback: Treat as invalid coupon |
| Pricing Service | Coupon Service | POST | `/api/coupons/validate` | Validate code and get discount | Timeout/Fallback: Treat as invalid coupon |

**Resilience Considerations:**
- **Timeouts**: Strict timeouts for validation calls to prevent Cart/Pricing latency.
- **Fallback**: If the Coupon Service is unavailable, the system should fail-safe by treating the coupon as invalid rather than blocking the checkout process.

## 8. API Specification

### Create Coupon (Admin)

- Method: `POST`
- Path: `/api/coupons`
- Purpose: Create a new discount coupon.
- Authentication: Required
- Authorization: ADMIN
- Request body: `CouponRequest` (code, discountValue, discountType, startDate, endDate, usageLimit, minOrderValue)
- Success response: `201 Created` with `CouponResponse`.
- Error responses: `400 Bad Request` (validation failure), `409 Conflict` (code already exists).

### Validate Coupon

- Method: `POST`
- Path: `/api/coupons/validate`
- Purpose: Check if a coupon code is valid for a specific order context.
- Authentication: Optional/Required (depending on if user-specific limits apply)
- Authorization: USER/ADMIN
- Request body: `CouponValidationRequest` (code, userId, orderValue)
- Success response: `200 OK` with `CouponValidationResponse` (isValid, discountValue, discountType, message).
- Error responses: `400 Bad Request`, `404 Not Found` (coupon doesn't exist).

### Get Coupon Details

- Method: `GET`
- Path: `/api/coupons/{code}`
- Purpose: Retrieve details of a specific coupon.
- Authentication: Required
- Authorization: ADMIN
- Success response: `200 OK` with `CouponResponse`.
- Error responses: `404 Not Found`.

## 9. Data Model

### Coupon Entity

| Field | Type | Required | Constraints | Description |
|------|------|----------|-------------|-------------|
| id | UUID | Yes | Primary Key | Unique internal ID |
| code | String | Yes | Unique, Indexed | The alphanumeric code (e.g., "SAVE20") |
| discountValue | BigDecimal | Yes | Positive | The value of the discount |
| discountType | Enum | Yes | PERCENTAGE, FIXED | Type of discount |
| startDate | LocalDateTime | Yes | - | When the coupon becomes active |
| endDate | LocalDateTime | Yes | - | When the coupon expires |
| usageLimit | Long | No | $\ge 0$ | Max total times this coupon can be used |
| usedCount | Long | Yes | Default 0 | Current number of times used |
| minOrderValue | BigDecimal | No | $\ge 0$ | Minimum order total required to use coupon |

## 10. DTOs

### CouponRequest
- Purpose: Input for creating/updating coupons.
- Fields: `code`, `discountValue`, `discountType`, `startDate`, `endDate`, `usageLimit`, `minOrderValue`.
- Validation: `code` not blank, `discountValue` positive.

### CouponResponse
- Purpose: Output for coupon details.
- Fields: `id`, `code`, `discountValue`, `discountType`, `startDate`, `endDate`, `usageLimit`, `usedCount`, `minOrderValue`.

### CouponValidationRequest
- Purpose: Request to validate a coupon.
- Fields: `code`, `userId`, `orderValue`.
- Validation: `code` not blank.

### CouponValidationResponse
- Purpose: Result of validation.
- Fields: `isValid` (boolean), `discountValue`, `discountType`, `message` (reason for invalidity).

## 11. Business Rules

- A coupon is invalid if the current date is before `startDate` or after `endDate`.
- A coupon is invalid if the `usedCount` has reached the `usageLimit`.
- A coupon is invalid if the `orderValue` is less than the `minOrderValue`.
- Coupon codes must be unique and case-insensitive (stored/checked as uppercase).
- The `discountValue` for a `PERCENTAGE` type must be between 0 and 100.

## 12. Design Patterns

### Pattern: Strategy Pattern

- Where: Discount calculation logic.
- Why: To handle different types of discounts (Fixed vs Percentage) cleanly.
- Problem it solves: Avoids complex if-else blocks when calculating the actual deduction amount.
- Key participants: `DiscountStrategy` (interface), `FixedDiscountStrategy`, `PercentageDiscountStrategy`.

### Pattern: Repository Pattern

- Where: Data access layer.
- Why: Standard Spring Data JPA abstraction for Coupon persistence.

## 13. Dependencies

### Required
- `spring-boot-starter-web`: For REST APIs.
- `spring-boot-starter-data-jpa`: For H2 database interaction.
- `spring-boot-starter-validation`: For request body validation.
- `com.h2database:h2`: Development database.
- `org.projectlombok:lombok`: Boilerplate reduction.
- `spring-boot-starter-actuator`: Health checks and metrics.

### Optional / Later
- `spring-cloud-starter-openfeign`: If the Coupon Service needs to call other services (e.g., User Service to verify user eligibility).

## 14. Configuration

```properties
server.port=8085
spring.application.name=coupon-service
spring.datasource.url=jdbc:h2:mem:coupon-db
# standard JPA/H2 properties as per project baseline
```

## 15. Security

- **Authentication**: Required for admin endpoints (Create/Update/Delete).
- **Authorization**: Only users with `ROLE_ADMIN` can manage coupons.
- **Validation**: The `validate` endpoint may be accessible to authenticated users.

## 16. Exception and Error Handling

- `CouponNotFoundException`: Thrown when a requested code does not exist. HTTP 404.
- `CouponExpiredException`: Thrown when a coupon is past its end date. Handled as `isValid=false` in validation response.
- `CouponLimitExceededException`: Thrown when usage limit is reached. Handled as `isValid=false` in validation response.
- `InvalidCouponRequestException`: Thrown for malformed requests. HTTP 400.

## 17. Transaction Boundaries

- **Coupon Creation**: Local transaction to ensure the coupon is persisted.
- **Coupon Validation/Usage**: A transaction is required when incrementing the `usedCount` to prevent over-usage in concurrent requests (Optimistic Locking via `@Version` on the Coupon entity is recommended).

## 18. Validation

- **Request Validation**: `@NotBlank` for codes, `@NotNull` for required fields.
- **Business Invariants**: `discountValue` must be $\ge 0$.
- **State Transitions**: A coupon cannot be transitioned to "active" if its end date is in the past.

## 19. Testing Strategy

### Unit Tests
- Test `DiscountStrategy` implementations for correct math.
- Test business rules for expiry and usage limits in the service layer.

### Controller Tests
- Verify `404` for non-existent coupons.
- Verify `400` for invalid request bodies.
- Verify `200` success response for valid coupons.

### Repository Tests
- Verify unique constraint on the coupon code.
- Verify retrieval by code (case-insensitive).

### Integration Tests
- End-to-end flow: Create coupon $\rightarrow$ Validate coupon $\rightarrow$ Use coupon (increment count).

### Failure Tests
- Test behavior when H2 is unavailable.
- Test concurrent validation requests to ensure usage limits are strictly enforced.

## 20. Observability

- **Logs**: Log every coupon creation and failed validation attempt (with reason).
- **Metrics**: Count of valid vs invalid coupon attempts.
- **Health**: Standard Actuator `/health` endpoint.

## 21. Implementation Sequence

1. Initialize project structure and `pom.xml` (Done).
2. Implement `Coupon` entity with `@Version` for optimistic locking.
3. Implement `CouponRepository`.
4. Create `CouponRequest`, `CouponResponse`, `CouponValidationRequest`, and `CouponValidationResponse` DTOs.
5. Implement `DiscountStrategy` interface and concrete strategies (`Fixed`, `Percentage`).
6. Implement `CouponService` (business logic for validation and management).
7. Implement `CouponController` for Admin and User endpoints.
8. Implement global exception handling for `CouponNotFoundException` and validation errors.
9. Configure `application.properties`.
10. Write unit tests for strategies and service logic.
11. Write controller and integration tests.
12. Verify with `./mvnw clean test`.

## 22. Acceptance Criteria

- [ ] Admin can create coupons with constraints (expiry, limits, min order).
- [ ] `/api/coupons/validate` correctly returns `isValid=true` for active, non-expired, within-limit coupons.
- [ ] `/api/coupons/validate` correctly returns `isValid=false` for expired or limit-reached coupons.
- [ ] Minimum order value is enforced during validation.
- [ ] Usage count is incremented correctly when a coupon is "used" (assuming a use-coupon endpoint is added or integrated).
- [ ] Case-insensitive lookup for coupon codes.
- [ ] No direct database access from other services.
- [ ] Tests cover success and failure cases.
- [ ] `./mvnw clean test` passes.
- [ ] No Kafka/RabbitMQ introduced.
- [ ] Java 17 and Spring Boot 3.2.5 used.

## 23. Out of Scope

- User-specific coupon assignment (e.g., "this coupon only for User X").
- Complex coupon stacking rules (e.g., "cannot use Coupon A with Coupon B").
- Automatic email distribution of coupon codes.

## 24. Decisions and Open Questions

- **Decision**: Use synchronous REST calls for validation to maintain simplicity as per `CLAUDE.md`.
- **Decision**: Implement optimistic locking to handle concurrent usage of the same coupon code.
- **Question**: Should the `validate` endpoint also "reserve" the coupon (increment count) or just check validity? (Assumption: Validation only checks; a separate `redeem` call increments the count).

## 25. Recommended Git Branch

`feature/coupon-service`

Please switch to this branch before starting implementation.
