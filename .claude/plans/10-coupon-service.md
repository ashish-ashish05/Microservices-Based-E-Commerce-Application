# 10. Coupon Service Implementation Plan

## 1. Plan Metadata

- Service: Coupon Service
- Build Order: 10
- Assignment: Coupon Management
- Specification: .claude/specs/10-coupon-service.md
- Status: Planned
- Plan Generated: 2026-10-03
- Prerequisite Services: None

## 2. Objective

The Coupon Service will manage the lifecycle and validation of discount coupons, ensuring that only valid, non-expired, and within-limit coupons are used to provide discounts.

## 3. Existing Context

The Coupon Service is a provider service, primarily consumed by the Pricing Service (Port 8085) and the Cart Service (Port 8086).

## 4. Scope

### In Scope
- Coupon CRUD operations (Create, Get).
- Validation logic for codes, expiry dates, and usage limits.
- Implementation of the Strategy pattern for calculating discount amounts (Fixed vs Percentage).
- Implementation of Repository and DTO patterns.
- Optimistic locking via `@Version` to handle concurrent coupon usage.
- Unit, Controller, and Integration tests.

### Out of Scope
- Applying the discount to the order total (handled by Pricing Service).
- Payment processing.
- User profile management.

## 5. Architecture

Client
  |
API Gateway
  |
Cart Service / Pricing Service
  |
Coupon Service
  |
(H2 Database)

The Coupon Service owns the `Coupon` entity. It provides a "Validation Result" (isValid + discount value) to calling services.

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
    - `spring.datasource.url=jdbc:h2:mem:coupon-db`

## 7. Data Model Plan

| Entity | Field | Type | Constraint | Notes |
|--------|-------|------|------------|-------|
| Coupon | id | UUID | Primary Key | Internal ID |
| Coupon | code | String | Unique, Indexed | Alphanumeric code (stored as uppercase) |
| Coupon | discountValue | BigDecimal | Positive | Value of the discount |
| Coupon | discountType | Enum | Not Null | PERCENTAGE or FIXED |
| Coupon | startDate | LocalDateTime | Not Null | Activation date |
| Coupon | endDate | LocalDateTime | Not Null | Expiry date |
| Coupon | usageLimit | Long | $\ge 0$ | Max uses allowed |
| Coupon | usedCount | Long | Default 0 | Current usage count |
| Coupon | minOrderValue | BigDecimal | $\ge 0$ | Min order total required |
| Coupon | version | Long | @Version | For optimistic locking |

## 8. API Implementation Plan

### Create Coupon (Admin)
- HTTP method: `POST`
- path: `/api/coupons`
- request DTO: `CouponRequest` (code, discountValue, discountType, startDate, endDate, usageLimit, minOrderValue)
- validation: `code` (@NotBlank), `discountValue` (Positive)
- response DTO: `CouponResponse`
- status codes: `201 Created`, `400 Bad Request`, `409 Conflict` (duplicate code)
- authentication/authorization: ADMIN
- downstream calls: None
- error behavior: Return 409 if coupon code exists.

### Validate Coupon
- HTTP method: `POST`
- path: `/api/coupons/validate`
- request DTO: `CouponValidationRequest` (code, userId, orderValue)
- validation: `code` (@NotBlank)
- response DTO: `CouponValidationResponse` (isValid, discountValue, discountType, message)
- status codes: `200 OK`, `400 Bad Request`, `404 Not Found` (coupon not found)
- authentication/authorization: USER/ADMIN
- downstream calls: None
- error behavior: Return `isValid=false` for expired, limit-reached, or order-value-too-low coupons.

### Get Coupon Details (Admin)
- HTTP method: `GET`
- path: `/api/coupons/{code}`
- request DTO: N/A
- validation: String format for `{code}`
- response DTO: `CouponResponse`
- status codes: `200 OK`, `404 Not Found`
- authentication/authorization: ADMIN
- downstream calls: None
- error behavior: Throw `CouponNotFoundException`.

## 9. Service Layer Plan

1. **Create Coupon**:
    - Normalize code to uppercase.
    - Validate constraints (e.g., `discountValue` positive).
    - Save the `Coupon` entity.
2. **Validate Coupon**:
    - Retrieve `Coupon` by code (case-insensitive).
    - Check if current date is between `startDate` and `endDate`.
    - Check if `usedCount` < `usageLimit`.
    - Check if `orderValue` $\ge$ `minOrderValue`.
    - Use `DiscountStrategy` to determine the final discount amount based on `discountType`.
    - Return `CouponValidationResponse`.
3. **Use Coupon (Redeem)**:
    - Increment `usedCount` within a transaction using optimistic locking.
    - Handle `ObjectOptimisticLockingFailureException` by retrying or returning an error.

## 10. Design Pattern Implementation Plan

### Strategy Pattern
- Where: `DiscountStrategy` interface and concrete strategies (`FixedDiscountStrategy`, `PercentageDiscountStrategy`).
- Why: To handle different discount types (Fixed vs Percentage) cleanly.
- Main participants: `DiscountStrategy`, `FixedDiscountStrategy`, `PercentageDiscountStrategy`.

### Repository Pattern
- Where: `CouponRepository`
- Why: Standard JPA abstraction.
- Main participants: `CouponRepository` extending `JpaRepository`.

### DTO Pattern
- Where: `CouponRequest`, `CouponResponse`, `CouponValidationRequest`, `CouponValidationResponse`.
- Why: Decouples internal entity from API contract.
- Main participants: `CouponRequest`, `CouponResponse`, `CouponValidationRequest`, `CouponValidationResponse`.

## 11. Remote Communication Plan

No remote calls are made by the Coupon Service.

## 12. Exception Handling Plan

- **CouponNotFoundException**: Thrown when a requested code does not exist. Maps to 404 Not Found.
- **Global Exception Handler**: Standardized JSON response.

## 13. Security Plan

- **Authentication**: Admin endpoints require `ROLE_ADMIN`.
- **Authorization**: Validation endpoint is public or requires `USER` role.
- **Sensitive Data**: None.

## 14. Testing Plan

### Unit Tests
- `DiscountStrategy` implementations (math verification).
- Business rules for expiry, limits, and min order value in the service layer.

### Controller Tests
- Verify `404` for non-existent coupons and `400` for invalid requests.

### Repository Tests
- Verify unique constraint on the coupon code.
- Verify case-insensitive retrieval.

### Integration Tests
- End-to-end flow: Create coupon $\rightarrow$ Validate coupon $\rightarrow$ Use coupon.
- Test concurrent validation requests to ensure usage limits are strictly enforced.

## 15. Files to Create

```text
src/main/java/com/ecommerce/coupon_service/
├── CouponServiceApplication.java
├── controller/CouponController.java
├── service/CouponService.java
├── service/CouponServiceImpl.java
├── repository/CouponRepository.java
├── entity/Coupon.java
├── dto/CouponRequest.java
├── dto/CouponResponse.java
├── dto/CouponValidationRequest.java
├── dto/CouponValidationResponse.java
├── strategy/DiscountStrategy.java
├── strategy/FixedDiscountStrategy.java
├── strategy/PercentageDiscountStrategy.java
└── exception/CouponNotFoundException.java
└── exception/GlobalExceptionHandler.java
```

## 16. Files to Modify

None.

## 17. Implementation Sequence

1. Initialize Maven project and add dependencies.
2. Implement `Coupon` entity with `@Version`.
3. Implement `CouponRepository`.
4. Create DTOs.
5. Implement `DiscountStrategy` and concrete strategies.
6. Implement `CouponService` (validation and management logic).
7. Implement `CouponController`.
8. Implement global exception handling.
9. Configure `application.properties`.
10. Write unit, controller, and integration tests.
11. Run `./mvnw clean test` to verify.

## 18. Verification Plan

- Run `./mvnw clean test`.
- Run `./mvnw help:effective-pom`.

## 19. Risks and Failure Scenarios

- **Concurrent Usage**: Multiple requests to use the same coupon simultaneously. Handled by Optimistic Locking.
- **Invalid Code Case**: Case sensitivity issues. Handled by storing and searching in uppercase.

## 20. Acceptance Criteria

- [ ] Admin can create coupons with constraints (expiry, limits, min order).
- [ ] `/api/coupons/validate` correctly returns `isValid=true` for active, non-expired, within-limit coupons.
- [ ] `/api/coupons/validate` correctly returns `isValid=false` for expired or limit-reached coupons.
- [ ] Minimum order value is enforced during validation.
- [ ] Case-insensitive lookup for coupon codes.
- [ ] No direct access to other services' databases.
- [ ] `./mvnw clean test` passes.
- [ ] Dependency versions follow `CLAUDE.md`.

## 21. Planning Issues

None.

## 22. Implementation Prompt

> Implement `Coupon Service` according to `.claude/specs/10-coupon-service.md` and this plan. Read the root `CLAUDE.md`. Follow the implementation sequence and acceptance criteria. Do not introduce Kafka/RabbitMQ. Verify the build and tests before completion.
