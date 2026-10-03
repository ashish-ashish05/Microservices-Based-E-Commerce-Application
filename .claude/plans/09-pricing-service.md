# 09. Pricing Service Implementation Plan

## 1. Plan Metadata

- Service: Pricing Service
- Build Order: 09
- Assignment: Pricing & Discounts
- Specification: .claude/specs/09-pricing-service.md
- Status: Planned
- Plan Generated: 2026-10-03
- Prerequisite Services: Product Service, Coupon Service

## 2. Objective

The Pricing Service will centralize all pricing logic, calculating final product prices and total cart costs by applying various strategies (Seasonal, Bulk, etc.) and coordinating with the Coupon Service for validated discounts.

## 3. Existing Context

The Pricing Service is called by the Product Service (for item prices) and the Cart Service (for total calculation). It depends on the Coupon Service (Port 8086) for discount values.

## 4. Scope

### In Scope
- Calculation of single product prices using `PricingStrategy`.
- Calculation of cart totals using a `DiscountHandler` chain.
- Implementation of the Strategy and Chain of Responsibility patterns.
- Integration with Coupon Service via OpenFeign.
- Persistence of `PricingRule` entities.
- Global exception handling for missing products or invalid coupons.
- Unit, Controller, and Integration tests.

### Out of Scope
- Managing the shopping cart state (handled by Cart Service).
- Coupon lifecycle management (handled by Coupon Service).
- Geographic tax calculations.

## 5. Architecture

Client
  |
API Gateway
  |
Cart Service / Product Service
  |
Pricing Service
  |
+--> Coupon Service (Synchronous REST)

Pricing Service owns the `PricingRule` entities. It calculates the delta between the base price (from Product Service) and the final price.

## 6. Implementation Dependencies

- **Maven Dependencies**:
    - `spring-boot-starter-web`
    - `spring-boot-starter-data-jpa`
    - `spring-boot-starter-validation`
    - `spring-cloud-starter-openfeign`
    - `com.h2database:h2`
    - `org.projectlombok:lombok`
    - `spring-boot-starter-actuator`
- **Prerequisite Services**:
    - Coupon Service (Running on port 8086)
- **Configuration**:
    - `server.port=8085`
    - `spring.datasource.url=jdbc:h2:mem:pricing-db`
    - `coupon.service.url=http://localhost:8086`

## 7. Data Model Plan

| Entity | Field | Type | Constraint | Notes |
|--------|-------|------|------------|-------|
| PricingRule | id | UUID | Primary Key | Unique ID |
| PricingRule | ruleName | String | Unique, Not Null | e.g., "Black Friday" |
| PricingRule | discountPercent | Double | 0-100 | Percentage discount |
| PricingRule | startDate | LocalDateTime | Not Null | Rule active from |
| PricingRule | endDate | LocalDateTime | Not Null | Rule active until |
| PricingRule | productId | UUID | - | If null, rule is global |

## 8. API Implementation Plan

### Calculate Product Price
- HTTP method: `GET`
- path: `/pricing/calculate/{productId}`
- request DTO: N/A
- validation: UUID format for `{productId}`
- response DTO: `PriceResponse` (productId, basePrice, finalPrice, discountApplied, currency)
- status codes: `200 OK`, `404 Not Found`
- authentication/authorization: Public
- downstream calls: None (Pricing Service calculates based on internal rules)
- error behavior: Return 404 if product not found in Pricing Rules (unless a default is used).

### Calculate Cart Total
- HTTP method: `POST`
- path: `/pricing/calculate-cart`
- request DTO: `CartPricingRequest` (items, couponCode)
- validation: `items` not empty, quantities > 0
- response DTO: `CartPricingResponse` (subTotal, discountTotal, finalTotal, currency, breakdown)
- status codes: `200 OK`, `400 Bad Request`
- authentication/authorization: USER/ADMIN
- downstream calls: `POST /api/coupons/validate` (Coupon Service)
- error behavior: Fallback to 0 discount if Coupon Service is unavailable.

## 9. Service Layer Plan

1. **Calculate Product Price**:
    - Identify applicable `PricingRule` for the product/date.
    - Use `PricingStrategyFactory` to select the correct `PricingStrategy` (Regular, Bulk, Seasonal).
    - Calculate final price and map to `PriceResponse`.
2. **Calculate Cart Total**:
    - For each item, calculate the subtotal.
    - Pass the request through the `DiscountHandler` chain (Global $\rightarrow$ Product $\rightarrow$ Coupon).
    - If a `couponCode` is provided, call the `CouponServiceClient` to get the discount value.
    - Sum the results and map to `CartPricingResponse`.

## 10. Design Pattern Implementation Plan

### Strategy Pattern
- Where: `PricingStrategy` interface and implementations (`RegularPricingStrategy`, `BulkPricingStrategy`, `SeasonalPricingStrategy`).
- Why: Different pricing types require different math.
- Main participants: `PricingStrategy`, `PricingStrategyFactory`.

### Chain of Responsibility
- Where: `DiscountHandler` abstract class and concrete handlers (`GlobalDiscountHandler`, `ProductDiscountHandler`, `CouponDiscountHandler`).
- Why: To apply a sequence of discounts in a specific order.
- Main participants: `DiscountHandler` and its subclasses.

## 11. Remote Communication Plan

| Caller | Provider | Endpoint | Purpose | Failure Handling |
|--------|----------|----------|---------|------------------|
| Pricing | Coupon | `/api/coupons/validate` | Validate coupon and get discount | Fallback to 0 discount; Circuit Breaker |

- **Timeout**: 2-second read timeout.
- **Retry**: No retries for POST requests.
- **Circuit Breaker**: Resilience4j to be used for Coupon Service calls.

## 12. Exception Handling Plan

- **ProductNotFoundException**: Maps to 404 Not Found.
- **InvalidCouponException**: Maps to 400 Bad Request.
- **Global Exception Handler**: Standardized JSON response.

## 13. Security Plan

- **Authentication**: `calculate-cart` requires a valid JWT.
- **Authorization**: `calculate-cart` requires `USER` or `ADMIN` role.
- **Public Access**: `calculate/{productId}` is public.

## 14. Testing Plan

### Unit Tests
- Individual `PricingStrategy` logic.
- `DiscountHandler` chain sequence and result.

### Controller Tests
- Validate `CartPricingRequest` and verify response shapes.

### Repository Tests
- Verify `PricingRule` retrieval based on date range.

### Integration Tests
- End-to-end flow from Request $\rightarrow$ Strategy $\rightarrow$ Response.

### Contract / Remote Tests
- Mock Coupon Service using WireMock to test Feign client behavior.

## 15. Files to Create

```text
src/main/java/com/ecommerce/pricing_service/
├── PricingServiceApplication.java
├── controller/PricingController.java
├── service/PricingService.java
├── service/PricingServiceImpl.java
├── repository/PricingRuleRepository.java
├── entity/PricingRule.java
├── dto/PriceRequest.java
├── dto/PriceResponse.java
├── dto/CartPricingRequest.java
├── dto/CartPricingResponse.java
├── strategy/PricingStrategy.java
├── strategy/RegularPricingStrategy.java
├── strategy/BulkPricingStrategy.java
├── strategy/SeasonalPricingStrategy.java
├── strategy/PricingStrategyFactory.java
├── handler/DiscountHandler.java
├── handler/GlobalDiscountHandler.java
├── handler/ProductDiscountHandler.java
├── handler/CouponDiscountHandler.java
├── client/CouponServiceClient.java
└── exception/GlobalExceptionHandler.java
```

## 16. Files to Modify

None.

## 17. Implementation Sequence

1. Define `PricingRule` entity and `PricingRuleRepository`.
2. Implement `PricingStrategy` interface and basic implementations.
3. Implement `PricingStrategyFactory`.
4. Implement `DiscountHandler` chain for coordinating discounts.
5. Create Feign client for Coupon Service.
6. Implement `PricingService` business logic.
7. Implement REST Controllers.
8. Implement global exception handling.
9. Configure `application.properties`.
10. Add unit and integration tests.
11. Verify with `./mvnw clean test`.

## 18. Verification Plan

- Run `./mvnw clean test`.
- Run `./mvnw help:effective-pom`.

## 19. Risks and Failure Scenarios

- **Coupon Service Down**: Handled by fallback to 0 discount.
- **Math Errors**: Handled by strict `BigDecimal` usage for currency.
- **Rule Overlap**: Handled by the "highest discount" rule.

## 20. Acceptance Criteria

- [ ] `GET /pricing/calculate/{productId}` returns correct price.
- [ ] `POST /pricing/calculate-cart` returns correct total for multiple items.
- [ ] Coupons are applied correctly via Coupon Service.
- [ ] Only the best applicable discount is used (unless additive).
- [ ] Rules are applied only if current date is within range.
- [ ] OpenFeign is used for remote calls.
- [ ] `./mvnw clean test` passes.

## 21. Planning Issues

None.

## 22. Implementation Prompt

> Implement `Pricing Service` according to `.claude/specs/09-pricing-service.md` and this plan. Read the root `CLAUDE.md`. Inspect the Coupon Service to understand its contract. Follow the implementation sequence and acceptance criteria. Do not introduce Kafka/RabbitMQ. Verify the build and tests before completion.
