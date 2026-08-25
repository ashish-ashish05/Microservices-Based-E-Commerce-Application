# 04. API Gateway Specification

## 1. Document Status

- Service: API Gateway
- Build Order: 04
- Assignment: 1
- Status: Proposed
- Scope: Specification only

## 2. Purpose

The API Gateway serves as the single entry point for all clients to the microservices ecosystem. It is responsible for routing requests to the appropriate downstream services, centralizing cross-cutting concerns, and acting as the primary security enforcement point for the entire platform.

## 3. Responsibilities

- Route incoming HTTP requests to downstream microservices (Product, Category, Inventory, etc.).
- Centralize cross-cutting concerns such as logging, request tracking, and rate limiting (future).
- Enforce security policies (JWT validation) before forwarding requests to protected services.
- Provide a unified API surface for clients, hiding the internal network topology.
- Handle basic request/response transformations if needed.

## 4. Non-Responsibilities

- Containing business logic.
- Directly accessing microservice databases.
- Managing user accounts or categories.
- Processing payments or orders.

## 5. Service Boundary

- **Data Ownership**: Owns no business data. It may maintain routing configurations.
- **Domain Ownership**: Owns the "Entry Point" and "Traffic Management" domain.
- **External Dependencies**: All business microservices (Downstream).
- **Boundary Justification**: By isolating routing and security from business logic, services can be modified, moved, or scaled without updating every client.

## 6. Architecture

Client
  |
API Gateway <--- (JWT Validation)
  |
  +---> Product Service
  |
  +---> Category Service
  |
  +---> Inventory Service
  |
  +---> (Other Services...)

## 7. Service-to-Service Communication

The Gateway uses Spring Cloud Gateway's routing mechanism to proxy requests.

| Consumer | Provider | Method | Endpoint | Purpose | Failure Consideration |
|----------|----------|--------|----------|---------|-----------------------|
| Gateway  | Product   | ALL    | `/products/**` | Proxy to Product Service | Timeout, 503 Service Unavailable |
| Gateway  | Category  | ALL    | `/categories/**` | Proxy to Category Service | Timeout, 503 Service Unavailable |
| Gateway  | Inventory | ALL    | `/inventory/**` | Proxy to Inventory Service | Timeout, 503 Service Unavailable |

**Synchronous Communication Policy**:
- **Timeouts**: Configurable global timeout for downstream services (default 5 seconds).
- **Retry**: Basic retry for idempotent GET requests.
- **Circuit Breaker**: Integrated via Resilience4j (to be implemented in later phases).
- **Fallback**: Return a standardized "Service Unavailable" JSON response.

## 8. API Specification

The API Gateway does not expose its own business APIs; it exposes the APIs of downstream services through routes.

### Routing Table (Initial)

| Path Pattern | Downstream Service URL | Purpose |
|--------------|-----------------------|----------|
| `/products/**` | `http://localhost:8081` | Route to Product Service |
| `/categories/**`| `http://localhost:8082` | Route to Category Service |
| `/inventory/**` | `http://localhost:8083` | Route to Inventory Service |

## 9. Data Model

The API Gateway is stateless and does not maintain a database.

## 10. DTOs

The API Gateway typically passes request and response bodies through without modification. It does not define business DTOs.

## 11. Business Rules

- **Route Priority**: Specific routes take precedence over generic routes.
- **Security First**: All protected routes must pass through a JWT validation filter before being routed.
- **Public Routes**: Routes such as `/auth/login` or `/auth/register` (when implemented) must be explicitly excluded from security filters.

## 12. Design Patterns

### Pattern: API Gateway Pattern
- Where: Entire service.
- Why: To provide a single entry point to a distributed system.
- Problem it solves: Prevents clients from needing to know the location and port of every microservice.

### Pattern: Proxy Pattern
- Where: Spring Cloud Gateway Routing.
- Why: To forward requests without altering the core business logic.
- Problem it solves: Simplifies the client-side request process.

## 13. Dependencies

### Required
- `spring-cloud-starter-gateway`: Core routing and filter functionality.
- `spring-boot-starter-actuator`: For health and monitoring.
- `spring-boot-starter-test`: For testing routing configurations.

### Optional / Later
- `spring-boot-starter-security`: For JWT validation.
- `spring-security-oauth2-resource-server`: For OAuth2/JWT support.
- `spring-security-oauth2-jose`: For JWT decoding/verification.

## 14. Configuration

```properties
server.port=8080

# Routing Configurations
spring.cloud.gateway.routes[0].id=product-service
spring.cloud.gateway.routes[0].uri=http://localhost:8081
spring.cloud.gateway.routes[0].predicates[0]=Path=/products/**

spring.cloud.gateway.routes[1].id=category-service
spring.cloud.gateway.routes[1].uri=http://localhost:8082
spring.cloud.gateway.routes[1].predicates[0]=Path=/categories/**

spring.cloud.gateway.routes[2].id=inventory-service
spring.cloud.gateway.routes[2].uri=http://localhost:8083
spring.cloud.gateway.routes[2].predicates[0]=Path=/inventory/**
```

## 15. Security

- **Authentication**: Centralized JWT validation (to be implemented after Auth Service).
- **Authorization**: Role-based access control (RBAC) enforced at the gateway level via filters.
- **Public Routes**: `/auth/**` routes are public.
- **Protected Routes**: All other routes require a valid JWT.

## 16. Exception and Error Handling

The Gateway handles routing errors and downstream service failures.

| Error | Trigger | HTTP Status | Response Shape | Handling |
|-------|---------|-------------|---------------|-----------|
| Route Not Found | No matching predicate for path | 404 Not Found | `{ "error": "Route not found" }` | Default Gateway Handler |
| Service Unavailable | Downstream service is down | 503 Service Unavailable | `{ "error": "Service currently unavailable" }` | Custom Gateway Filter |
| Unauthorized | Missing or invalid JWT | 401 Unauthorized | `{ "error": "Unauthorized access" }` | Security Filter |

## 17. Transaction Boundaries

The API Gateway is stateless and does not participate in database transactions.

## 18. Validation

- **Route Validation**: Ensure request paths match predefined predicates.
- **Header Validation**: Validate required headers (e.g., `Authorization`) for protected routes.

## 19. Testing Strategy

### Unit Tests
- Test custom Gateway Filters in isolation.

### Integration Tests
- **Routing Tests**: Use `WebTestClient` to verify that requests to `/products/**` are correctly routed to the Product Service.
- **Security Tests**: Verify that requests without JWTs are blocked for protected routes and allowed for public routes.
- **Downstream Failure Tests**: Verify that 503 responses are returned when downstream services are unavailable.

## 20. Observability

- **Logs**: Log every incoming request (Method, Path, Client IP, Response Time).
- **Health**: Actuator `/health` endpoint.
- **Metrics**: Track request throughput, error rates per route, and latency.

## 21. Implementation Sequence

1. Initialize Maven project with Spring Boot 3.2.5 and Spring Cloud 2023.0.3.
2. Add `spring-cloud-starter-gateway` and `spring-boot-starter-actuator` dependencies.
3. Implement routing configuration in `application.properties` or a Java Configuration class.
4. Implement a basic `GlobalFilter` for request logging.
5. Add integration tests using `WebTestClient` to verify routes.
6. Run `./mvnw clean test` to verify.
7. (Later) Add `spring-boot-starter-security` and implement JWT validation filters.
8. (Later) Implement circuit breakers via Resilience4j.

## 22. Acceptance Criteria

- [ ] All initial routes (`/products/**`, `/categories/**`, `/inventory/**`) are functional.
- [ ] Requests are correctly proxied to downstream services.
- [ ] `server.port` is configured to 8080.
- [ ] Gateway does not contain any business logic.
- [ ] Gateway does not access any database.
- [ ] Integration tests verify successful routing.
- [ ] `./mvnw clean test` passes.
- [ ] Dependency versions follow `CLAUDE.md`.

## 23. Out of Scope

- Implementing business APIs.
- Implementing a User Interface.
- Managing service discovery (handled by localhost URLs initially, Eureka later).

## 24. Decisions and Open Questions

- **Decision**: Use `application.properties` for initial routing configuration for simplicity and visibility.
- **Decision**: Synchronous proxying is used; the Gateway does not buffer or aggregate responses from multiple services (that is the responsibility of the facade services like Product Service).
- **Question**: Should we use a custom domain or keep `localhost` for initial development? (Decision: `localhost` until Docker/Deployment phase).

## 25. Recommended Git Branch

Suggested Branch: `feature/api-gateway`

Please switch to this branch before starting implementation.
