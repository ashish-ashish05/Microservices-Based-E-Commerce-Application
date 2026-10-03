# 03. Inventory Service Implementation Plan

## 1. Plan Metadata

- Service: Inventory Service
- Build Order: 03
- Assignment: 1
- Specification: .claude/specs/03-inventory-service.md
- Status: Planned
- Plan Generated: 2026-10-03
- Prerequisite Services: None

## 2. Objective

The Inventory Service will track stock levels for all products, providing APIs to update stock, check availability, and manage stock reservations and releases during the order lifecycle.

## 3. Existing Context

The Inventory Service is a leaf service in the current dependency graph. It is consumed by the Product Service (Port 8081) and Order Service (planned) for stock management.

## 4. Scope

### In Scope
- Inventory CRUD operations (Create, Update, Get).
- Stock reservation (decrement) and release (increment) operations.
- Implementation of the Strategy pattern for stock availability.
- Implementation of Repository and DTO patterns.
- Global exception handling for 404 and 400 errors.
- Unit, Controller, and Integration tests.

### Out of Scope
- Product details (handled by Product Service).
- Category management (handled by Category Service).
- Multi-warehouse inventory.
- Real-time warehouse integration.

## 5. Architecture

Client
  |
API Gateway
  |
Inventory Service
  |
(H2 Database)

The Inventory Service owns the `Inventory` entity and manages stock quantities per `productId`.

## 6. Implementation Dependencies

- **Maven Dependencies**:
    - `spring-boot-starter-web`
    - `spring-boot-starter-data-jpa`
    - `spring-boot-starter-validation`
    - `com.h2database:h2`
    - `org.projectlombok:lombok`
    - `spring-boot-starter-actuator`
- **Configuration**:
    - `server.port=8083`
    - `spring.datasource.url=jdbc:h2:mem:inventory-db`

## 7. Data Model Plan

| Entity | Field | Type | Constraint | Notes |
|--------|-------|------|------------|-------|
| Inventory | id | UUID | Primary Key | Internal ID |
| Inventory | productId | UUID | Unique, Not Null | Reference to Product Service |
| Inventory | quantity | int | Min(0) | Current stock level |

## 8. API Implementation Plan

### Create or Update Inventory
- HTTP method: `POST`
- path: `/inventory`
- request DTO: `InventoryRequest` (productId, quantity)
- validation: `productId` (@NotNull), `quantity` (@Min(0))
- response DTO: `InventoryResponse`
- status codes: `200 OK`, `400 Bad Request`
- authentication/authorization: ADMIN
- downstream calls: None
- error behavior: Return 400 if quantity is negative.

### Get Inventory
- HTTP method: `GET`
- path: `/inventory/{productId}`
- request DTO: N/A
- validation: UUID format for `{productId}`
- response DTO: `InventoryResponse` (productId, quantity, inStock)
- status codes: `200 OK`, `404 Not Found`
- authentication/authorization: Public
- downstream calls: None
- error behavior: Throw `InventoryNotFoundException`.

### Reserve Stock
- HTTP method: `PUT`
- path: `/inventory/{productId}/reserve`
- request DTO: N/A (query params: `quantity`)
- validation: `quantity` > 0
- response DTO: N/A
- status codes: `200 OK`, `400 Bad Request` (insufficient stock), `404 Not Found`
- authentication/authorization: Order Service / Admin
- downstream calls: None
- error behavior: Throw `IllegalStateException` if quantity < requested.

### Release Stock
- HTTP method: `PUT`
- path: `/inventory/{productId}/release`
- request DTO: N/A (query params: `quantity`)
- validation: `quantity` > 0
- response DTO: N/A
- status codes: `200 OK`, `404 Not Found`
- authentication/authorization: Order Service / Admin
- downstream calls: None
- error behavior: Throw `InventoryNotFoundException`.

## 9. Service Layer Plan

1. **Create/Update Inventory**:
    - Save or update the `Inventory` record for the given `productId`.
2. **Get Inventory**:
    - Retrieve `Inventory` by `productId`.
    - Use `StockStrategy` to determine `inStock` status.
    - Map to `InventoryResponse`.
3. **Reserve Stock**:
    - Wrap in `@Transactional`.
    - Retrieve `Inventory` by `productId`.
    - Verify `quantity >= requestedQuantity`.
    - Decrement quantity and save.
4. **Release Stock**:
    - Wrap in `@Transactional`.
    - Retrieve `Inventory` by `productId`.
    - Increment quantity and save.

## 10. Design Pattern Implementation Plan

### Strategy Pattern
- Where: `StockStrategy` (Interface) and `DefaultStockStrategy` (Implementation)
- Why: To decouple the definition of "availability" from the service logic.
- Main participants: `StockStrategy`, `DefaultStockStrategy`.
- Expected behavior: `isAvailable(int quantity)` returns true if `quantity > 0`.

### Repository Pattern
- Where: `InventoryRepository`
- Why: Standard JPA abstraction.
- Main participants: `InventoryRepository` extending `JpaRepository`.

### DTO Pattern
- Where: `InventoryRequest`, `InventoryResponse`
- Why: Separates internal entity from API contract.
- Main participants: `InventoryRequest`, `InventoryResponse`.

## 11. Remote Communication Plan

No remote calls are made by the Inventory Service.

## 12. Exception Handling Plan

- **InventoryNotFoundException**: Thrown when `productId` is not found in inventory. Maps to 404 Not Found.
- **IllegalStateException**: Thrown when insufficient stock exists for reservation. Maps to 400 Bad Request.
- **Global Exception Handler**: Uses `@RestControllerAdvice` to map exceptions to a standardized error response shape: `{ "error": "...", "productId": "..." }`.

## 13. Security Plan

- **Authentication**: Handled by API Gateway via JWT.
- **Authorization**: `POST /inventory` requires `ADMIN` role. Public access for `GET` endpoints. Internal access for `PUT` (Order Service).
- **Sensitive Data**: None.

## 14. Testing Plan

### Unit Tests
- `InventoryServiceImpl` reservation and release logic.
- `DefaultStockStrategy` availability check.

### Controller Tests
- `InventoryController` endpoint mapping and request validation (400 errors).
- Response status codes for success and 404.

### Repository Tests
- `InventoryRepository` using H2 in-memory database.

### Integration Tests
- Full flow from Controller $\rightarrow$ Service $\rightarrow$ Repository.
- Concurrent reservation tests to verify transaction isolation.

## 15. Files to Create

```text
src/main/java/com/ecommerce/inventory_service/
├── InventoryServiceApplication.java
├── controller/InventoryController.java
├── service/InventoryService.java
├── service/InventoryServiceImpl.java
├── repository/InventoryRepository.java
├── entity/Inventory.java
├── dto/InventoryRequest.java
├── dto/InventoryResponse.java
├── strategy/StockStrategy.java
├── strategy/DefaultStockStrategy.java
└── exception/InventoryNotFoundException.java
└── exception/GlobalExceptionHandler.java
```

## 16. Files to Modify

None.

## 17. Implementation Sequence

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

## 18. Verification Plan

- Run `./mvnw clean test` to verify all tests pass.
- Run `./mvnw help:effective-pom` to check dependency versions.

## 19. Risks and Failure Scenarios

- **Concurrency**: Multiple requests to reserve stock for the same product can lead to race conditions. Handled by `@Transactional` and JPA locking.
- **Insufficient Stock**: Requests to reserve more than available. Handled by `IllegalStateException`.

## 20. Acceptance Criteria

- [ ] `POST /inventory` creates or updates stock.
- [ ] `GET /inventory/{productId}` returns correct quantity and availability.
- [ ] `PUT /inventory/{productId}/reserve` decrements stock and prevents negative values.
- [ ] `PUT /inventory/{productId}/release` increments stock.
- [ ] Validation on `InventoryRequest` is enforced.
- [ ] `InventoryNotFoundException` returns 404.
- [ ] No direct access to Product or Category databases.
- [ ] Tests cover success and failure cases.
- [ ] `./mvnw clean test` passes.
- [ ] Dependency versions follow `CLAUDE.md`.

## 21. Planning Issues

None.

## 22. Implementation Prompt

> Implement `Inventory Service` according to `.claude/specs/03-inventory-service.md` and this plan. Read the root `CLAUDE.md`. Follow the implementation sequence and acceptance criteria. Do not introduce Kafka/RabbitMQ or unrelated infrastructure. Verify the build and tests before completion.
