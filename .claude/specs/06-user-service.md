# 06. User Service Specification

## 1. Document Status

- Service: User Service
- Build Order: 06
- Assignment: User Management and Profiles
- Status: Proposed
- Scope: Specification only

## 2. Purpose

The User Service is responsible for managing user registration, profiles, and the association between users and their security roles. It serves as the primary source of truth for user identity data within the e-commerce platform, providing necessary lookup capabilities for the Auth Service to perform authentication.

## 3. Responsibilities

- Register new users (account creation).
- Maintain user profile data (name, email, etc.).
- Store secure password hashes using BCrypt.
- Manage the association between a User and a Role (reference to `roleId`).
- Provide user lookup APIs (by email/username) for the Auth Service.
- Manage user lifecycle (e.g., account updates, deletion).

## 4. Non-Responsibilities

- **JWT Generation:** Does NOT generate JWT tokens (handled by Auth Service).
- **JWT Validation:** Does NOT validate JWTs as its primary responsibility (handled by Gateway/Auth Service).
- **Authentication Flow:** Does NOT handle the actual login handshake or session management.
- **Role Metadata Management:** Does NOT define what a "Role" is or what permissions it has (handled by Role Service).

## 5. Service Boundary

- **Data Ownership:** Owns the `User` entity and the mapping to `roleId`.
- **Domain Ownership:** Owns the "User Profile" and "User Credentials" domains.
- **External Dependencies:** Depends on the Role Service to validate that a role exists before assigning it to a user.
- **Boundary Justification:** Separating user data from authentication logic (Auth Service) and role definitions (Role Service) ensures that profile management can evolve independently of security token logic and authorization models.

## 6. Architecture

Client
  |
API Gateway
  |
Auth Service  <--->  User Service  <--->  Role Service
  |
  v
(Other Services)

## 7. Service-to-Service Communication

| Consumer | Provider | Method | Endpoint | Purpose | Failure Consideration |
|----------|----------|--------|----------|---------|-----------------------|
| Auth Service | User Service | GET | `/api/users/lookup` | Find user by email for login | Circuit breaker + Fallback to 401 Unauthorized |
| User Service | Role Service | GET | `/api/roles/{roleId}` | Validate role exists during registration/update | Circuit breaker + Fallback to 400 Bad Request |

### Communication Details:
- **Timeouts:** Strict timeouts (e.g., 2s) for all remote calls.
- **Retry:** Retry only on idempotent GET requests.
- **Circuit Breaker:** Resilience4j used for calls to Role Service.
- **Fallback:** If Role Service is unavailable during user creation, the request fails with a "Role validation service unavailable" error.

## 8. API Specification

### Register User
- Method: `POST`
- Path: `/api/users`
- Purpose: Create a new user account.
- Authentication: Public
- Authorization: Public
- Request body: `UserRegistrationRequestDTO` (email, password, name, roleId)
- Success response: `201 Created` with `UserResponseDTO`
- Error responses: `400 Bad Request` (validation), `409 Conflict` (email exists), `404 Not Found` (roleId invalid)

### Get User Profile
- Method: `GET`
- Path: `/api/users/{userId}`
- Purpose: Retrieve profile information for a specific user.
- Authentication: Authenticated
- Authorization: User (self) or ADMIN
- Path parameters: `userId` (UUID)
- Success response: `200 OK` with `UserResponseDTO`
- Error responses: `404 Not Found`, `403 Forbidden`

### Update User Profile
- Method: `PUT`
- Path: `/api/users/{userId}`
- Purpose: Update profile details.
- Authentication: Authenticated
- Authorization: User (self) or ADMIN
- Path parameters: `userId` (UUID)
- Request body: `UserUpdateRequestDTO`
- Success response: `200 OK` with `UserResponseDTO`
- Error responses: `404 Not Found`, `400 Bad Request`

### User Lookup (Internal)
- Method: `GET`
- Path: `/api/users/lookup`
- Purpose: Retrieve user credentials and role for authentication.
- Authentication: System/Service-to-Service
- Authorization: Internal (Auth Service only)
- Query parameters: `email` (Required)
- Success response: `200 OK` with `UserAuthDTO` (id, passwordHash, roleId)
- Error responses: `404 Not Found`

## 9. Data Model

### Entity: User
| Field | Type | Required | Constraints | Description |
|------|------|----------|-------------|-------------|
| id | UUID | Yes | Primary Key | Unique identifier for the user |
| email | String | Yes | Unique, Not Null | User email used for login |
| passwordHash | String | Yes | Not Null | BCrypt hashed password |
| fullName | String | Yes | Not Null | User's full name |
| roleId | UUID | Yes | Not Null | Reference to Role Service's Role ID |
| createdAt | LocalDateTime | Yes | Not Null | Timestamp of creation |
| updatedAt | LocalDateTime | Yes | Not Null | Timestamp of last update |

## 10. DTOs

### UserRegistrationRequestDTO
- **Purpose:** Input for creating a new account.
- **Fields:** `email`, `password`, `fullName`, `roleId`.
- **Validation:** `email` must be valid format; `password` must meet complexity requirements; `fullName` not blank.

### UserUpdateRequestDTO
- **Purpose:** Input for updating profile.
- **Fields:** `fullName`, `email` (optional).
- **Validation:** `email` must be valid if provided.

### UserResponseDTO
- **Purpose:** Public user profile representation.
- **Fields:** `id`, `email`, `fullName`, `roleId`.
- **Critical:** MUST NOT include `passwordHash`.

### UserAuthDTO
- **Purpose:** Internal data transfer to Auth Service.
- **Fields:** `id`, `passwordHash`, `roleId`.
- **Validation:** Internal use only.

## 11. Business Rules

- Email must be unique across all users.
- Passwords must be hashed using `BCryptPasswordEncoder` before storage.
- Password hashes must NEVER be returned in any public API response.
- A user must be assigned a valid `roleId` (verified via Role Service).
- Only the account owner or an ADMIN can update a profile.

## 12. Design Patterns

### Pattern: Repository Pattern
- **Where:** `UserRepository`
- **Why:** Standard Spring Data JPA abstraction.
- **Problem it solves:** Decouples business logic from persistence.

### Pattern: DTO Pattern
- **Where:** `UserRegistrationRequestDTO`, `UserResponseDTO`, etc.
- **Why:** Protects internal entity structure and prevents password hash leakage.
- **Problem it solves:** Decouples the API contract from the database schema.

### Pattern: Service Layer Pattern
- **Where:** `UserService`
- **Why:** Centralizes business logic (hashing, validation, remote role checks).
- **Problem it solves:** Ensures consistent application of business rules regardless of the entry point.

## 13. Dependencies

### Required
- `spring-boot-starter-web`: For REST APIs.
- `spring-boot-starter-data-jpa`: For database access.
- `spring-boot-starter-security`: For `BCryptPasswordEncoder`.
- `spring-boot-starter-validation`: For input validation.
- `spring-cloud-starter-openfeign`: For communicating with Role Service.
- `com.h2database:h2`: Development database.
- `org.projectlombok:lombok`: Boilerplate reduction.
- `spring-boot-starter-actuator`: Health and monitoring.
- `spring-boot-starter-test`: Unit and integration tests.
- `spring-security-test`: Testing security constraints.

### Optional / Later
- `spring-cloud-starter-circuitbreaker-resilience4j`: For the OpenFeign client calling Role Service.

## 14. Configuration

```properties
server.port=8084
spring.application.name=user-service
spring.datasource.url=jdbc:h2:mem:user-db
spring.jpa.hibernate.ddl-auto=update
# Role service URL for Feign client
services.role-service.url=http://localhost:8085
```
- `server.port`: Port the service listens on.
- `spring.application.name`: Name for the service.
- `spring.datasource.url`: Connection string for the H2 in-memory database.
- `services.role-service.url`: Base URL for the Role Service.

## 15. Security

- **Authentication:** Profile and Update APIs require a valid JWT.
- **Authorization:** 
    - `GET /api/users/{userId}`: Allowed if `userId` matches JWT subject or user has `ROLE_ADMIN`.
    - `PUT /api/users/{userId}`: Allowed if `userId` matches JWT subject or user has `ROLE_ADMIN`.
- **Password Handling:** Use BCrypt. Never log password hashes.
- **Internal APIs:** `/api/users/lookup` should be restricted to internal network/Auth Service.

## 16. Exception and Error Handling

| Exception | Trigger | HTTP Status | Response Shape |
|------------|---------|-------------|----------------|
| `UserNotFoundException` | Requested User ID doesn't exist | `404 Not Found` | `{ "error": "User not found", "id": "..." }` |
| `EmailAlreadyExistsException` | Registering with an existing email | `409 Conflict` | `{ "error": "Email already in use", "email": "..." }` |
| `InvalidRoleException` | Assigned `roleId` does not exist in Role Service | `404 Not Found` | `{ "error": "Invalid role assigned", "roleId": "..." }` |
| `AccessDeniedException` | User attempting to modify another user's profile | `403 Forbidden` | `{ "error": "Access denied" }` |

## 17. Transaction Boundaries

- Registration: `BCrypt` hashing -> Role validation (remote) -> Save User. The local save must be transactional.
- Profile Update: Local transaction.
- Note: The remote call to Role Service occurs outside the local DB transaction to avoid holding connections open during network I/O.

## 18. Validation

- `UserRegistrationRequestDTO.email`: Valid email format, not null.
- `UserRegistrationRequestDTO.password`: Not null, minimum 8 characters.
- `UserRegistrationRequestDTO.fullName`: Not blank.
- `UserRegistrationRequestDTO.roleId`: Not null, must be a valid UUID.

## 19. Testing Strategy

### Unit Tests
- Password hashing logic.
- `UserService` business rules (email uniqueness, role validation logic).
- DTO validation.

### Controller Tests
- `@WebMvcTest` for registration, profile retrieval, and updates.
- Verify status codes and response DTOs.

### Repository Tests
- `@DataJpaTest` for email uniqueness constraints and user lookups.

### Integration Tests
- `@SpringBootTest` for the full registration flow including mocked Role Service calls.

### Contract / Remote Tests
- Verify `RoleServiceClient` (Feign) correctly maps responses and handles 404s.

### Failure Tests
- Role Service unavailable during registration.
- Attempting to register with a duplicate email.
- Unauthorized access to another user's profile.

## 20. Observability

- **Logs:** Log registration events (email), profile updates, and failed login lookups.
- **Health:** Actuator `/health` endpoint.
- **Metrics:** Track registration rate and user lookup latency.

## 21. Implementation Sequence

1. Create Maven project and add required dependencies.
2. Create the `User` entity.
3. Create the `UserRepository` interface.
4. Create `UserRegistrationRequestDTO`, `UserUpdateRequestDTO`, `UserResponseDTO`, and `UserAuthDTO`.
5. Implement the `RoleServiceClient` (OpenFeign) to call Role Service.
6. Implement the `UserService` (hashing, remote role check, persistence).
7. Implement the `UserController` with registration and profile endpoints.
8. Implement global exception handling.
9. Configure `application.properties`.
10. Implement Unit, Repository, and Controller tests.
11. Implement Integration tests.
12. Run `./mvnw clean test` to verify.

## 22. Acceptance Criteria

- [ ] `POST /api/users` creates a user with a hashed password and validates the role.
- [ ] `GET /api/users/{id}` returns a profile without the password hash.
- [ ] `PUT /api/users/{id}` updates profile data.
- [ ] `GET /api/users/lookup` returns credentials for the Auth Service.
- [ ] Duplicate emails are rejected with `409 Conflict`.
- [ ] Invalid role IDs result in registration failure.
- [ ] Password hashes are never exposed in APIs.
- [ ] All tests pass with `./mvnw clean test`.
- [ ] No Kafka/RabbitMQ introduced.
- [ ] Dependency versions match `CLAUDE.md`.

## 23. Out of Scope

- Implementing actual JWT validation inside the User Service.
- Managing role definitions or permissions.
- Sending registration confirmation emails (this would be Notification Service).

## 24. Decisions and Open Questions

- **Decision:** User Service depends on Role Service for validation during account creation to ensure data integrity.
- **Decision:** Using BCrypt for passwords as per `CLAUDE.md`.
- **Decision:** Internal lookup API provided for Auth Service to keep credentials isolated.
- **Question:** Should we implement account locking/activation logic now or in a later phase? (Current decision: Simple registration).

## 25. Recommended Git Branch

Suggested Branch: `feature/user-service`

Please switch to this branch before starting implementation.
