# 05. Role Service Specification

## 1. Document Status

- Service: Role Service
- Build Order: 05
- Assignment: Roles and Authorization
- Status: Proposed
- Scope: Specification only

## 2. Purpose

The Role Service is responsible for managing the roles and permissions within the e-commerce platform. It provides a centralized way to define what different types of users (e.g., ADMIN, USER) are allowed to do, ensuring a consistent authorization model across all microservices.

## 3. Responsibilities

- Manage the lifecycle of roles (Create, Read, Update, Delete).
- Maintain role metadata (e.g., role name, description).
- Provide role information to the User Service (for assigning roles to users) and Auth Service (for including roles in JWT tokens).
- Manage permissions associated with roles as the authorization model evolves.

## 4. Non-Responsibilities

- **User Management:** Does not manage users or user profiles (handled by User Service).
- **Authentication:** Does not verify passwords or generate JWT tokens (handled by Auth Service).
- **Session Management:** Does not track active user sessions.
- **Direct Authorization Enforcement:** While it defines roles, the actual enforcement of "can user X access resource Y" typically happens at the Gateway or within the specific microservice using the roles provided in the JWT.

## 5. Service Boundary

- **Data Ownership:** The Role Service owns the `Role` entity and any associated `Permission` data.
- **Domain Ownership:** Owns the concept of "Role" and "Permission".
- **External Dependencies:** Primarily serves as a provider to the User Service and Auth Service.
- **Boundary Justification:** Separating roles into their own service allows for independent evolution of the authorization model without impacting user profile data or authentication logic.

## 6. Architecture

Client
  |
API Gateway
  |
Auth Service  <--->  Role Service
  |                      ^
User Service  <----------|

## 7. Service-to-Service Communication

| Consumer | Provider | Method | Endpoint | Purpose | Failure Consideration |
|----------|----------|--------|----------|---------|-----------------------|
| Auth Service | Role Service | GET | `/api/roles/{roleId}` | Fetch role details for JWT claims | Circuit breaker + Fallback to default role |
| User Service | Role Service | GET | `/api/roles` | List available roles for user assignment | Circuit breaker + Fallback to empty list |

### Communication Details:
- **Timeouts:** Strict timeouts (e.g., 2s) to prevent cascading failures.
- **Retry:** Retry only on idempotent GET requests.
- **Circuit Breaker:** Resilience4j used to open circuit if Role Service is down.
- **Fallback:** If Role Service is unavailable, services may fallback to a basic `USER` role or return an error indicating authorization metadata is temporarily unavailable.

## 8. API Specification

### List All Roles
- Method: `GET`
- Path: `/api/roles`
- Purpose: Retrieve a list of all defined roles in the system.
- Authentication: ADMIN (typically)
- Authorization: `ROLE_ADMIN`
- Success response: `200 OK` with `List<RoleResponseDTO>`
- Error responses: `401 Unauthorized`, `403 Forbidden`

### Get Role by ID
- Method: `GET`
- Path: `/api/roles/{roleId}`
- Purpose: Retrieve detailed information about a specific role.
- Authentication: System/Service-to-Service
- Authorization: Internal or `ROLE_ADMIN`
- Path parameters: `roleId` (UUID)
- Success response: `200 OK` with `RoleResponseDTO`
- Error responses: `404 Not Found`, `401 Unauthorized`

### Create Role
- Method: `POST`
- Path: `/api/roles`
- Purpose: Define a new role in the system.
- Authentication: ADMIN
- Authorization: `ROLE_ADMIN`
- Request body: `RoleRequestDTO` (name, description)
- Success response: `201 Created` with `RoleResponseDTO`
- Error responses: `400 Bad Request` (validation failure), `409 Conflict` (role name exists)

### Update Role
- Method: `PUT`
- Path: `/api/roles/{roleId}`
- Purpose: Update the metadata of an existing role.
- Authentication: ADMIN
- Authorization: `ROLE_ADMIN`
- Path parameters: `roleId` (UUID)
- Request body: `RoleRequestDTO`
- Success response: `200 OK` with `RoleResponseDTO`
- Error responses: `404 Not Found`, `400 Bad Request`

### Delete Role
- Method: `DELETE`
- Path: `/api/roles/{roleId}`
- Purpose: Remove a role from the system.
- Authentication: ADMIN
- Authorization: `ROLE_ADMIN`
- Path parameters: `roleId` (UUID)
- Success response: `204 No Content`
- Error responses: `404 Not Found`, `409 Conflict` (if users are assigned to this role)

## 9. Data Model

### Entity: Role
| Field | Type | Required | Constraints | Description |
|------|------|----------|-------------|-------------|
| id | UUID | Yes | Primary Key | Unique identifier for the role |
| name | String | Yes | Unique, Not Null | The role name (e.g., "ROLE_ADMIN", "ROLE_USER") |
| description | String | No | - | Human-readable description of the role's purpose |
| createdAt | LocalDateTime | Yes | Not Null | Timestamp of creation |
| updatedAt | LocalDateTime | Yes | Not Null | Timestamp of last update |

## 10. DTOs

### RoleRequestDTO
- **Purpose:** Used for creating and updating roles.
- **Fields:** `name` (Required, non-empty), `description` (Optional).
- **Validation:** `name` must follow a specific pattern (e.g., starts with `ROLE_`).

### RoleResponseDTO
- **Purpose:** Used for returning role information to clients/services.
- **Fields:** `id`, `name`, `description`.
- **Validation:** None.

## 11. Business Rules

- Role names must be unique.
- Role names should follow the Spring Security convention (e.g., starting with `ROLE_`).
- A role cannot be deleted if it is currently assigned to any user (checked via User Service if necessary, or handled via soft-delete/constraint).
- Only users with `ROLE_ADMIN` can manage roles.

## 12. Design Patterns

### Pattern: Repository Pattern
- **Where:** `RoleRepository`
- **Why:** Standard Spring Data JPA abstraction.
- **Problem it solves:** Decouples business logic from the underlying persistence mechanism (H2/PostgreSQL).

### Pattern: DTO Pattern
- **Where:** `RoleRequestDTO`, `RoleResponseDTO`
- **Why:** Prevents exposing internal entity structures and audit fields to the API.
- **Problem it solves:** Ensures a stable API contract and avoids accidental data leakage.

### Pattern: Strategy Pattern (Future)
- **Where:** Permission evaluation logic.
- **Why:** If the authorization model evolves from simple roles to complex permission-based access control (RBAC/ABAC).
- **Problem it solves:** Allows different strategies for evaluating whether a role has a specific permission.

## 13. Dependencies

### Required
- `spring-boot-starter-web`: For REST APIs.
- `spring-boot-starter-data-jpa`: For database access.
- `spring-boot-starter-validation`: For input validation.
- `com.h2database:h2`: Development database.
- `org.projectlombok:lombok`: Boilerplate reduction.
- `spring-boot-starter-actuator`: Health and monitoring.
- `spring-boot-starter-test`: Unit and integration tests.

### Optional / Later
- `spring-cloud-starter-circuitbreaker-resilience4j`: If Role Service starts depending on other services.

## 14. Configuration

```properties
server.port=8085
spring.application.name=role-service
spring.datasource.url=jdbc:h2:mem:role-db
spring.jpa.hibernate.ddl-auto=update
```
- `server.port`: Port the service listens on.
- `spring.application.name`: Name used for service discovery and logging.
- `spring.datasource.url`: Connection string for the H2 in-memory database.
- `spring.jpa.hibernate.ddl-auto`: Ensures the schema is updated automatically on startup.

## 15. Security

- **Authentication:** Requests to management APIs must be authenticated.
- **Authorization:** `ROLE_ADMIN` is required for all POST, PUT, and DELETE operations.
- **Service-to-Service:** The Auth Service and User Service may call the Role Service via internal authenticated channels (e.g., shared secret or JWT).
- **Sensitive Data:** No sensitive secrets are stored in the Role Service.

## 16. Exception and Error Handling

| Exception | Trigger | HTTP Status | Response Shape |
|------------|---------|-------------|----------------|
| `RoleNotFoundException` | Requested Role ID doesn't exist | `404 Not Found` | `{ "error": "Role not found", "id": "..." }` |
| `RoleAlreadyExistsException` | Creating a role with a duplicate name | `409 Conflict` | `{ "error": "Role name already exists", "name": "..." }` |
| `InvalidRoleRequestException` | Validation failure in `RoleRequestDTO` | `400 Bad Request` | Standard Spring Validation response |
| `AccessDeniedException` | User lacks `ROLE_ADMIN` | `403 Forbidden` | `{ "error": "Access denied" }` |

## 17. Transaction Boundaries

- All Create/Update/Delete operations in `RoleService` should be wrapped in `@Transactional` to ensure atomicity.
- Since the Role Service only manages its own database, there are no distributed transaction concerns here.

## 18. Validation

- `RoleRequestDTO.name`: Not null, not blank, length between 3 and 50 characters.
- `RoleRequestDTO.description`: Max 255 characters.
- State transition: A role cannot be renamed to a name that already exists.

## 19. Testing Strategy

### Unit Tests
- `RoleService` business logic (e.g., uniqueness checks).
- DTO validation logic.

### Controller Tests
- Use `@WebMvcTest` to verify endpoint mapping, request validation, and status codes.

### Repository Tests
- Use `@DataJpaTest` to verify custom queries and constraints in the H2 database.

### Integration Tests
- Use `@SpringBootTest` to verify the end-to-end flow from Controller to Database.

### Failure Tests
- Test behavior when the database is unavailable (though unlikely with H2 in-mem).
- Test `403 Forbidden` for non-admin users.
- Test `409 Conflict` for duplicate role names.

## 20. Observability

- **Logs:** Log role creation, updates, and deletions with the role name and ID.
- **Health:** Actuator `/health` endpoint to monitor service availability.
- **Metrics:** Track the number of roles created and the frequency of role lookups.

## 21. Implementation Sequence

1. Create Maven project using existing conventions.
2. Add required dependencies (`web`, `jpa`, `validation`, `h2`, `lombok`, `actuator`).
3. Create the `Role` entity.
4. Create the `RoleRepository` interface.
5. Create `RoleRequestDTO` and `RoleResponseDTO`.
6. Implement the `RoleService` business logic.
7. Implement the `RoleController` with endpoints for CRUD.
8. Implement global exception handling for `RoleNotFoundException` and `RoleAlreadyExistsException`.
9. Configure `application.properties`.
10. Implement Unit, Repository, and Controller tests.
11. Implement Integration tests for a full role lifecycle.
12. Run `./mvnw clean test` to verify.

## 22. Acceptance Criteria

- [ ] `GET /api/roles` returns a list of roles.
- [ ] `GET /api/roles/{id}` returns a specific role or 404.
- [ ] `POST /api/roles` creates a role and prevents duplicate names.
- [ ] `PUT /api/roles/{id}` updates role metadata.
- [ ] `DELETE /api/roles/{id}` removes a role.
- [ ] Request validation for role names is enforced.
- [ ] Only users with `ROLE_ADMIN` can modify roles.
- [ ] All tests pass with `./mvnw clean test`.
- [ ] No Kafka/RabbitMQ introduced.
- [ ] Dependency versions match `CLAUDE.md`.

## 23. Out of Scope

- Implementing a complex Permission-based Access Control (PBAC) system in this initial phase.
- Generating JWT tokens.
- Managing user-to-role assignments (this is the User Service's responsibility).

## 24. Decisions and Open Questions

- **Decision:** Using a simple Role-based model initially.
- **Decision:** Using UUIDs for Role IDs to ensure uniqueness across potential future migrations.
- **Question:** Should we implement soft-deletes for roles to avoid breaking foreign key references in the User Service? (Current decision: Hard delete, but User Service should handle cleanup or the Role Service should check for usage).

## 25. Recommended Git Branch

Suggested Branch: `feature/role-service`

Please switch to this branch before starting implementation.
