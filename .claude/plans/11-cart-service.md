# 11. Cart Service Implementation Plan

## 1. Plan Metadata

- Service: Cart Service
- Build Order: 11
- Assignment: Shopping Cart Management
- Specification: .claude/specs/11-cart-service.md
- Status: Planned
- Plan Generated: 2026-10-03
- Prerequisite Services: Product Service, Pricing Service

## 2. Objective

The Cart Service will manage a user's temporary collection of products, providing functionality to add, update, and remove items, and aggregating total pricing from the Pricing Service.

## 3. Existing Context

The Cart Service is used by the user to stage products before checkout. It depends on the Product Service (Port 8081) for product existence verification and the Pricing Service (Port 8085) for real-time cost calculation.

## 4. Scope

### In Scope
- Cart and CartItem CRUD operations (Create, Get, Update, Delete).
- Integration with Product Service via OpenFeign to validate products.
- Integration with Pricing Service via OpenFeign to calculate cart totals.
- Implementation of Repository and DTO patterns.
- Global exception handling for 404, 400, and 503 errors.
- Unit, Controller, and Integration tests.

### Out of Scope
- Product data management (handled by Product Service).
- Pricing logic/discount rules (handled by Pricing Service).
- Order creation (handled by Order Service).
- Redis caching (deferred).

## 5. Architecture

Client
  |
API Gateway
  |
Cart Service
  |--------------------|
  v                    v
Product Service   Pricing Service

The Cart Service owns the `Cart` and `CartItem` entities. It acts as a facade, combining local state (items) with remote data (existence and pricing).

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
    - Product Service (Running on port 8081)
    - Pricing Service (Running on port 8085)
- **Configuration**:
    - `server.port=8085` (Note: Check port conflict with Pricing Service)
    - `spring.datasource.url=jdbc:h2:mem:cart-db`
    - `product-service.url=http://localhost:8081`
    - `pricing-service.url=http://localhost:8086`

## 7. Data Model Plan

| Entity | Field | Type | Constraint | Notes |
|--------|-------|------|------------|-------|
| Cart | id | UUID | Primary Key | Unique Cart ID |
| Cart | userId | String | Unique, Indexed | Reference to User Service |
| Cart | createdAt | LocalDateTime | | Creation timestamp |
| Cart | updatedAt | LocalDateTime | | Last update timestamp |
| CartItem | id | UUID | Primary Key | Unique Item ID |
| CartItem | cartId | UUID | FK to Cart | Reference to parent cart |
| CartItem | productId | UUID | Indexed | Reference to Product Service |
| CartItem | quantity | Integer | > 0 | Number of items |

## 8. API Implementation Plan

### Get Cart
- HTTP method: `GET`
- path: `/carts/{userId}`
- request DTO: N/A
- validation: String format for `{userId}`
- response DTO: `CartResponse` (userId, items, totalPrice, totalItems)
- status codes: `200 OK`, `404 Not Found`
- authentication/authorization: User (self) or ADMIN
- downstream calls: `POST /pricing/calculate-cart` (Pricing Service)
- error behavior: If Pricing Service is unavailable, mark `totalPrice` as null and return 503 or a partial response.

### Add Item to Cart
- HTTP method: `POST`
- path: `/carts/{userId}/items`
- request DTO: `CartRequest` (productId, quantity)
- validation: `productId` (@NotNull), `quantity` (> 0)
- response DTO: `CartResponse`
- status codes: `201 Created`, `400 Bad Request`, `404 Not Found`
- authentication/authorization: User (self) or ADMIN
- downstream calls: `GET /products/{id}` (Product Service)
- error behavior: Throw `ProductNotFoundException` if product does not exist.

### Update Item Quantity
- HTTP method: `PUT`
- path: `/carts/{userId}/items/{productId}`
- request DTO: `CartRequest` (quantity)
- validation: `quantity` (> 0)
- response DTO: `CartResponse`
- status codes: `200 OK`, `400 Bad Request`, `404 Not Found`
- authentication/authorization: User (self) or ADMIN
- downstream calls: None
- error behavior: Throw `CartNotFoundException` if cart or item doesn't exist.

### Remove Item from Cart
- HTTP method: `DELETE`
- path: `/carts/{userId}/items/{productId}`
- request DTO: N/A
- validation: N/A
- response DTO: N/A
- status codes: `204 No Content`, `404 Not Found`
- authentication/authorization: User (self) or ADMIN
- downstream calls: None
- error behavior: Throw `CartNotFoundException`.

### Clear Cart
- HTTP method: `DELETE`
- path: `/carts/{userId}`
- request DTO: N/A
- validation: N/A
- response DTO: N/A
- status codes: `204 No Content`
- authentication/authorization: User (self) or ADMIN
- downstream calls: None
- error behavior: None.

## 9. Service Layer Plan

1. **Get Cart**:
    - Retrieve `Cart` and its `CartItem`s from the database.
    - Call `PricingServiceClient` with the list of items to get the total price and breakdown.
    - Map to `CartResponse`.
2. **Add Item**:
    - Verify `productId` exists via `ProductServiceClient`.
    - Find or create a `Cart` for the `userId`.
    - If the product is already in the cart, increment the quantity.
    - Otherwise, create a new `CartItem`.
    - Save and return the updated cart.
3. **Update Quantity**:
    - Retrieve `CartItem` by `cartId` and `productId`.
    - Update the quantity and save.
4. **Remove Item**:
    - Delete the `CartItem` record.

## 10. Design Pattern Implementation Plan

### Repository Pattern
- Where: `CartRepository`, `CartItemRepository`
- Why: Standard JPA abstraction for H2.
- Main participants: `CartRepository`, `CartItemRepository`.

### DTO Pattern
- Where: `CartRequest`, `CartResponse`, `CartItemResponse`
- Why: Decouples internal entities from the API.
- Main participants: `CartRequest`, `CartResponse`, `CartItemResponse`.

### Facade / Application Service
- Where: `CartService`
- Why: Orchestrates calls to Product Service, Pricing Service, and local repositories.
- Main participants: `CartService`, `ProductServiceClient`, `PricingServiceClient`.

## 11. Remote Communication Plan

| Caller | Provider | Endpoint | Purpose | Failure Handling |
|--------|----------|----------|---------|------------------|
| Cart | Product | `/products/{id}` | Validate product existence | Fallback: Treat as 404 Not Found |
| Cart | Pricing | `/pricing/calculate-cart` | Get total cart cost | Circuit Breaker + Fallback: Mark total as unavailable |

- **Timeout**: 2-second read timeout.
- **Retry**: Retry once for GET requests (Product lookup).
- **Circuit Breaker**: Resilience4j to prevent hangs during pricing failures.

## 12. Exception Handling Plan

- **ProductNotFoundException**: Maps to 404 Not Found.
- **InvalidQuantityException**: Maps to 400 Bad Request.
- **CartNotFoundException**: Maps to 404 Not Found.
- **PricingServiceException**: Maps to 503 Service Unavailable.
- **Global Exception Handler**: Standardized JSON response.

## 13. Security Plan

- **Authentication**: All endpoints require a valid JWT.
- **Authorization**: `userId` in the JWT must match `{userId}` in the path (or user is ADMIN).
- **Sensitive Info**: No PII logged in the request logs.

## 14. Testing Plan

### Unit Tests
- `CartService` business logic (quantity increments, item additions).
- Request validation.

### Controller Tests
- Endpoint mapping, JWT security checks, and request validation.

### Repository Tests
- H2 persistence for `Cart` and `CartItem`.

### Integration Tests
- End-to-end flow: Add item $\rightarrow$ Update quantity $\rightarrow$ Get Cart.

### Contract / Remote Tests
- Mock `ProductService` and `PricingService` using WireMock to test Feign clients.

## 15. Files to Create

```text
src/main/java/com/ecommerce/cart_service/
├── CartServiceApplication.java
├── controller/CartController.java
├── service/CartService.java
├── service/CartServiceImpl.java
├── repository/CartRepository.java
├── repository/CartItemRepository.java
├── entity/Cart.java
├── entity/CartItem.java
├── dto/CartRequest.java
├── dto/CartResponse.java
├── dto/CartItemResponse.java
└── exception/CartNotFoundException.java
└── exception/ProductNotFoundException.java
└── exception/GlobalExceptionHandler.java
```

## 16. Files to Modify

None.

## 17. Implementation Sequence

1. Create Maven project and add dependencies.
2. Define `Cart` and `CartItem` entities.
3. Create `CartRepository` and `CartItemRepository`.
4. Create Request/Response DTOs.
5. Implement OpenFeign clients for `ProductService` and `PricingService`.
6. Implement `CartService` business logic.
7. Implement `CartController`.
8. Implement Global Exception Handler.
9. Configure `application.properties`.
10. Add Unit and Integration tests.
11. Run `./mvnw clean test` to verify.

## 18. Verification Plan

- Run `./mvnw clean test`.
- Run `./mvnw help:effective-pom`.

## 19. Risks and Failure Scenarios

- **Pricing Service Down**: Cart cannot show totals. Handled by circuit breaker and fallback.
- **Product Service Down**: Cannot add items to cart. Handled by 503 response.
- **Concurrent Cart Updates**: Multiple requests to update the same cart. Handled by `@Transactional`.

## 20. Acceptance Criteria

- [ ] All endpoints are implemented and working.
- [ ] Request validation (quantity > 0) is enforced.
- [ ] Database constraints (userId uniqueness) are enforced.
- [ ] Product existence is verified via Product Service.
- [ ] Cart totals are retrieved from Pricing Service.
- [ ] OpenFeign is used for all remote calls.
- [ ] Circuit breaker/fallback is implemented for Pricing Service.
- [ ] No direct access to other service databases.
- [ ] All tests pass.
- [ ] No Kafka/RabbitMQ introduced.

## 21. Planning Issues

None.

## 22. Implementation Prompt

> Implement `Cart Service` according to `.claude/specs/11-cart-service.md` and this plan. Read the root `CLAUDE.md`. Inspect the Product and Pricing services to understand their contracts. Follow the implementation sequence and acceptance criteria. Do not introduce Kafka/RabbitMQ. Verify the build and tests before completion.
