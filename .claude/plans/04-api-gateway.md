# 04. API Gateway Implementation Plan

## 1. Plan Metadata

- Service: API Gateway
- Build Order: 04
- Assignment: 1
- Specification: .claude/specs/04-api-gateway.md
- Status: Planned
- Plan Generated: 2026-10-03
- Prerequisite Services: Product Service, Category Service, Inventory Service

## 2. Objective

The API Gateway will serve as the single entry point for the microservices ecosystem, routing requests to the appropriate downstream services and centralizing cross-cutting concerns like logging.

## 3. Existing Context

The API Gateway is the entry point for the system. It routes requests to the Product, Category, and Inventory services (Ports 8081, 8082, 8083).

## 4. Scope

### In Scope
- Implementation of Spring Cloud Gateway routing.
- Configuration of routes for Product, Category, and Inventory services.
- Implementation of a `GlobalFilter` for request logging.
- Integration tests to verify routing using `WebTestClient`.
- Actuator for health monitoring.

### Out of Scope
- JWT validation (implemented in a separate specification `08-api-gateway-jwt-security.md`).
- Circuit Breakers (Resilience4j) - to be implemented later.
- Business logic.
- Database access.

## 5. Architecture

Client
  |
API Gateway (Port 8080)
  |
  +---> Product Service (Port 8081)
  |
  +---> Category Service (Port 8082)
  |
  +---> Inventory Service (Port 8083)

The Gateway acts as a proxy, forwarding requests without modifying the business logic of the downstream services.

## 6. Implementation Dependencies

- **Maven Dependencies**:
    - `spring-cloud-starter-gateway`
    - `spring-boot-starter-actuator`
    - `spring-boot-starter-test`
- **Configuration**:
    - `server.port=8080`
    - Routing rules in `application.yaml` or Java Config.

## 7. Data Model Plan

The API Gateway is stateless and has no data model.

## 8. API Implementation Plan

The Gateway exposes the endpoints of downstream services via routes:

| Path Pattern | Downstream Service URL | Purpose |
|--------------|-----------------------|----------|
| `/products/**` | `http://localhost:8081` | Route to Product Service |
| `/categories/**`| `http://localhost:8082` | Route to Category Service |
| `/inventory/**` | `http://localhost:8083` | Route to Inventory Service |

## 9. Service Layer Plan

No business service layer exists in the Gateway. The logic is contained within Gateway Filters and Routing predicates.

## 10. Design Pattern Implementation Plan

### API Gateway Pattern
- Where: Entire service.
- Why: To provide a single entry point to a distributed system.
- Main participants: `RouteLocator`, `GatewayFilter`.

### Proxy Pattern
- Where: Spring Cloud Gateway Routing.
- Why: To forward requests without altering the core business logic.
- Main participants: `RouteDefinition`, `Predicate`, `Filter`.

## 11. Remote Communication Plan

The Gateway uses synchronous HTTP proxying.

- **Timeout**: Global timeout of 5 seconds.
- **Retry**: Basic retry for GET requests.
- **Failure Handling**: Return 503 Service Unavailable if downstream services are unreachable.

## 12. Exception Handling Plan

- **Route Not Found**: Handled by Spring Cloud Gateway (404 Not Found).
- **Service Unavailable**: Custom filter or default gateway behavior (503 Service Unavailable).
- **Unauthorized**: Handled by security filters (to be implemented later).

## 13. Security Plan

- **Authentication**: JWT validation to be implemented in phase `08-api-gateway-jwt-security.md`.
- **Authorization**: RBAC enforced at the gateway level via filters.

## 14. Testing Plan

### Unit Tests
- Test custom Gateway Filters in isolation.

### Integration Tests
- **Routing Tests**: Use `WebTestClient` to verify that requests to `/products/**`, `/categories/**`, and `/inventory/**` are correctly routed.
- **Downstream Failure Tests**: Verify that 503 responses are returned when downstream services are unavailable.

## 15. Files to Create

```text
src/main/java/com/ecommerce/api_gateway/
├── ApiGatewayApplication.java
├── filter/LoggingGlobalFilter.java
└── exception/GlobalErrorExceptionHandler.java
```

## 16. Files to Modify

None.

## 17. Implementation Sequence

1. Initialize Maven project with Spring Boot 3.2.5 and Spring Cloud 2023.0.3.
2. Add `spring-cloud-starter-gateway` and `spring-boot-starter-actuator` dependencies.
3. Implement routing configuration in `application.yaml`.
4. Implement `LoggingGlobalFilter` for request logging.
5. Add integration tests using `WebTestClient`.
6. Run `./mvnw clean test` to verify.

## 18. Verification Plan

- Run `./mvnw clean test` to verify routing.
- Run `./mvnw help:effective-pom` to check dependency versions.

## 19. Risks and Failure Scenarios

- **Downstream Service Unavailable**: Result in 503 errors. Handled by custom filters.
- **Timeout**: Request hang. Handled by global timeouts.

## 20. Acceptance Criteria

- [ ] All initial routes are functional.
- [ ] Requests are correctly proxied to downstream services.
- [ ] `server.port` is configured to 8080.
- [ ] Gateway does not contain business logic.
- [ ] Gateway does not access any database.
- [ ] Integration tests verify successful routing.
- [ ] `./mvnw clean test` passes.
- [ ] Dependency versions follow `CLAUDE.md`.

## 21. Planning Issues

None.

## 22. Implementation Prompt

> Implement `API Gateway` according to `.claude/specs/04-api-gateway.md` and this plan. Read the root `CLAUDE.md`. Follow the implementation sequence and acceptance criteria. Do not introduce business logic or database access. Verify the build and tests before completion.
