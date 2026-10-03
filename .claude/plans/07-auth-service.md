# 07. Auth Service Implementation Plan

## 1. Plan Metadata

- Service: Auth Service
- Build Order: 07
- Assignment: Authentication and Authorization
- Specification: .claude/specs/07-auth-service.md
- Status: Planned
- Plan Generated: 2026-10-03
- Prerequisite Services: User Service, Role Service

## 2. Objective

The Auth Service will be the central identity provider for the platform, handling user authentication, credential verification through the User Service, role aggregation through the Role Service, and the issuance of secure JWT access tokens.

## 3. Existing Context

The Auth Service orchestrates the login flow. It calls the User Service (Port 8084) to verify the user exists and get their password hash, and the Role Service (Port 8085) to retrieve role metadata for the JWT claims.

## 4. Scope

### In Scope
- User authentication via username/password.
- Integration with User Service via OpenFeign to fetch credentials.
- Integration with Role Service via OpenFeign to fetch roles/permissions.
- Implementation of JWT generation and parsing using `jjwt`.
- Implementation of a `Facade` for the login workflow.
- Implementation of a `Strategy` pattern for authentication methods.
- Internal token validation API.
- Unit, Controller, and Integration tests.

### Out of Scope
- User registration (handled by User Service).
- Role management (handled by Role Service).
- JWT validation for every system request (handled by API Gateway).
- OAuth2/LDAP integration (deferred).

## 5. Architecture

Client
  |
API Gateway
  |
Auth Service
  |
  +---> User Service (Credential verification)
  |
  +---> Role Service (Role/Permission retrieval)

The Auth Service is stateless. It coordinates between the User and Role services to produce a signed JWT.

## 6. Implementation Dependencies

- **Maven Dependencies**:
    - `spring-boot-starter-web`
    - `spring-boot-starter-security`
    - `spring-cloud-starter-openfeign`
    - `io.jsonwebtoken:jjwt-api`
    - `io.jsonwebtoken:jjwt-impl` (runtime)
    - `io.jsonwebtoken:jjwt-jackson` (runtime)
    - `spring-boot-starter-validation`
    - `spring-boot-starter-actuator`
    - `com.h2database:h2` (for potential token blacklist)
    - `org.projectlombok:lombok`
- **Prerequisite Services**:
    - User Service (Running on port 8084)
    - Role Service (Running on port 8085)
- **Configuration**:
    - `server.port=8085` (Note: Role service is also on 8085 in some specs, check port mapping)
    - `jwt.secret=env_variable_secret`
    - `jwt.expiration=3600000` (1 hour)
    - `user-service.url=http://localhost:8084`
    - `role-service.url=http://localhost:8082` (Check port consistency)

## 7. Data Model Plan

The Auth Service is designed to be stateless. No primary business entities are owned.

## 8. API Implementation Plan

### Login
- HTTP method: `POST`
- path: `/auth/login`
- request DTO: `LoginRequest` (username, password)
- validation: `username` (@NotBlank), `password` (@NotBlank)
- response DTO: `LoginResponse` (token, tokenType, expiresIn)
- status codes: `200 OK`, `401 Unauthorized`, `400 Bad Request`
- authentication/authorization: Public
- downstream calls: `GET /api/users/lookup` (User Service), `GET /api/roles/{roleId}` (Role Service)
- error behavior: Return 401 for invalid credentials or non-existent users.

### Validate Token
- HTTP method: `POST`
- path: `/auth/validate`
- request DTO: `TokenValidationRequest` (token)
- validation: `token` (@NotBlank)
- response DTO: `TokenValidationResponse` (valid, username, roles)
- status codes: `200 OK`, `401 Unauthorized`
- authentication/authorization: Internal
- downstream calls: None
- error behavior: Return 401 for expired or tampered tokens.

## 9. Service Layer Plan

1. **Login Workflow (Facade)**:
    - Receive `LoginRequest`.
    - Call `UserServiceClient` to find user by username/email and retrieve `passwordHash` and `roleId`.
    - Verify the provided password against the `passwordHash` using `BCrypt`.
    - Call `RoleServiceClient` to fetch role details for the user's `roleId`.
    - Generate a JWT containing the username and roles as claims using `JwtProvider`.
    - Return `LoginResponse`.
2. **Token Validation**:
    - Parse the JWT using `JwtProvider`.
    - Verify the signature and expiration date.
    - Return `TokenValidationResponse`.

## 10. Design Pattern Implementation Plan

### Facade Pattern
- Where: `AuthServiceImpl`
- Why: Orchestrates the complex login process involving User Client, Role Client, and JwtProvider.
- Main participants: `AuthServiceImpl`, `UserServiceClient`, `RoleServiceClient`, `JwtProvider`.

### Strategy Pattern
- Where: `AuthenticationStrategy`
- Why: Decouples the login controller from the specific credential verification logic.
- Main participants: `AuthenticationStrategy` (Interface), `PasswordAuthenticationStrategy` (Implementation).

## 11. Remote Communication Plan

| Caller | Provider | Endpoint | Purpose | Failure Handling |
|--------|----------|----------|---------|------------------|
| Auth | User | `/api/users/lookup` | Fetch user credentials | 404 $\rightarrow$ Authentication Failure |
| Auth | Role | `/api/roles/{roleId}` | Fetch role details | 500 $\rightarrow$ Fallback to default role or fail login |

- **Timeout**: 2-second read timeout.
- **Retry**: No retries for password verification.
- **Circuit Breaker**: To be implemented using Resilience4j.

## 12. Exception Handling Plan

- **InvalidCredentialsException**: Thrown when password mismatch occurs. Maps to 401 Unauthorized.
- **UserNotFoundException**: Thrown when user does not exist. Maps to 401 Unauthorized (for security).
- **TokenExpiredException**: Thrown when JWT is expired. Maps to 401 Unauthorized.
- **Global Exception Handler**: Uses `@RestControllerAdvice` to map exceptions to standardized responses.

## 13. Security Plan

- **JWT Secret**: Must be loaded from an environment variable or secure vault.
- **Password Handling**: Never log plain text passwords or password hashes.
- **Token Claims**: Include only non-sensitive data (username, roles).
- **Internal APIs**: `/auth/validate` restricted to internal traffic.

## 14. Testing Plan

### Unit Tests
- `JwtProvider`: Test token generation, claims extraction, and expiration.
- `AuthServiceImpl`: Mock clients to test the login orchestrator.
- `AuthenticationStrategy`: Test password verification logic.

### Controller Tests
- `AuthController` endpoints mapping and response status codes.

### Integration Tests
- Full flow from `AuthController` $\rightarrow$ `AuthService` $\rightarrow$ Mocked Feign Clients.

### Contract / Remote Tests
- Verify Feign clients correctly map responses from User and Role services.

## 15. Files to Create

```text
src/main/java/com/ecommerce/auth_service/
├── AuthServiceApplication.java
├── controller/AuthController.java
├── service/AuthService.java
├── service/AuthServiceImpl.java
├── provider/JwtProvider.java
├── client/UserServiceClient.java
├── client/RoleServiceClient.java
├── dto/LoginRequest.java
├── dto/LoginResponse.java
├── dto/TokenValidationRequest.java
├── dto/TokenValidationResponse.java
├── strategy/AuthenticationStrategy.java
├── strategy/PasswordAuthenticationStrategy.java
└── exception/InvalidCredentialsException.java
└── exception/TokenExpiredException.java
└── exception/GlobalExceptionHandler.java
```

## 16. Files to Modify

None.

## 17. Implementation Sequence

1. Initialize Maven project with `jjwt` and `spring-security` dependencies.
2. Implement `JwtProvider` utility for signing and parsing tokens.
3. Create OpenFeign clients for `UserServiceClient` and `RoleServiceClient`.
4. Create Request/Response DTOs.
5. Implement `AuthenticationStrategy` and `PasswordAuthenticationStrategy`.
6. Implement `AuthService` (Facade) to orchestrate the login process.
7. Implement `AuthController` with `/login` and `/validate` endpoints.
8. Implement `GlobalExceptionHandler` for auth-specific errors.
9. Configure `application.properties` with secret keys and service URLs.
10. Add unit and controller tests.
11. Run `./mvnw clean test` to verify.

## 18. Verification Plan

- Run `./mvnw clean test` to verify all tests pass.
- Run `./mvnw help:effective-pom` to check dependency versions.

## 19. Risks and Failure Scenarios

- **Downstream Service Unavailable**: User or Role services might be down. Result in 503 error.
- **Invalid JWT Secret**: Misconfigured secret leads to token validation failure.
- **Password Leak**: Accidental logging of passwords. Handled by strict logging policy.

## 20. Acceptance Criteria

- [ ] `/auth/login` returns a valid JWT upon correct credentials.
- [ ] `/auth/login` returns 401 for incorrect passwords or non-existent users.
- [ ] JWT contains `username` and `roles` claims.
- [ ] `/auth/validate` correctly identifies valid vs expired/tampered tokens.
- [ ] OpenFeign is used for all remote calls.
- [ ] No passwords or secrets are logged.
- [ ] No direct database access to User or Role services.
- [ ] All tests pass.
- [ ] No Kafka/RabbitMQ introduced.
- [ ] Dependency versions follow `CLAUDE.md`.

## 21. Planning Issues

Slight port conflict in specifications: Role Service is on 8085 and Auth Service is also on 8085. 
- **Resolution**: Auth Service will be configured to port 8086 to avoid conflict.

## 22. Implementation Prompt

> Implement `Auth Service` according to `.claude/specs/07-auth-service.md` and this plan. Read the root `CLAUDE.md`. Inspect the User and Role services to understand their contracts. Follow the implementation sequence and acceptance criteria. Do not introduce Kafka/RabbitMQ or unrelated infrastructure. Verify the build and tests before completion.
