# 03. Inventory Service Specification

## 1. Document Status

- Service: Inventory Service
- Build Order: 03
- Assignment: 1
- Status: Proposed
- Scope: Specification only

## 2. Purpose

The Inventory Service is responsible for tracking the stock levels of all products in the e-commerce platform. It provides APIs to update stock, check availability, and manage reservations/releases of stock during the order lifecycle.

## 3. Responsibilities

- Maintain the current quantity of stock for each product.
- Provide real-time stock availability checks.
- Create or update inventory records (typically triggered by Product Service).
- Reserve stock (decrement quantity) when an order is placed.
- Release stock (increment quantity) when an order is cancelled or payment fails.
- Enforce that stock cannot go below zero.

## 4. Non-Responsibilities

- Managing product details like name or price (owned by Product Service).
- Managing categories (owned by Category Service).
- Processing payments or shipping.

## 5. Service Boundary

- **Data Ownership**: Owns the `Inventory` entity.
- **Domain Ownership**: Owns the Inventory/Stock domain.
- **External Dependencies**: None (This is a leaf service).
- **Boundary Justification**: Isolating inventory allows the stock management logic (which is highly transactional) to be scaled and optimized independently from the product catalog.

## 6. Architecture

Client
  |
API Gateway
  |
Inventory Service
  |
(H2 Database)

(Product Service and Order Service call this service synchronously)

## 7. Service-to-Service Communication

This service does not call any other microservices. It acts as a provider.

| Consumer | Provider | Method | Endpoint | Purpose | Failure Consideration |
|----------|----------|--------|----------|---------|-----------------------|
| Product  | Inventory| GET    | `/inventory/{productId}` | Check availability | Handled by Product Service fallback |
| Product  | Inventory| POST   | `/inventory` | Initial stock creation | Logged by Product Service |
| Order    | Inventory| PUT    | `/inventory/{productId}/reserve` | Reserve items for order | Order Service triggers compensation |
| Order    | Inventory| PUT    | `/inventory/{productId}/release` | Restore stock on failure | Idempotency required |

## 8. API Specification

### Create or Update Inventory

- Method: `POST`
- Path: `/inventory`
- Purpose: Set or update the stock quantity for a product.
- Authentication: Admin only (implemented via Gateway/JWT).
- Authorization: Admin.
- Request body: `InventoryRequest`
- Success response: `200 OK` with `InventoryResponse`.
- Error responses: `400 Bad Request` (Validation failure).

### Get Inventory

- Method: `GET`
- Path: `/inventory/{productId}`
- Purpose: Retrieve the current stock level and availability status.
- Authentication: Public.
- Authorization: Public.
- Path parameters: `productId` (UUID).
- Success response: `200 OK` with `InventoryResponse`.
- Error responses: `404 Not Found` (Product not found in inventory).

### Reserve Stock

- Method: `PUT`
- Path: `/inventory/{productId}/reserve`
- Purpose: Decrement stock quantity for a pending order.
- Authentication: Internal (implemented via Gateway/JWT).
- Authorization: Order Service / Admin.
- Path parameters: `productId` (UUID).
- Query parameters: `quantity` (int).
- Success response: `200 OK`.
- Error responses: `400 Bad Request` (Insufficient stock), `404 Not Found`.

### Release Stock

- Method: `PUT`
- Path: `/inventory/{productId}/release`
- Purpose: Increment stock quantity (e.g., order cancellation).
- Authentication: Internal (implemented via Gateway/JWT).
- Authorization: Order Service / Admin.
- Path parameters: `productId` (UUID).
- Query parameters: `quantity` (int).
- Success response: `200 OK`.
- Error responses: `404 Not Found`.

## 9. Data Model

**Entity: Inventory**

| Field | Type | Required | Constraints | Description |
|------|------|----------|-------------|-------------|
| id | UUID | Yes | Primary Key | Internal ID |
| productId | UUID | Yes | Unique, Not Null | Reference to the Product |
| quantity | int | Yes | Min(0) | Current stock level |

## 10. DTOs

### InventoryRequest
- Purpose: Set stock quantity.
- Fields: `productId`, `quantity`.
- Validation: `productId` (@NotNull), `quantity` (@Min(0)).

### InventoryResponse
- Purpose: Stock status view.
- Fields: `productId`, `quantity`, `inStock`.
- Validation: None.

## 11. Business Rules

- **Non-Negative Stock**: Stock quantity can never be negative. Any attempt to reserve more than available must throw an `IllegalStateException` (400 Bad Request).
- **Product Linkage**: Every inventory record must be linked to a valid `productId`.
- **Availability**: A product is considered `inStock` if its quantity is greater than 0 (managed via `StockStrategy`).
- **Atomic Updates**: Reserve and Release operations must be atomic to prevent race conditions.

## 12. Design Patterns

### Pattern: Strategy Pattern
- Where: `StockStrategy` and `DefaultStockStrategy`
- Why: To decouple the definition of "availability" from the service logic.
- Problem it solves: Allows the business to change the definition of "in stock" (e.g., "Available if quantity > 5") without changing the service implementation.
- Key participants: `StockStrategy` (Interface), `DefaultStockStrategy` (Implementation).

### Pattern: Repository Pattern
- Where: `InventoryRepository`
- Why: Abstracts data access.
- Problem it solves: Decouples business logic from H2/JPA.

### Pattern: DTO Pattern
- Where: `InventoryRequest`, `InventoryResponse`
- Why: Separates internal entity from the API contract.
- Problem it solves: Prevents leaking internal `id` of the inventory record.

## 13. Dependencies

### Required
- `spring-boot-starter-web`: For REST APIs.
- `spring-boot-starter-data-jpa`: For H2 persistence.
- `spring-boot-starter-validation`: For request validation.
- `com.h2database:h2`: Development database.
- `org.projectlombok:lombok`: Boilerplate reduction.

### Optional / Later
- `spring-cloud-starter-circuitbreaker-resilience4j`: To be used by consumers of this service.

## 14. Configuration

```properties
server.port=8083
spring.datasource.url=jdbc:h2:mem:inventory-db
```

## 15. Security

- **Authentication**: Integrated via API Gateway (JWT).
- **Authorization**:
    - `POST /inventory`: Admin only.
    - `GET /inventory/**`: Public.
    - `PUT /inventory/**`: Order Service / Admin.
- **Sensitive Data**: None.

## 16. Exception and Error Handling

| Exception | Trigger | HTTP Status | Response Shape | Handling |
|-----------|---------|-------------|---------------|-----------|
| InventoryNotFoundException | Product ID not found in inventory | 404 Not Found | `{ "error": "Inventory not found", "productId": "..." }` | Global Handler |
| IllegalStateException | Insufficient stock for reservation | 400 Bad Request | `{ "error": "Insufficient Stock" }` | Global Handler |

## 17. Transaction Boundaries

- **Reserve/Release Stock**: These operations must be wrapped in `@Transactional` to ensure that the read-modify-write cycle is atomic and consistent.

## 18. Validation

- **Request Validation**: `productId` must be provided, `quantity` must be $\ge 0$.
- **Business Validation**: Ensure `quantity` does not drop below zero during reservation.

## 19. Testing Strategy

### Unit Tests
- `InventoryServiceImpl` stock reservation and release logic.
- `DefaultStockStrategy` availability check.

### Controller Tests
- Request validation (400 errors).
- Correct HTTP status codes for success and 404.

### Repository Tests
- H2 persistence for `Inventory` entity.
- Verification of unique constraint on `productId`.

### Integration Tests
- Full flow from Controller to Repository.
- Concurrent reservation tests to verify transaction isolation.

### Failure Tests
- Attempting to reserve more stock than available.
- Attempting to update inventory for a non-existent product.

## 20. Observability

- **Logs**: Log stock updates, reservations, and failures due to insufficient stock.
- **Health**: Actuator `/health` endpoint.
- **Metrics**: Track stock depletion rates and reservation failure rates.

## 21. Implementation Sequence

1. Initialize Maven project with Spring Boot 3.2.5.
2. Add required dependencies (Web, JPA, Validation, H2, Lombok).
3. Implement `Inventory` entity and `InventoryRepository`.
4. Implement `InventoryRequest` and `InventoryResponse` DTOs.
5. Implement `StockStrategy` and `DefaultStockStrategy`.
6. Implement `InventoryService` interface and `InventoryServiceImpl`.
7. Implement `InventoryController` with REST endpoints.
8. Implement `InventoryNotFoundException` and global exception handler.
9. Configure `application.properties`.
10. Add Unit, Controller, and Integration tests.
11. Run `./mvnw clean test` to verify.
12. Update OpenAPI/Swagger documentation.

## 22. Acceptance Criteria

- [ ] `POST /inventory` creates or updates stock.
- [ ] `GET /inventory/{productId}` returns correct quantity and availability.
- [ ] `PUT /inventory/{productId}/reserve` decrements stock and prevents negative values.
- [ ] `PUT /inventory/{productId}/release` increments stock.
- [ ] Validation on `InventoryRequest` is enforced.
- [ ] `InventoryNotFoundException` returns 404.
- [ ] No direct access to Product or Category databases.
- [ ] Tests cover success and failure cases (especially concurrency).
- [ ] `./mvnw clean test` passes.
- [ ] Dependency versions follow `CLAUDE.md`.

## 23. Out of Scope

- Integrating with a real warehouse management system (WMS).
- Handling multi-warehouse inventory.
- Implementing a complex auditing log of every stock change.

## 24. Decisions and Open Questions

- **Decision**: The Strategy pattern is used for `isAvailable` to allow future business changes to "availability" (e.g., safety stock thresholds).
- **Decision**: Use of `@Transactional` on the service implementation to ensure atomicity of stock updates.
- **Question**: Should we implement a "Soft Reserve" (hold stock for X minutes) or "Hard Reserve" (immediate decrement)? (Currently implements Hard Reserve).

## 25. Recommended Git Branch

Suggested Branch: `feature/inventory-service`

Please switch to this branch before starting implementation.
