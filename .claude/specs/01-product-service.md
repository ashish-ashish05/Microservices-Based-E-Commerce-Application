# 01. Product Service Specification

## 1. Document Status

- Service: Product Service
- Build Order: 01
- Assignment: 1
- Status: Proposed
- Scope: Specification only

## 2. Purpose

The Product Service is the central authority for product identity and information in the e-commerce platform. It manages the core product catalog and provides aggregated product views by orchestrating calls to the Category and Inventory services.

## 3. Responsibilities

- Create and persist new products.
- Maintain product identity (UUID).
- Update product details.
- Retrieve a single product's detailed information.
- List all available products.
- Coordinate the initial creation of inventory when a product is created.
- Aggregate category and inventory data into a comprehensive product response.

## 4. Non-Responsibilities

- Managing category hierarchies (owned by Category Service).
- Managing stock levels or reservations (owned by Inventory Service).
- Calculating dynamic pricing or discounts (owned by Pricing Service).
- Processing orders or payments.

## 5. Service Boundary

- **Data Ownership**: Owns the `Product` entity.
- **Domain Ownership**: Owns the Product domain.
- **External Dependencies**: 
    - Category Service (for category names).
    - Inventory Service (for stock status and initial creation).
- **Boundary Justification**: Separating product identity from stock levels allows inventory to scale independently and allows categories to be managed as a separate taxonomic hierarchy.

## 6. Architecture

Client
  |
API Gateway
  |
Product Service
  |
  +---> Category Service (Synchronous REST)
  |
  +---> Inventory Service (Synchronous REST)

## 7. Service-to-Service Communication

| Consumer | Provider | Method | Endpoint | Purpose | Failure Consideration |
|----------|----------|--------|----------|---------|-----------------------|
| Product  | Category | GET    | `/categories/{id}` | Fetch category name | Fallback to "Unknown Category" or throw error |
| Product  | Inventory| GET    | `/inventory/{productId}` | Check stock status | Fallback to `inStock = false` |
| Product  | Inventory| POST   | `/inventory` | Create initial inventory | Log failure; product is created regardless |

**Synchronous Communication Policy**:
- **Timeouts**: 2-second read timeout for remote calls.
- **Retry**: No retries for POST requests to avoid duplicates.
- **Circuit Breaker**: Resilience4j to be implemented for `Category` and `Inventory` calls to prevent cascading failures.
- **Fallback**: Provide default values (e.g., `inStock: false`) when Inventory Service is unavailable.

## 8. API Specification

### Create Product

- Method: `POST`
- Path: `/products`
- Purpose: Create a new product and trigger inventory creation.
- Authentication: Admin only (to be implemented via Gateway/JWT).
- Authorization: Admin.
- Request body: `ProductRequest`
- Success response: `200 OK` with `ProductResponse`.
- Error responses: `400 Bad Request` (Validation failure).

### Get Product

- Method: `GET`
- Path: `/products/{id}`
- Purpose: Retrieve detailed product info including category name and stock status.
- Authentication: Public.
- Authorization: Public.
- Path parameters: `id` (UUID).
- Success response: `200 OK` with `ProductResponse`.
- Error responses: `404 Not Found` (Product not found).

### List Products

- Method: `GET`
- Path: `/products`
- Purpose: List all products with aggregated data.
- Authentication: Public.
- Authorization: Public.
- Success response: `200 OK` with `List<ProductResponse>`.
- Error responses: `500 Internal Server Error`.

## 9. Data Model

**Entity: Product**

| Field | Type | Required | Constraints | Description |
|------|------|----------|-------------|-------------|
| id | UUID | Yes | Primary Key | Unique identifier for the product |
| name | String | Yes | Not Blank | Display name of the product |
| description | String | No | - | Detailed description |
| price | BigDecimal | Yes | Not Null | Unit price of the product |
| categoryId | UUID | Yes | Not Null | Reference to the category |

## 10. DTOs

### ProductRequest
- Purpose: Create product payload.
- Fields: `name`, `description`, `price`, `categoryId`.
- Validation: `name` (@NotBlank), `price` (@NotNull), `categoryId` (@NotNull).

### ProductResponse
- Purpose: Aggregated product view.
- Fields: `id`, `name`, `description`, `price`, `categoryName`, `inStock`.
- Validation: None.
- Security: No internal database IDs or sensitive pricing logic exposed.

### InventoryRequest
- Purpose: Request inventory creation.
- Fields: `productId`, `quantity`.

### InventoryResponse
- Purpose: Receive stock info.
- Fields: `productId`, `quantity`.

### CategoryResponse
- Purpose: Receive category info.
- Fields: `id`, `name`.

## 11. Business Rules

- **Automatic Inventory**: When a product is created, the service must send a request to the Inventory Service to create an inventory record with `quantity = 0`.
- **Data Aggregation**: The `ProductResponse` must not contain the `categoryId` but must instead contain the `categoryName` fetched from the Category Service.
- **Stock Status**: `inStock` is `true` if Inventory Service reports `quantity > 0`.
- **Identity**: Product UUID is generated by the Product Service.

## 12. Design Patterns

### Pattern: Repository Pattern
- Where: `ProductRepository`
- Why: Abstracts the data access layer.
- Problem it solves: Decouples business logic from H2/JPA implementation.

### Pattern: DTO Pattern
- Where: `ProductRequest`, `ProductResponse`
- Why: Prevents leaking internal entity structure to the API.
- Problem it solves: Decouples the API contract from the database schema.

### Pattern: Factory Pattern
- Where: `ProductFactory`
- Why: Centralizes the creation logic for `Product` entities from `ProductRequest` DTOs.
- Problem it solves: Keeps the service layer clean of mapping boilerplate.

### Pattern: Facade / Aggregation
- Where: `ProductServiceImpl`
- Why: Combines data from three different sources (Local DB, Category Service, Inventory Service).
- Problem it solves: Simplifies client interaction by providing a single endpoint for a complete product view.

## 13. Dependencies

### Required
- `spring-boot-starter-web`: For REST APIs.
- `spring-boot-starter-data-jpa`: For H2 persistence.
- `spring-boot-starter-validation`: For request validation.
- `spring-cloud-starter-openfeign`: For synchronous calls to Category and Inventory services.
- `com.h2database:h2`: Development database.
- `org.projectlombok:lombok`: Boilerplate reduction.

### Optional / Later
- `spring-cloud-starter-circuitbreaker-resilience4j`: For failure handling of remote calls.

## 14. Configuration

```properties
server.port=8081
spring.datasource.url=jdbc:h2:mem:product-db
category.service.url=http://localhost:8082
inventory.service.url=http://localhost:8083
```

## 15. Security

- **Authentication**: Integrated via API Gateway (JWT).
- **Authorization**:
    - `POST /products`: Requires `ADMIN` role.
    - `GET /products/**`: Public access.
- **Sensitive Data**: No database internals exposed in responses.

## 16. Exception and Error Handling

| Exception | Trigger | HTTP Status | Response Shape | Handling |
|-----------|---------|-------------|---------------|-----------|
| ProductNotFoundException | Product ID not found in DB | 404 Not Found | `{ "error": "Product not found", "id": "..." }` | Global Handler |
| MethodArgumentNotValidException | Validation constraints fail | 400 Bad Request | `{ "errors": [...] }` | Global Handler |

## 17. Transaction Boundaries

- **Product Creation**: Local transaction covers the saving of the `Product` entity. The call to `InventoryClient` is outside the local database transaction to avoid holding locks during remote I/O.

## 18. Validation

- **Request Validation**: `name` must not be blank, `price` and `categoryId` must be provided.
- **Cross-Service Validation**: While the service assumes `categoryId` is valid, failure to fetch category details results in a fallback or error.

## 19. Testing Strategy

### Unit Tests
- `ProductFactory` mapping logic.
- `ProductServiceImpl` business logic using Mockito for clients/repositories.

### Controller Tests
- Request validation (400 errors).
- Correct HTTP status codes for success and 404 scenarios.

### Repository Tests
- H2 persistence for `Product` entity.

### Integration Tests
- Full flow from Controller to Repository using `@SpringBootTest`.

### Contract / Remote Tests
- OpenFeign client interactions using `@FeignClient` mocks or WireMock.

### Failure Tests
- Behavior when Category Service returns 404.
- Behavior when Inventory Service is timed out.

## 20. Observability

- **Logs**: Log product creation, updates, and remote call failures.
- **Health**: Actuator `/health` endpoint.
- **Metrics**: Track product retrieval latency and remote call failure rates.

## 21. Implementation Sequence

1. Initialize Maven project with Spring Boot 3.2.5.
2. Add required dependencies (Web, JPA, Validation, OpenFeign, H2, Lombok).
3. Implement `Product` entity and `ProductRepository`.
4. Implement `ProductRequest` and `ProductResponse` DTOs.
5. Implement `ProductFactory` for mapping.
6. Implement `CategoryClient` and `InventoryClient` using OpenFeign.
7. Implement `ProductService` interface and `ProductServiceImpl`.
8. Implement `ProductController` with REST endpoints.
9. Implement `ProductNotFoundException` and global exception handler.
10. Configure `application.properties`.
11. Add Unit, Controller, and Integration tests.
12. Run `./mvnw clean test` to verify.
13. Update OpenAPI/Swagger documentation.

## 22. Acceptance Criteria

- [ ] `POST /products` creates a product and triggers inventory creation.
- [ ] `GET /products/{id}` returns aggregated category name and stock status.
- [ ] `GET /products` lists all products with aggregated data.
- [ ] Validation on `ProductRequest` is enforced.
- [ ] Remote calls use OpenFeign.
- [ ] `ProductNotFoundException` returns 404.
- [ ] No direct access to Category or Inventory databases.
- [ ] Tests cover success and failure cases.
- [ ] `./mvnw clean test` passes.
- [ ] Dependency versions follow `CLAUDE.md`.

## 23. Out of Scope

- Implementing the actual Category or Inventory logic.
- Implementing JWT validation (handled by Gateway).
- Implementing complex pricing logic.

## 24. Decisions and Open Questions

- **Decision**: Synchronous aggregation is used instead of a read-model/CQRS for simplicity in this learning phase.
- **Decision**: Product creation is not rolled back if inventory creation fails; instead, the failure is logged.
- **Question**: Should the Product Service verify the `categoryId` exists via a call to Category Service *before* saving the product? (Currently, it's assumed valid).

## 25. Recommended Git Branch

Suggested Branch: `feature/product-service`

Please switch to this branch before starting implementation.
