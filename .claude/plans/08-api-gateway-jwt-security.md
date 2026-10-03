# 08. API Gateway JWT Security Implementation Plan

## 1. Plan Metadata

- Service: API Gateway (Security Enhancement)
- Build Order: 08
- Assignment: 2
- Specification: .claude/specs/08-api-gateway-jwt-security.md
- Status: Planned
- Plan Generated: 2026-10-03
- Prerequisite Services: Auth Service (for token generation logic)

## 2. Objective

Enhance the API Gateway to act as the security enforcement point by validating JWTs for protected routes, extracting user identity and roles, and propagating this context to downstream services via HTTP headers.

## 3. Existing Context

The API Gateway (Port 8080) already handles basic routing to Product, Category, and Inventory services. This phase adds a security layer using a `GlobalFilter` to intercept requests before they are routed.

## 4. Scope

### In Scope
- Implementation of a `JwtAuthenticationFilter` (Global Filter).
- JWT validation logic (signature, expiration, issuer) using the shared secret from the Auth Service.
- Header enrichment: adding `X-User-Id` and `X-User-Role` to forwarded requests.
- Implementation of a "Public Routes" bypass list.
- Global exception handler for 401 (Unauthorized) and 403 (Forbidden) responses.
- Integration tests to verify security flow (public vs protected access).

### Out of Scope
- Token generation (handled by Auth Service).
- User/Role management (handled by User/Role Services).
- Token revocation/blacklist (deferred).

## 5. Architecture

Client
  |
  | (Request + JWT)
  v
API Gateway
  |
  +---> [ JWT Validation Filter ] --- (Invalid Token) ---> 401 Unauthorized
  |           |
  |           +--- (Valid Token) --- [ Header Enrichment ]
  |                                          |
  |                                          v
  +------------------------------------------+
  |
  +---> Downstream Services (with X-User-Id, X-User-Role headers)

The Gateway validates the token locally using a shared symmetric secret, avoiding a network call to the Auth Service for every request.

## 6. Implementation Dependencies

- **Maven Dependencies**:
    - `spring-boot-starter-security`
    - `spring-security-oauth2-resource-server`
    - `spring-security-oauth2-jose`
    - `io.jsonwebtoken:jjwt-api`, `jjwt-impl`, `jjwt-jackson` (for consistency with Auth Service)
- **Configuration**:
    - `jwt.secret`: Shared secret key.
    - `jwt.issuer`: `auth-service`.
    - `gateway.security.public-routes`: `/auth/**,/users/register`.

## 7. Data Model Plan

The API Gateway remains stateless.

## 8. API Implementation Plan

The Gateway modifies the behavior of existing routes:

| Route Type | Path Pattern | Requirement | Outcome |
|------------|--------------|-------------|---------|
| Public | `/auth/**`, `/users/register` | None | Forwarded directly |
| Protected | All others | Valid JWT in `Authorization` header | Forwarded with identity headers |

## 9. Service Layer Plan

The logic resides in the `JwtAuthenticationFilter`:
1. **Intercept Request**: Check if the path matches any `public-routes`. If yes, proceed.
2. **Extract Token**: Look for `Authorization: Bearer <token>` header. If missing, return 401.
3. **Validate Token**: Use `JwtUtil` to check signature and expiration. If invalid, return 401.
4. **Enrich Headers**:
    - Strip any incoming `X-User-Id` or `X-User-Role` to prevent spoofing.
    - Extract `sub` (userId) and `roles` from claims.
    - Add `X-User-Id` and `X-User-Role` (comma-separated) to the request.
5. **Forward**: Proceed to the next filter in the chain.

## 10. Design Pattern Implementation Plan

### Intercepting Filter
- Where: `JwtAuthenticationFilter`
- Why: To implement security as a cross-cutting concern.
- Main participants: `GlobalFilter` implementation.

### Adapter (JWT Decoder)
- Where: `JwtUtil`
- Why: To encapsulate JJWT library details.
- Main participants: `JwtUtil`.

## 11. Remote Communication Plan

No remote calls are made during validation; it is a local cryptographic check.

## 12. Exception Handling Plan

- **JwtAuthenticationException**: Thrown on invalid/expired tokens. Maps to 401 Unauthorized.
- **AccessDeniedException**: Thrown when a valid token lacks required roles. Maps to 403 Forbidden.
- **Global Exception Handler**: Returns standardized JSON: `{ "error": "..." }`.

## 13. Security Plan

- **Symmetric Secret**: Use a strong key loaded from environment variables.
- **Spoofing Prevention**: Explicitly clear `X-User-Id` and `X-User-Role` headers before adding them.
- **Token Format**: Strictly enforce `Bearer ` prefix.

## 14. Testing Plan

### Unit Tests
- `JwtUtil`: Test valid tokens, expired tokens, tampered signatures, and missing claims.

### Integration Tests
- **Public Access**: Verify `/auth/login` is accessible without a token.
- **Protected Access (No Token)**: Verify `/products` returns 401.
- **Protected Access (Invalid Token)**: Verify `/products` returns 401.
- **Protected Access (Valid Token)**: Verify `/products` returns 200 (mocking downstream).
- **Header Enrichment**: Verify that a mocked downstream service receives the correct `X-User-Id` and `X-User-Role`.

## 15. Files to Create/Modify

- **Create**: `src/main/java/com/ecommerce/api_gateway/security/JwtUtil.java`
- **Create**: `src/main/java/com/ecommerce/api_gateway/security/JwtAuthenticationFilter.java`
- **Create**: `src/main/java/com/ecommerce/api_gateway/security/JwtAuthenticationException.java`
- **Modify**: `src/main/java/com/ecommerce/api_gateway/security/SecurityConfig.java` (to register the filter).

## 16. Implementation Sequence

1. Add security dependencies to `pom.xml`.
2. Implement `JwtUtil` for token parsing and validation.
3. Implement `JwtAuthenticationFilter` as a `GlobalFilter`.
4. Configure the filter to handle public routes and perform header enrichment.
5. Implement the global exception handler for security errors.
6. Add configuration properties for the secret and public routes.
7. Write unit tests for `JwtUtil`.
8. Write integration tests for the security flow using `WebTestClient`.
9. Run `./mvnw clean test` to verify.

## 17. Verification Plan

- Run `./mvnw clean test`.
- Verify that downstream services receive the expected headers when a valid JWT is used.

## 18. Risks and Failure Scenarios

- **Secret Mismatch**: If the Gateway and Auth Service use different secrets, all tokens will be rejected.
- **Performance**: JWT parsing on every request adds latency (though minimal).
- **Spoofing**: If headers are not cleared, clients can impersonate users.

## 19. Acceptance Criteria

- [ ] Public routes are bypassed.
- [ ] Protected routes require a valid JWT.
- [ ] Expired/Tampered tokens result in 401.
- [ ] Downstream services receive `X-User-Id` and `X-User-Role`.
- [ ] Client-provided identity headers are stripped.
- [ ] `./mvnw clean test` passes.

## 20. Planning Issues

None.

## 21. Implementation Prompt

> Enhance `API Gateway` with JWT security according to `.claude/specs/08-api-gateway-jwt-security.md` and this plan. Read the root `CLAUDE.md`. Ensure consistency with the Auth Service's token structure. Implement the `GlobalFilter` for validation and header enrichment. Verify the build and tests before completion.
