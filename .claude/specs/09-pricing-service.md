# 09. Pricing Service Specification

## 1. Document Status

- Service: Pricing Service
- Build Order: 09
- Assignment: Pricing & Discounts
- Status: Proposed
- Scope: Specification only

## 2. Purpose

The Pricing Service is responsible for calculating the final price of products and total costs for carts. It centralizes pricing logic, ensuring that discounts, seasonal rules, and member pricing are applied consistently across the platform without duplicating this logic in the Product or Cart services.

## 3. Responsibilities

- Calculate the current price of a specific product.
- Calculate total cart costs including all items.
- Apply pricing rules (e.g., seasonal discounts, bulk pricing).
- Coordinate multiple pricing strategies to determine the best price for the user.
- Provide pricing data for product listings and cart summaries.

## 4. Non-Responsibilities

- **Product Data Ownership:** Does not own product descriptions, categories, or basic identity (owned by Product Service).
- **Cart Management:** Does not manage cart items or user sessions (owned by Cart Service).
- **Coupon Validation:** Does not validate coupon codes or manage coupon lifecycles (owned by Coupon Service).
- **Payment Processing:** Does not handle actual financial transactions (owned by Payment Service).

## 5. Service Boundary

- **Data Ownership:** Owns pricing rules, strategy configurations, and historical pricing data if persisted.
- **Domain Ownership:** Owns the "Price Calculation" domain.
- **External Dependencies:** 
    - **Product Service:** To verify product existence or get base prices.
    - **Coupon Service:** To get discount values for validated coupons.
- **Boundary Justification:** Pricing logic changes frequently (sales, promotions). Isolating it prevents having to redeploy the core Product or Cart services every time a pricing rule changes.

## 6. Architecture

Client
  |
API Gateway
  |
Cart Service / Product Service
  |
Pricing Service
  |
(Calls Coupon Service for specific discounts)

## 7. Service-to-Service Communication

| Consumer | Provider | Method | Endpoint | Purpose | Failure Consideration |
|----------|----------|--------|----------|---------|-----------------------|
| Pricing | Coupon | GET | `/coupons/{code}/discount` | Get discount value for a code | Fallback to 0 discount; Circuit Breaker |
| Cart | Pricing | POST | `/pricing/calculate-cart` | Get total cost for cart items | Critical; timeout and retry; Cart service handles failure |
| Product | Pricing | GET | `/pricing/calculate/{productId}` | Get current price for a product | Fallback to base price from Product DB |

**Resilience Notes:**
- **Timeouts:** All remote calls must have a strict timeout (e.g., 2 seconds).
- **Circuit Breakers:** Used for the Coupon Service to prevent cascading failures during heavy promotion periods.
- **Fallbacks:** If Pricing Service is unavailable, the Product Service should fall back to the base price stored in the product database.

## 8. API Specification

### Calculate Product Price

- Method: `GET`
- Path: `/pricing/calculate/{productId}`
- Purpose: Returns the calculated price for a single product after applying global rules.
- Authentication: None (Public)
- Authorization: None
- Path parameters: `productId` (UUID)
- Success response: `200 OK`
- Response body:
  ```json
  {
    "productId": "uuid",
    "basePrice": 100.00,
    "finalPrice": 90.00,
    "discountApplied": 10.00,
    "currency": "USD"
  }
  ```
- Error responses: `404 Not Found` (Product not found)

### Calculate Cart Total

- Method: `POST`
- Path: `/pricing/calculate-cart`
- Purpose: Calculates total for a list of items, optionally applying a coupon code.
- Authentication: JWT (User)
- Authorization: USER/ADMIN
- Request body:
  ```json
  {
    "items": [
      { "productId": "uuid", "quantity": 2 },
      { "productId": "uuid", "quantity": 1 }
    ],
    "couponCode": "SUMMER20"
  }
  ```
- Success response: `200 OK`
- Response body:
  ```json
  {
    "subTotal": 250.00,
    "discountTotal": 50.00,
    "finalTotal": 200.00,
    "currency": "USD",
    "breakdown": [
      { "productId": "uuid", "itemTotal": 100.00, "discount": 20.00 }
    ]
  }
  ```
- Error responses: `400 Bad Request` (Invalid items/coupon)

## 9. Data Model

| Entity | Field | Type | Required | Constraints | Description |
|--------|-------|------|----------|-------------|-------------|
| PricingRule | id | UUID | Yes | PK | Unique ID of the rule |
| PricingRule | ruleName | String | Yes | Unique | Name of the rule (e.g., "Black Friday") |
| PricingRule | discountPercent | Double | Yes | 0-100 | Percentage discount |
| PricingRule | startDate | LocalDateTime | Yes | | Rule active from |
| PricingRule | endDate | LocalDateTime | Yes | | Rule active until |
| PricingRule | productId | UUID | No | FK (Logical) | If null, rule is global; if set, product-specific |

## 10. DTOs

### PriceRequest
- Purpose: Request for single product pricing.
- Fields: `productId`.

### PriceResponse
- Purpose: Result of price calculation.
- Fields: `productId`, `basePrice`, `finalPrice`, `discountApplied`, `currency`.

### CartPricingRequest
- Purpose: Calculate total for a cart.
- Fields: `items` (List of `CartItemDTO`), `couponCode`.

### CartPricingResponse
- Purpose: Total cost and breakdown.
- Fields: `subTotal`, `discountTotal`, `finalTotal`, `currency`, `breakdown`.

## 11. Business Rules

- **Rule Precedence:** Only the highest applicable discount should be applied unless rules are explicitly additive.
- **Date Validation:** A pricing rule must only be applied if the current date is between `startDate` and `endDate`.
- **Minimum Price:** The final price cannot be negative.
- **Bulk Discount:** If quantity > X, apply a specific bulk discount strategy.

## 12. Design Patterns

### Pattern: Strategy Pattern

- Where: Price calculation logic.
- Why: Different pricing types (Regular, Seasonal, Bulk, Member) require different calculation logic.
- Problem it solves: Avoids a giant `if-else` block in the service layer.
- Key participants: `PricingStrategy` (Interface), `RegularPricingStrategy`, `BulkPricingStrategy`, `SeasonalPricingStrategy`, `PricingStrategyFactory`.

### Pattern: Chain of Responsibility

- Where: Applying a sequence of discounts (Global $\rightarrow$ Category $\rightarrow$ Product $\rightarrow$ Coupon).
- Why: Allows a request to pass through multiple potential discount filters.
- Problem it solves: Decouples the order of discount application from the calculation logic.
- Key participants: `DiscountHandler` (Abstract), `GlobalDiscountHandler`, `ProductDiscountHandler`, `CouponDiscountHandler`.

## 13. Dependencies

### Required
- `spring-boot-starter-web`: REST APIs.
- `spring-boot-starter-data-jpa`: Rule persistence.
- `spring-boot-starter-validation`: DTO validation.
- `spring-cloud-starter-openfeign`: Communication with Coupon Service.
- `com.h2database:h2`: Development database.
- `org.projectlombok:lombok`: Boilerplate reduction.
- `spring-boot-starter-actuator`: Monitoring.
- `spring-boot-starter-test`: Testing.

### Optional / Later
- `spring-cloud-starter-circuitbreaker-resilience4j`: For Coupon Service calls.

## 14. Configuration

```properties
server.port=8085
spring.application.name=pricing-service
spring.datasource.url=jdbc:h2:mem:pricing-db
coupon.service.url=http://localhost:8086 # Example port
```

## 15. Security

- **Authentication:** `calculate-cart` requires a valid JWT from API Gateway.
- **Authorization:** `calculate-cart` requires `USER` or `ADMIN` role.
- **Public Access:** `calculate/{productId}` is public to allow guests to see prices.

## 16. Exception and Error Handling

- `ProductNotFoundException`: `404 Not Found` - When a requested product ID doesn't exist in Product Service.
- `InvalidCouponException`: `400 Bad Request` - When the Coupon Service reports the code is invalid/expired.
- `PricingCalculationException`: `500 Internal Server Error` - Unexpected math or logic error.

## 17. Transaction Boundaries

- **Read-Only:** Most operations are read-only calculations and do not require database transactions.
- **Admin Rule Updates:** Creating or updating `PricingRule` entities must be wrapped in `@Transactional`.

## 18. Validation

- **Request Validation:** Quantities must be $> 0$.
- **Rule Invariants:** `discountPercent` must be between $0$ and $100$.
- **State Transitions:** Rules cannot be updated to an invalid date range.

## 19. Testing Strategy

### Unit Tests
- Test individual `PricingStrategy` implementations.
- Test `DiscountHandler` chain logic.

### Controller Tests
- Validate `calculate-cart` request body.
- Verify HTTP status codes for missing products.

### Repository Tests
- Verify `PricingRule` persistence and date-based querying.

### Integration Tests
- End-to-end flow from Request $\rightarrow$ Strategy $\rightarrow$ Response.

### Contract / Remote Tests
- Mock Coupon Service using WireMock to test Feign client behavior.

### Failure Tests
- Simulate Coupon Service timeout and verify fallback to 0 discount.

## 20. Observability

- **Logs:** Log every time a specific pricing rule is applied for audit purposes.
- **Metrics:** Track the most frequently used pricing rules.
- **Health:** Actuator `/health` endpoint.

## 21. Implementation Sequence

1. Define `PricingRule` entity and `PricingRuleRepository`.
2. Implement `PricingStrategy` interface and basic implementations (Regular, Bulk).
3. Implement `PricingStrategyFactory`.
4. Implement `DiscountHandler` chain for coordinating different discount types.
5. Create Feign client for Coupon Service.
6. Implement `PricingService` business logic (combining strategies and handlers).
7. Implement REST Controllers for product and cart pricing.
8. Implement global exception handling.
9. Add configuration and properties.
10. Write unit and integration tests.
11. Verify with `./mvnw clean test`.

## 22. Acceptance Criteria

- [ ] `GET /pricing/calculate/{productId}` returns correct price based on active rules.
- [ ] `POST /pricing/calculate-cart` returns correct total for multiple items.
- [ ] Coupons are applied correctly via Coupon Service integration.
- [ ] Only the best applicable discount is used (unless additive).
- [ ] Rules are only applied if the current date is within the rule's range.
- [ ] OpenFeign is used for remote calls.
- [ ] Failures in Coupon Service do not crash the pricing calculation (fallback used).
- [ ] No direct access to Product or Coupon databases.
- [ ] `./mvnw clean test` passes.

## 23. Out of Scope

- Managing the actual cart state.
- Managing the lifecycle of coupons.
- Processing payments.
- Complex tax calculations based on geography (future scope).

## 24. Decisions and Open Questions

- **Decision:** Synchronous communication is used for all calculations to ensure real-time pricing.
- **Assumption:** The Product Service provides the base price; Pricing Service calculates the delta.
- **Question:** Should the Pricing Service cache base prices from the Product Service to reduce network calls? (Suggested: No, for now, to keep it simple).

---
**Suggested Branch:** `feature/pricing-service`
Please switch to this branch before starting implementation.
