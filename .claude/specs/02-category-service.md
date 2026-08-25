# 02. Category Service Specification

## 1. Document Status

- Service: Category Service
- Build Order: 02
- Assignment: 1
- Status: Proposed
- Scope: Specification only

## 2. Purpose

The Category Service manages the product categorization system. It provides the ability to create categories and organize them into a hierarchical tree structure, allowing products to be grouped logically for browsing and filtering.

## 3. Responsibilities

- Create and manage categories.
- Maintain parent-child relationships between categories.
- Build and provide a hierarchical category tree (Composite pattern).
- Provide category details to the Product Service for aggregated product views.
- Ensure category names are unique.

## 4. Non-Responsibilities

- Managing products associated with categories (owned by Product Service).
- Managing inventory for categories.
- Handling user-specific category preferences.

## 5. Service Boundary

- **Data Ownership**: Owns the `Category` entity.
- **Domain Ownership**: Owns the Category domain.
- **External Dependencies**: None (This is a leaf service in the current dependency graph).
- **Boundary Justification**: Decoupling category management allows the taxonomy to evolve independently of the product catalog.

## 6. Architecture

Client
  |
API Gateway
  |
Category Service
  |
(H2 Database)

(Product Service calls this service synchronously)

## 7. Service-to-Service Communication

This service does not call any other microservices. It acts as a provider.

| Consumer | Provider | Method | Endpoint | Purpose | Failure Consideration |
|----------|----------|--------|----------|---------|-----------------------|
| Product  | Category | GET    | `/categories/{id}` | Fetch category name | Handled by Product Service fallback |

## 8. API Specification

### Create Category

- Method: `POST`
- Path: `/categories`
- Purpose: Create a new category.
- Authentication: Admin only (implemented via Gateway/JWT).
- Authorization: Admin.
- Request body: `CategoryRequest`
- Success response: `200 OK` with `CategoryResponse`.
- Error responses: `400 Bad Request` (Validation failure or duplicate name), `500 Internal Server Error`.

### Get Category

- Method: `GET`
- Path: `/categories/{id}`
- Purpose: Retrieve a specific category's details.
- Authentication: Public.
- Authorization: Public.
- Path parameters: `id` (UUID).
- Success response: `200 OK` with `CategoryResponse`.
- Error responses: `404 Not Found` (Category not found).

### Get Category Tree

- Method: `GET`
- Path: `/categories/tree`
- Purpose: Retrieve the entire category hierarchy as a nested tree.
- Authentication: Public.
- Authorization: Public.
- Success response: `200 OK` with `List<CategoryNode>`.
- Error responses: `500 Internal Server Error`.

## 9. Data Model

**Entity: Category**

| Field | Type | Required | Constraints | Description |
|------|------|----------|-------------|-------------|
| id | UUID | Yes | Primary Key | Unique identifier for the category |
| name | String | Yes | Not Blank, Unique | Name of the category |
| parentCategoryId | UUID | No | - | Reference to the parent category (self-reference) |

## 10. DTOs

### CategoryRequest
- Purpose: Payload for creating a category.
- Fields: `name`, `parentCategoryId`.
- Validation: `name` (@NotBlank).

### CategoryResponse
- Purpose: Simple category view.
- Fields: `id`, `name`.

### CategoryNode
- Purpose: Recursive structure for the category tree.
- Fields: `id`, `name`, `subcategories` (List of `CategoryNode`).
- Validation: None.

## 11. Business Rules

- **Uniqueness**: Category names must be unique across the entire system.
- **Hierarchy**: A category can have one parent and multiple children.
- **Root Categories**: Categories without a `parentCategoryId` are considered root categories.
- **Circular References**: A category cannot be its own parent or an ancestor of its own parent (to be enforced during creation/update).

## 12. Design Patterns

### Pattern: Composite Pattern
- Where: `CategoryNode` and `CategoryServiceImpl.getCategoryTree()`
- Why: To represent a part-whole hierarchy where individual categories and groups of categories are treated uniformly.
- Problem it solves: Allows the client to receive a nested tree structure instead of a flat list that would require client-side processing.
- Key participants: `CategoryNode` (Composite object).

### Pattern: Repository Pattern
- Where: `CategoryRepository`
- Why: Abstracts data access.
- Problem it solves: Decouples the service layer from the H2 database.

### Pattern: DTO Pattern
- Where: `CategoryRequest`, `CategoryResponse`, `CategoryNode`
- Why: Separates the internal entity from the API contract.
- Problem it solves: Prevents internal database structure (like `parentCategoryId`) from being leaked unless explicitly intended.

## 13. Dependencies

### Required
- `spring-boot-starter-web`: For REST APIs.
- `spring-boot-starter-data-jpa`: For H2 persistence.
- `spring-boot-starter-validation`: For request validation.
- `com.h2database:h2`: Development database.
- `org.projectlombok:lombok`: Boilerplate reduction.

### Optional / Later
- `springdoc-openapi`: For API documentation.

## 14. Configuration

```properties
server.port=8082
spring.datasource.url=jdbc:h2:mem:category-db
```

## 15. Security

- **Authentication**: Integrated via API Gateway (JWT).
- **Authorization**:
    - `POST /categories`: Requires `ADMIN` role.
    - `GET /categories/**`: Public access.
- **Sensitive Data**: None.

## 16. Exception and Error Handling

| Exception | Trigger | HTTP Status | Response Shape | Handling |
|-----------|---------|-------------|---------------|-----------|
| CategoryNotFoundException | Category ID not found in DB | 404 Not Found | `{ "error": "Category not found", "id": "..." }` | Global Handler |

## 17. Transaction Boundaries

- **Category Creation**: Local transaction ensures the category is saved.
- **Tree Generation**: Read-only operation; no transaction required beyond standard JPA defaults.

## 18. Validation

- **Request Validation**: `name` must not be blank.
- **Business Validation**: Category name must be unique. `parentCategoryId` must refer to an existing category if provided.

## 19. Testing Strategy

### Unit Tests
- `CategoryServiceImpl` tree building logic (Composite pattern verification).
- Validation of uniqueness rules.

### Controller Tests
- Request validation (400 errors).
- Correct HTTP status codes for 200, 404.

### Repository Tests
- H2 persistence for `Category` entity.
- Verification of unique constraint on `name`.

### Integration Tests
- Full flow from Controller to Repository.
- Verification of the `/tree` endpoint returning the correct nested structure.

### Failure Tests
- Attempting to create a category with a duplicate name.
- Attempting to fetch a non-existent category.

## 20. Observability

- **Logs**: Log category creation and tree retrieval requests.
- **Health**: Actuator `/health` endpoint.
- **Metrics**: Track category retrieval latency.

## 21. Implementation Sequence

1. Initialize Maven project with Spring Boot 3.2.5.
2. Add required dependencies (Web, JPA, Validation, H2, Lombok).
3. Implement `Category` entity and `CategoryRepository`.
4. Implement `CategoryRequest`, `CategoryResponse`, and `CategoryNode` DTOs.
5. Implement `CategoryService` interface and `CategoryServiceImpl` (including tree logic).
6. Implement `CategoryController` with REST endpoints.
7. Implement `CategoryNotFoundException` and global exception handler.
8. Configure `application.properties`.
9. Add Unit, Controller, and Integration tests.
10. Run `./mvnw clean test` to verify.
11. Update OpenAPI/Swagger documentation.

## 22. Acceptance Criteria

- [ ] `POST /categories` creates a category and enforces unique names.
- [ ] `GET /categories/{id}` returns category details.
- [ ] `GET /categories/tree` returns a nested hierarchical structure.
- [ ] Validation on `CategoryRequest` is enforced.
- [ ] `CategoryNotFoundException` returns 404.
- [ ] No direct access to other services' databases.
- [ ] Tests cover success and failure cases (especially the tree logic).
- [ ] `./mvnw clean test` passes.
- [ ] Dependency versions follow `CLAUDE.md`.

## 23. Out of Scope

- Implementing the Product catalog.
- Implementing a UI for the category tree.
- Implementing advanced search/filtering of categories.

## 24. Decisions and Open Questions

- **Decision**: The Composite pattern is used to return a tree structure in a single API call, reducing the number of requests a client needs to make to build a menu.
- **Decision**: The `parentCategoryId` is a simple UUID reference to avoid JPA recursive relationship complexities during simple CRUD operations.
- **Question**: Should we allow moving categories (updating `parentCategoryId`)? (Currently only creation is specified).

## 25. Recommended Git Branch

Suggested Branch: `feature/category-service`

Please switch to this branch before starting implementation.
