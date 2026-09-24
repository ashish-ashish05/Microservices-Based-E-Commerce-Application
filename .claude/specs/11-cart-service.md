# 11. Cart Service Specification

## 1. Document Status

- Service: Cart Service
- Build Order: 11
- Assignment: Shopping Cart Management
- Status: Proposed
- Scope: Specification only

## 2. Purpose

The Cart Service is responsible for managing a user's temporary collection of products they intend to purchase. It acts as the staging area before an order is created, allowing users to add, remove, and modify items while maintaining a current view of their potential purchase.

## 3. Responsibilities

- Create and manage a user's shopping cart.
- Add products to the cart.
- Update item quantities in the cart.
- Remove items from the cart.
- Retrieve the current state of a user's cart.
- Clear the entire cart.
- Validate product existence and availability via the Product Service.
- Calculate current cart totals by interacting with the Pricing Service.

## 4. Non-Responsibilities

- **Product Data**: Does not own product details (name, description, etc.); references them by ID.
- **Pricing Logic**: Does not calculate prices or apply discounts; delegates this to the Pricing Service.
- **Order Management**: Does not create orders; the Order Service will convert a cart into an order.
- **Inventory Reservation**: Does not reserve stock; this is handled during the order process.
- **Payment Processing**: Does not handle payments.

## 5. Service Boundary

- **Data Ownership**: Owns the `Cart` and `CartItem` entities.
- **Domain Ownership**: Manages the "Shopping Cart" domain.
- **External Dependencies**: 
    - `Product Service`: To validate that products exist.
    - `Pricing Service`: To get current prices for cart items.
- **Boundary Logic**: The boundary exists to decouple the temporary nature of a shopping cart from the permanent nature of an order and the complex logic of pricing.

## 6. Architecture

Client
  |
API Gateway
  |
Cart Service
  |--------------------|
  v                    v
Product Service   Pricing Service

## 7. Service-to-Service Communication

| Consumer | Provider | Method | Endpoint | Purpose | Failure Consideration |
|----------|----------|--------|----------|---------|-----------------------|
| Cart | Product | GET | `/products/{id}` | Validate product exists | Fallback: Treat product as unavailable/error |
| Cart | Pricing | POST | `/pricing/calculate` | Get total for cart items | Fallback: Use cached price or return error |

### Synchronous Communication Details
- **Timeouts**: Strict 2-second read timeout for all OpenFeign calls.
- **Retries**: Retry once for idempotent GET requests (Product lookup).
- **Circuit Breaker**: Use Resilience4j to open the circuit if Pricing Service is down, preventing cart hangs.
- **Fallback**: If Pricing Service is unavailable, the cart can still be viewed, but the "Total Price" will be marked as "Unavailable".

## 8. API Specification

### Get Cart
- Method: `GET`
- Path: `/carts/{userId}`
- Purpose: Retrieve the current cart for a user.
- Authentication: JWT Required
- Authorization: User must be the owner or Admin.
- Success response: `CartResponse` DTO
- Error responses: `404 Not Found` if cart doesn't exist.

### Add Item to Cart
- Method: `POST`
- Path: `/carts/{userId}/items`
- Purpose: Add a product to the cart.
- Authentication: JWT Required
- Request body: `{ "productId": "UUID", "quantity": 1 }`
- Success response: `201 Created` with updated `CartResponse`.
- Error responses: `400 Bad Request` (invalid quantity), `404 Not Found` (product doesn't exist).

### Update Item Quantity
- Method: `PUT`
- Path: `/carts/{userId}/items/{productId}`
- Purpose: Update the quantity of a specific item.
- Authentication: JWT Required
- Request body: `{ "quantity": 5 }`
- Success response: `200 OK` with updated `CartResponse`.
- Error responses: `400 Bad Request`, `404 Not Found`.

### Remove Item from Cart
- Method: `DELETE`
- Path: `/carts/{userId}/items/{productId}`
- Purpose: Remove a specific product from the cart.
- Authentication: JWT Required
- Success response: `204 No Content`.
- Error responses: `404 Not Found`.

### Clear Cart
- Method: `DELETE`
- Path: `/carts/{userId}`
- Purpose: Remove all items from the cart.
- Authentication: JWT Required
- Success response: `204 No Content`.

## 9. Data Model

### Cart Entity
| Field | Type | Required | Constraints | Description |
|------|------|----------|-------------|-------------|
| id | UUID | Yes | Primary Key | Unique Cart ID |
| userId | String | Yes | Unique, Indexed | Reference to User Service ID |
| createdAt | LocalDateTime | Yes | | Creation timestamp |
| updatedAt | LocalDateTime | Yes | | Last update timestamp |

### CartItem Entity
| Field | Type | Required | Constraints | Description |
|------|------|----------|-------------|-------------|
| id | UUID | Yes | Primary Key | Unique Item ID |
| cartId | UUID | Yes | FK to Cart | Reference to parent cart |
| productId | UUID | Yes | Indexed | Reference to Product Service ID |
| quantity | Integer | Yes | > 0 | Number of items |

## 10. DTOs

### CartRequest
- Purpose: Adding/updating items.
- Fields: `productId` (UUID, required), `quantity` (Integer, > 0).

### CartResponse
- Purpose: Returning cart state.
- Fields: `userId`, `items` (List of `CartItemResponse`), `totalPrice` (BigDecimal), `totalItems` (Integer).

### CartItemResponse
- Purpose: Detailed item info.
- Fields: `productId`, `quantity`, `unitPrice` (from Pricing Service), `subTotal` (from Pricing Service).

## 11. Business Rules

- A user can only have one active cart.
- Item quantity must be at least 1.
- Adding an existing product to the cart increments the quantity.
- Product existence must be verified with the Product Service before adding/updating.
- Cart total must be calculated by the Pricing Service, not internally.

## 12. Design Patterns

### Pattern: Repository Pattern
- Where: `CartRepository`, `CartItemRepository`.
- Why: Standard abstraction for H2 data access.
- Problem it solves: Decouples business logic from persistence technology.

### Pattern: DTO Pattern
- Where: `CartRequest`, `CartResponse`.
- Why: Prevents leaking database entities to the API.
- Problem it solves: API contract stability and security.

### Pattern: Facade / Application Service
- Where: `CartService`.
- Why: Coordinates calls between Product Service, Pricing Service, and local repositories.
- Problem it solves: Simplifies the controller and centralizes orchestration.

## 13. Dependencies

### Required
- `spring-boot-starter-web`: REST APIs.
- `spring-boot-starter-data-jpa`: Persistence.
- `spring-boot-starter-validation`: Input validation.
- `spring-cloud-starter-openfeign`: Sync communication.
- `com.h2database:h2`: Development DB.
- `org.projectlombok:lombok`: Boilerplate reduction.
- `spring-boot-starter-actuator`: Health checks.

### Optional / Later
- `spring-boot-starter-data-redis`: For cart caching to improve performance.

## 14. Configuration

```properties
server.port=8085
spring.application.name=cart-service
# Database
spring.datasource.url=jdbc:h2:mem:cart-db
# Remote Services
product-service.url=http://localhost:8081
pricing-service.url=http://localhost:8086
```

## 15. Security

- **Authentication**: All endpoints require a valid JWT.
- **Authorization**: 
    - Users can only access/modify their own cart (`userId` in JWT must match `{userId}` in path).
    - Admin role can access any cart.
- **Sensitive Info**: Do not log User IDs in a way that violates privacy; use correlation IDs for tracing.

## 16. Exception and Error Handling

| Exception | Trigger | HTTP Status | Response Shape |
|-----------|---------|-------------|----------------|
| `ProductNotFoundException` | Product ID not found in Product Service | 404 | `{ "error": "Product not found" }` |
| `InvalidQuantityException` | Quantity <= 0 | 400 | `{ "error": "Quantity must be positive" }` |
| `CartNotFoundException` | User cart not found | 404 | `{ "error": "Cart not found" }` |
| `PricingServiceException` | Pricing service unavailable | 503 | `{ "error": "Price calculation unavailable" }` |

## 17. Transaction Boundaries

- `addItem`: Transactional. Must ensure the CartItem is saved correctly.
- `updateQuantity`: Transactional.
- `clearCart`: Transactional. Must remove all items atomically.

## 18. Validation

- **Request Fields**: `productId` must not be null; `quantity` must be > 0.
- **State Transitions**: Cannot update quantity for a product not already in the cart.
- **Cross-Service**: Verify `productId` exists in Product Service before saving to `CartItem`.

## 19. Testing Strategy

### Unit Tests
- `CartService` business logic (e.g., quantity increments).
- Validation logic.

### Controller Tests
- Endpoint mapping, JWT security checks, and request validation.

### Repository Tests
- H2 persistence for `Cart` and `CartItem`.

### Integration Tests
- End-to-end flow: Add item -> Update quantity -> Get Cart.

### Contract / Remote Tests
- Mock `ProductService` and `PricingService` using WireMock to test Feign clients.

### Failure Tests
- Pricing Service timeout (verify fallback).
- Product Service returning 404 (verify error response).

## 20. Observability

- **Logs**: Log every addition/removal of items with User ID and Product ID.
- **Health**: Actuator `/health` and `/info`.
- **Metrics**: Track "Cart Abandonment Rate" (carts created vs carts converted to orders - though conversion is tracked in Order Service).

## 21. Implementation Sequence

1. Create Maven project and add dependencies (`web`, `jpa`, `validation`, `openfeign`, `h2`, `lombok`, `actuator`).
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

## 22. Acceptance Criteria

- [ ] All endpoints (`GET`, `POST`, `PUT`, `DELETE`) are implemented and working.
- [ ] Request validation (e.g., quantity > 0) is enforced.
- [ ] Database constraints (User ID uniqueness) are enforced.
- [ ] Product existence is verified via Product Service.
- [ ] Cart totals are retrieved from Pricing Service.
- [ ] OpenFeign is used for all remote calls.
- [ ] Circuit breaker/fallback is implemented for Pricing Service.
- [ ] No direct access to other service databases.
- [ ] All tests pass (`./mvnw clean test`).
- [ ] No Kafka/RabbitMQ introduced.

## 23. Out of Scope

- Persistent storage beyond H2 (PostgreSQL is a later phase).
- Redis caching (deferred until performance requirements are defined).
- Complex discount logic (delegated to Pricing Service).

## 24. Decisions and Open Questions

- **Decision**: Synchronous calls to Product/Pricing services are acceptable for this learning phase.
- **Decision**: The Cart Service will not store the current price of a product; it will always request the latest price from the Pricing Service.
- **Question**: Should the cart be cleared automatically after a certain period of inactivity? (Current spec: No, manual clear or order conversion).

## 25. Recommended Git Branch

Suggested Branch: `feature/cart-service`
Please switch to this branch before starting implementation.
