---
name: check-service
description: Audit one microservice against the project's service checklist, report missing work, and implement the incomplete required items. Uses the actual conventions from Product, Category, Inventory, and API Gateway in the repository plus the rules in CLAUDE.md and the service specification.
argument-hint: "<service-name>"
disable-model-invocation: true
---

# Check and Complete Service

Use:

```text
/check-service <service-name>
```

Examples:

```text
/check-service product-service
/check-service category-service
/check-service inventory-service
/check-service role-service
/check-service user-service
/check-service auth-service
```

The command has two responsibilities:

1. Audit the requested service against the project checklist and its specification.
2. Implement missing required work when the service is incomplete.

This is NOT a passive review command.

If required work is missing, implement it, test it, and re-check the service.

---

# 1. Source of truth

Before doing anything:

1. Read root `CLAUDE.md`.
2. Read the service specification:
   `.claude/specs/<service-spec>.md`
   when it exists.
3. Inspect the actual service code.
4. Inspect the current Product, Category, Inventory, and API Gateway implementations to preserve established conventions.
5. Inspect the service's `pom.xml`.
6. Inspect the service's tests.

Never invent a checklist that contradicts `CLAUDE.md`.

If no specification exists, derive the service responsibilities from `CLAUDE.md` and the existing repository conventions and clearly state that no service specification was available.

---

# 2. Important safety rules

This command may modify code.

Before modifying anything:

- confirm the requested service directory exists or is the intended new service
- do not modify unrelated services
- do not redesign the architecture unless the current code violates `CLAUDE.md`
- do not add new microservices
- do not add Kafka
- do not add RabbitMQ
- do not add Redis/Eureka/Config Server/Docker unless the service specification explicitly requires them at this stage
- do not upgrade Spring Boot or Spring Cloud versions
- do not invent dependency versions
- do not silently change an existing public API contract
- preserve the repository's naming conventions
- do not remove working functionality to make a checklist pass

If a missing item requires a cross-service contract change, first inspect all known consumers and update only the necessary services.

---

# 3. Checklist

The service must be checked against the following categories.

## A. Project structure

Check:

- [ ] Service has its own Maven project.
- [ ] `pom.xml` exists.
- [ ] Java source package matches the repository naming convention.
- [ ] Main application class follows `<PascalCaseServiceName>Application`.
- [ ] `src/main/resources/application.properties` or the established configuration convention exists.
- [ ] Test source structure exists.
- [ ] No unnecessary generated files or classes exist.

Use existing repository examples:

```text
product-service
-> com.ecommerce.product_service
-> ProductServiceApplication

category-service
-> com.ecommerce.category_service
-> CategoryServiceApplication

inventory-service
-> com.ecommerce.inventory_service
-> InventoryServiceApplication
```

---

## B. Maven and dependency correctness

Check:

- [ ] Java version matches `CLAUDE.md`.
- [ ] Spring Boot version matches `CLAUDE.md`.
- [ ] Spring Cloud release train matches `CLAUDE.md` when applicable.
- [ ] Spring Boot parent is used correctly.
- [ ] Spring Cloud BOM is used when Spring Cloud dependencies exist.
- [ ] No unnecessary explicit versions on Boot/Cloud-managed dependencies.
- [ ] No duplicate dependency declarations.
- [ ] No unresolved dependencies.
- [ ] No Kafka/RabbitMQ dependencies.
- [ ] Dependencies are justified by the service specification.
- [ ] Test dependencies exist.
- [ ] Actuator exists when required by the project baseline.
- [ ] OpenAPI dependency exists when the service specification/project standard requires it.

Run:

```bash
./mvnw clean test
./mvnw help:effective-pom
./mvnw dependency:tree
```

If dependency conflicts or version drift are found, fix the `pom.xml` before continuing.

---

## C. Application configuration

Check:

- [ ] `server.port` is correct.
- [ ] `spring.application.name` matches artifact/service naming.
- [ ] Database configuration is present when persistence is required.
- [ ] Service-specific H2 database name is unique during H2 development.
- [ ] H2 console configuration matches project conventions where H2 is used.
- [ ] Remote service URLs are externalized when remote calls exist.
- [ ] No secrets are hard-coded.
- [ ] No credentials are committed to source code.

---

## D. Layered architecture

For a normal business service, check whether applicable:

- [ ] controller
- [ ] service interface
- [ ] service implementation
- [ ] repository
- [ ] entity/domain model
- [ ] DTOs
- [ ] exception package
- [ ] client package for remote services
- [ ] strategy/factory/composite/etc. package when required by the specification

Use constructor injection.

Controllers should remain thin.

Remote calls must not be hidden in entities or DTO mapping code unless explicitly justified.

---

## E. REST API

Check every endpoint in the service specification.

For each endpoint:

- [ ] path is correct
- [ ] HTTP method is correct
- [ ] request DTO exists
- [ ] validation exists
- [ ] response DTO exists where appropriate
- [ ] status code is reasonable
- [ ] error behavior is documented/implemented
- [ ] controller delegates to service layer
- [ ] no business logic is embedded in the controller

Use the existing services as the default style:

```java
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {
    ...
}
```

and equivalent Category/Inventory style.

---

## F. Entity and database model

Check:

- [ ] entity exists when persistence is required
- [ ] primary key exists
- [ ] UUID strategy follows existing project convention
- [ ] required fields have appropriate constraints
- [ ] unique fields have unique constraints where required
- [ ] no cross-service database foreign keys
- [ ] indexes/constraints required by the specification exist
- [ ] persistence naming follows the project style

For example, the current project uses UUID identifiers and JPA entities.

---

## G. DTOs and validation

Check:

- [ ] request DTOs exist
- [ ] response DTOs exist
- [ ] entities are not unnecessarily exposed directly from controllers
- [ ] validation annotations exist
- [ ] validation messages are useful
- [ ] sensitive fields are not exposed
- [ ] mapping is explicit and understandable

Current project convention includes DTOs such as:

```text
ProductRequest
ProductResponse
CategoryRequest
CategoryResponse
InventoryRequest
InventoryResponse
```

Preserve that style.

---

## H. Repository

If persistence is required:

- [ ] repository exists
- [ ] extends appropriate Spring Data interface
- [ ] query methods match actual business needs
- [ ] repository contains persistence concerns only
- [ ] no business logic is embedded in repository methods

Example project convention:

```java
public interface ProductRepository extends JpaRepository<Product, UUID> {
}
```

---

## I. Business logic

Check the service implementation for:

- [ ] required business rules
- [ ] validation beyond bean validation where required
- [ ] correct status transitions
- [ ] correct calculations
- [ ] duplicate handling
- [ ] not-found handling
- [ ] idempotency where needed
- [ ] transaction boundaries where needed

Do not move business logic into controllers.

---

## J. Design patterns

Check the design patterns specified for this service.

For every required pattern:

- [ ] pattern exists
- [ ] pattern is actually used
- [ ] pattern solves a real problem
- [ ] implementation is simple and understandable

Known project examples:

### Product Service
- Repository
- DTO
- Factory

### Category Service
- Composite
- Repository
- DTO

### Inventory Service
- Strategy
- Repository
- DTO
- Service Layer

Do not add patterns merely to increase the count.

If a specified pattern is not useful in the current service context, explain the issue before changing architecture.

---

## K. Service-to-service communication

For each OpenFeign/remote dependency:

- [ ] Feign client exists
- [ ] endpoint path matches provider
- [ ] DTO contract matches provider
- [ ] URL/configuration is externalized
- [ ] timeout/failure behavior is considered
- [ ] 404 behavior is handled appropriately
- [ ] retry is only used when safe
- [ ] circuit breaker is added when the specification requires resilience
- [ ] no direct database access is used

Project architecture:

```text
Product
  -> Category
  -> Inventory

Cart
  -> Product
  -> Pricing
  -> Coupon

Auth
  -> User
  -> Role

Order
  -> Cart
  -> Inventory
  -> Payment
  -> Shipping
```

All are synchronous REST/OpenFeign calls unless `CLAUDE.md` is explicitly changed.

---

## L. Exception handling

Check:

- [ ] domain exceptions exist where needed
- [ ] global exception handler exists
- [ ] validation errors have consistent responses
- [ ] 404/400/409/500 scenarios are handled appropriately
- [ ] generic internal errors do not leak implementation details
- [ ] downstream Feign failures are handled when relevant

Each service should own its own exception handling.

---

## M. Security

For secured services:

- [ ] password hashing uses BCrypt or approved project approach
- [ ] no plaintext password persistence
- [ ] sensitive information is not returned
- [ ] JWT handling follows Auth/Gateway responsibilities
- [ ] authorization rules are present when required
- [ ] security configuration is minimal and intentional
- [ ] security tests exist where appropriate

User Service must not generate JWTs.

Auth Service owns authentication/token creation.

API Gateway is responsible for JWT validation once that phase is implemented.

---

## N. Testing

Minimum expected test categories:

### Context
- [ ] application context loads

### Unit
- [ ] service business rules
- [ ] edge cases
- [ ] required design pattern behavior

### Controller
- [ ] successful request
- [ ] validation failure
- [ ] not found/error response

### Repository
- [ ] important persistence queries/constraints

### Integration
- [ ] important service workflows

### Remote-call scenarios
- [ ] success
- [ ] downstream 404 where relevant
- [ ] downstream failure where relevant

Do not consider `contextLoads()` alone sufficient.

---

## O. Observability

Check:

- [ ] Actuator is included when required.
- [ ] useful logs exist around important workflows.
- [ ] secrets/passwords/tokens are not logged.
- [ ] errors include useful context.
- [ ] correlation/trace IDs are not invented unless the project phase requires them.

---

## P. Documentation

Check:

- [ ] service specification exists
- [ ] API contract matches implementation
- [ ] configuration is documented where needed
- [ ] design-pattern usage is documented where non-obvious
- [ ] README is updated when appropriate

Do not create documentation outside the intended repository structure without a reason.

---

# 4. Implementation behavior

After the audit:

### If everything is complete

Do NOT make unnecessary changes.

Run verification:

```bash
./mvnw clean test
./mvnw help:effective-pom
./mvnw dependency:tree
```

Then report that the service is complete.

### If items are incomplete

Implement the missing required items in a sensible order:

1. Maven/dependency correctness
2. application configuration
3. domain/entity
4. repository
5. DTOs/validation
6. business/service layer
7. design pattern components
8. remote clients
9. controllers
10. exception handling
11. security
12. tests
13. documentation
14. verification

After each substantial change, run focused tests.

At the end, run:

```bash
./mvnw clean test
```

Then:

```bash
./mvnw help:effective-pom
./mvnw dependency:tree
```

Fix failures before declaring the service complete.

---

# 5. Existing service conventions to preserve

Inspect these current services first:

- `product-service`
- `category-service`
- `inventory-service`
- `api-gateway`

Current conventions include:

### Packages

```text
com.ecommerce.product_service
com.ecommerce.category_service
com.ecommerce.inventory_service
com.ecommerce.api_gateway
```

### Main classes

```text
ProductServiceApplication
CategoryServiceApplication
InventoryServiceApplication
ApiGatewayApplication
```

### Layers

```text
controller
service
repository
entity
dto
exception
client
strategy
factory
```

Only create a layer when the service actually needs it.

### Injection

Use Lombok constructor injection:

```java
@RequiredArgsConstructor
```

### REST controllers

Use:

```java
@RestController
@RequestMapping(...)
@RequiredArgsConstructor
```

### Response style

Use `ResponseEntity` consistently with the existing services unless there is a strong reason not to.

### Persistence

Use JPA repositories with UUID identifiers.

### Database

Use H2 during the current development phase unless the specification explicitly requires another database.

---

# 6. Do not over-fix

This command is allowed to complete missing required work, but it must not become a general refactoring command.

Do NOT:

- rename all packages because you prefer another convention
- rewrite working classes
- replace manual mapping with ModelMapper without a requirement
- introduce MapStruct without a requirement
- introduce Kafka
- split Order into unnecessary microservices
- migrate H2 to PostgreSQL prematurely
- add Redis without a use case
- add Eureka/Config Server before their planned phase
- upgrade Spring Boot/Spring Cloud
- rewrite all tests merely for style

---

# 7. Final report

The final report MUST contain:

## Audit Summary

| Area | Status |
|------|--------|
| Project structure | ✅ / ❌ |
| Dependencies | ✅ / ❌ |
| Configuration | ✅ / ❌ |
| API | ✅ / ❌ |
| Entity/model | ✅ / ❌ |
| DTO/validation | ✅ / ❌ |
| Repository | ✅ / ❌ |
| Business logic | ✅ / ❌ |
| Design patterns | ✅ / ❌ |
| Remote communication | ✅ / ❌ |
| Exception handling | ✅ / ❌ |
| Security | ✅ / ❌ / N/A |
| Tests | ✅ / ❌ |
| Observability | ✅ / ❌ |
| Documentation | ✅ / ❌ |

Then report:

### Completed during this run
- ...

### Remaining items
- ...

### Files changed
- ...

### Verification
- `./mvnw clean test`: PASS/FAIL
- `./mvnw help:effective-pom`: PASS/FAIL
- `./mvnw dependency:tree`: PASS/FAIL

If any required item remains incomplete, explicitly say why.

---

# 8. Completion rule

A service is considered COMPLETE only when:

- required functionality is implemented
- required patterns are implemented and justified
- API contracts are satisfied
- validation exists
- exception handling exists
- dependencies are correct
- no version mismatch exists
- tests are meaningful
- Maven verification passes
- no forbidden technology was introduced
- no required work from the specification remains

Do not declare COMPLETE based only on compilation.
