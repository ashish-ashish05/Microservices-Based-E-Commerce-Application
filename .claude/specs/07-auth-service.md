# 07. Auth Service Specification

## 1. Document Status

- Service: Auth Service
- Build Order: 07
- Assignment: Authentication and Authorization
- Status: Proposed
- Scope: Specification only

## 2. Purpose

The Auth Service is responsible for the authentication of users and the issuance of secure access tokens (JWT). It acts as the orchestrator for the login process, verifying user credentials and aggregating role information to provide a comprehensive identity token.

## 3. Responsibilities

- Authenticate users via username/password.
- Verify passwords using data retrieved from the User Service.
- Retrieve user roles and permissions from the Role Service.
- Generate signed JWT access tokens.
- Validate and inspect JWTs for internal authentication responsibilities.
- Provide login-related APIs.

## 4. Non-Responsibilities

- Must NOT maintain user profile data (owned by User Service).
- Must NOT maintain role definitions or permissions (owned by Role Service).
- Must NOT handle user registration.
- Must NOT perform JWT validation for every incoming request to the system (this is the API Gateway's responsibility).

## 5. Service Boundary

- **Data Ownership:** The Auth Service is largely stateless. If it persists any data, it is limited to authentication-specific metadata (e.g., token blacklists if implemented later).
- **Domain Ownership:** Authentication logic, JWT lifecycle, and login orchestration.
- **External Dependencies:** User Service (for credential verification), Role Service (for authorization metadata).
- **Boundary Justification:** Separating authentication from user management allows the authentication mechanism to evolve (e.g., adding OAuth2 or LDAP) without altering the user profile management logic.

## 6. Architecture

```text
Client
  |
API Gateway
  |
Auth Service
  |
  +---> User Service (Credential verification)
  |
  +---> Role Service (Role/Permission retrieval)
```

## 7. Service-to-Service Communication

| Consumer | Provider | Method | Endpoint | Purpose | Failure Consideration |
|----------|----------|--------|----------|---------|-----------------------|
| Auth | User | GET | `/users/{username}` | Fetch user details and password hash | 404 User Not Found $\rightarrow$ Authentication Failure |
| Auth | Role | GET | `/roles/{roleId}` | Fetch role details/permissions | 500 Internal Error $\rightarrow$ Fallback to basic USER role or fail login |

**Synchronous Communication Policy:**
- **Timeouts:** Strict read timeouts (e.g., 2s) to prevent login hangs.
- **Retries:** Not recommended for password verification to avoid account lockout triggers or unnecessary load.
- **Circuit Breaker:** Apply Resilience4j to User/Role service calls to fail fast if identity services are down.
- **Fallback:** If Role Service is unavailable, the system may deny login or provide a restricted "guest" scope depending on business policy.

## 8. API Specification

### Login

- **Method:** `POST`
- **Path:** `/auth/login`
- **Purpose:** Authenticate user and return a JWT.
- **Authentication:** None (Public)
- **Authorization:** None
- **Request Body:**
  ```json
  {
    "username": "ashish",
    "password": "securePassword123"
  }
  ```
- **Success Response:** `200 OK`
  ```json
  {
    "token": "eyJhbGci...<jwt>",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
  ```
- **Error Responses:**
  - `401 Unauthorized`: Invalid credentials.
  - `400 Bad Request`: Missing username or password.
  - `500 Internal Server Error`: Downstream service failure.

### Validate Token

- **Method:** `POST`
- **Path:** `/auth/validate`
- **Purpose:** Internally verify if a token is still valid and active.
- **Authentication:** Internal/Service-to-Service
- **Request Body:**
  ```json
  {
    "token": "eyJhbGci..."
  }
  ```
- **Success Response:** `200 OK`
  ```json
  {
    "valid": true,
    "username": "ashish",
    "roles": ["USER", "ADMIN"]
  }
  ```
- **Error Responses:**
  - `401 Unauthorized`: Invalid or expired token.

## 9. Data Model

The Auth Service is designed to be stateless. No primary business entities are owned. If a `TokenBlacklist` entity is introduced:

| Field | Type | Required | Constraints | Description |
|------|------|----------|-------------|-------------|
| tokenId | String | Yes | PK | Unique identifier of the revoked token |
| expiry | LocalDateTime | Yes | Index | Token expiration time |
| revokedAt | LocalDateTime | Yes | - | Time of revocation |

## 10. DTOs

### LoginRequest
- **Purpose:** Capture credentials from client.
- **Fields:** `username` (NotBlank), `password` (NotBlank).

### LoginResponse
- **Purpose:** Return the access token.
- **Fields:** `token`, `tokenType`, `expiresIn`.

### TokenValidationRequest
- **Purpose:** Request validation of a specific token.
- **Fields:** `token` (NotBlank).

### TokenValidationResponse
- **Purpose:** Return token status and identity.
- **Fields:** `valid` (boolean), `username`, `roles` (List).

## 11. Business Rules

- **Password Matching:** The password provided must match the BCrypt hash stored in the User Service.
- **Token Expiry:** JWTs must have a defined expiration time (e.g., 1 hour).
- **Role Inclusion:** The JWT must include the user's roles as claims to avoid repeated Role Service calls by other microservices.
- **Secret Security:** JWT signing keys must never be hard-coded.

## 12. Design Patterns

### Pattern: Facade Pattern

- **Where:** `AuthService` (Application layer).
- **Why:** The login process involves multiple steps: user lookup, password verification, role aggregation, and token generation.
- **Problem it solves:** Simplifies the client interface by hiding the complexity of coordinating three different components (User Client, Role Client, JwtProvider).

### Pattern: Strategy Pattern

- **Where:** `AuthenticationStrategy`.
- **Why:** To support different ways of authenticating (e.g., Password, API Key, OAuth2) in the future.
- **Problem it solves:** Decouples the login controller from the specific logic of credential verification.

## 13. Dependencies

### Required
- `spring-boot-starter-web`: REST APIs.
- `spring-boot-starter-security`: Security framework.
- `spring-cloud-starter-openfeign`: Communication with User/Role services.
- `io.jsonwebtoken (jjwt)`: JWT creation and parsing.
- `spring-boot-starter-validation`: Request validation.
- `spring-boot-starter-actuator`: Health checks.
- `com.h2database:h2`: For potential token blacklist storage.
- `org.projectlombok:lombok`: Boilerplate reduction.

### Optional / Later
- `spring-cloud-starter-circuitbreaker-resilience4j`: For remote call resilience.

## 14. Configuration

```properties
server.port=8085
spring.application.name=auth-service
# JWT Configuration
jwt.secret=env_variable_secret
jwt.expiration=3600000
# Downstream Services
user-service.url=http://localhost:8084
role-service.url=http://localhost:8082
```

## 15. Security

- **JWT Secret:** Must be loaded from an environment variable or secure vault.
- **Password Handling:** Auth Service receives plain text passwords over HTTPS; it must never log these passwords.
- **Token Claims:** Only non-sensitive data (username, roles) should be placed in the JWT.
- **Internal APIs:** `/auth/validate` should be restricted to internal network traffic.

## 16. Exception and Error Handling

| Exception | Trigger | HTTP Status | Response Shape |
|------------|---------|-------------|----------------|
| `InvalidCredentialsException` | Password mismatch | 401 | `{ "error": "Invalid username or password" }` |
| `UserNotFoundException` | User doesn't exist | 401 | `{ "error": "Invalid username or password" }` (Generic for security) |
| `TokenExpiredException` | JWT date > now | 401 | `{ "error": "Token has expired" }` |
| `ServiceUnavailableException` | Downstream failure | 503 | `{ "error": "Identity service currently unavailable" }` |

## 17. Transaction Boundaries

The Auth Service is primarily read-only (looking up users/roles). Local transactions are only required if implementing a `TokenBlacklist` for logout functionality.

## 18. Validation

- **LoginRequest:** `username` and `password` cannot be null or blank.
- **TokenValidationRequest:** `token` cannot be empty.
- **JWT Structure:** Must verify signature, issuer, and expiration date.

## 19. Testing Strategy

### Unit Tests
- `JwtProvider`: Test token generation, claims extraction, and expiration logic.
- `AuthServiceImpl`: Mock clients to test login orchestration and password verification.

### Controller Tests
- `AuthController`: Test `/login` and `/validate` endpoints, ensuring 400/401/200 status codes.

### Integration Tests
- Full flow from `AuthController` $\rightarrow$ `AuthService` $\rightarrow$ Mocked Feign Clients.

### Contract / Remote Tests
- Verify Feign clients correctly map User and Role service responses.

### Failure Tests
- Simulate `User Service` timeout $\rightarrow$ Verify 503 response.
- Provide expired token $\rightarrow$ Verify 401 response.

## 20. Observability

- **Logs:** Log login attempts (username only), token issuance, and downstream service errors.
- **Metrics:** Track `login_success_rate` and `login_latency`.
- **Health:** Actuator `/health` check for readiness.

## 21. Implementation Sequence

1. Update Maven project with `jjwt` and `spring-security` dependencies.
2. Implement `JwtProvider` utility for signing and parsing tokens.
3. Create OpenFeign clients for `UserServiceClient` and `RoleServiceClient`.
4. Create Request/Response DTOs.
5. Implement `AuthService` interface and `AuthServiceImpl` to orchestrate login.
6. Implement `AuthController` with `/login` and `/validate` endpoints.
7. Implement `GlobalExceptionHandler` for auth-specific errors.
8. Configure `application.properties` with secret keys and service URLs.
9. Add unit and controller tests.
10. Run `./mvnw clean test` for verification.

## 22. Acceptance Criteria

- [ ] `/auth/login` returns a valid JWT upon correct credentials.
- [ ] `/auth/login` returns 401 for incorrect passwords or non-existent users.
- [ ] JWT contains `username` and `roles` claims.
- [ ] `/auth/validate` correctly identifies valid vs expired/tampered tokens.
- [ ] OpenFeign is used for all remote calls.
- [ ] No passwords or secrets are logged.
- [ ] No direct database access to User or Role services.
- [ ] All tests pass.
- [ ] No Kafka/RabbitMQ introduced.

## 23. Out of Scope

- Implementing a full OAuth2 server (sticking to simple JWT).
- User registration (handled by User Service).
- Password reset flows.

## 24. Decisions and Open Questions

- **Decision:** Use `jjwt` for learning-oriented implementation as per `CLAUDE.md`.
- **Decision:** Synchronous calls to User/Role services; failure results in denied login.
- **Assumption:** API Gateway will handle the heavy lifting of JWT validation for every request; this service only handles the *issuance* and *internal* validation.

## 25. Recommended Git Branch

Suggested Branch: `feature/auth-service`
Please switch to this branch before starting implementation.
