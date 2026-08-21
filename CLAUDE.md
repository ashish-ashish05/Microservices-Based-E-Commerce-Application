# Microservices-Based E-Commerce Application

## 1. Project Description

This is a learning-focused, microservices-based E-Commerce application built with Java, Spring Boot, and Spring Cloud.

The project is intentionally divided into assignments. Each assignment introduces a group of related microservices and design patterns. The final system will integrate the services into one end-to-end e-commerce platform.

The project is designed to teach:

- Microservice boundaries and data ownership
- REST-based synchronous service communication
- Spring Boot and Spring Cloud
- API Gateway
- Authentication and authorization
- Clean layered architecture
- Design patterns applied to real business problems
- Distributed failure handling
- Resilience patterns
- Testing
- Docker/containerization
- Service discovery and centralized configuration later

## 2. Core Architecture Rules

- Use Java 17.
- Use Spring Boot 3.2.5 for the current project baseline.
- Use Spring Cloud 2023.0.3 where Spring Cloud components are required.
- Use Maven.
- Each microservice owns its own database.
- Never access another microservice's database directly.
- Communicate between services through REST/OpenFeign unless a future decision explicitly changes this.
- This project intentionally does NOT use Kafka or another message broker.
- Prefer synchronous communication and explicit orchestration.
- Important remote calls should eventually use timeout, retry, circuit breaker, and fallback mechanisms with Resilience4j where appropriate.
- Use DTOs at service/API boundaries.
- Prefer constructor injection.
- Keep controllers thin.
- Keep business logic in service/domain layers.
- Do not introduce a design pattern simply because it is available. Use a pattern when it solves a real problem.
- Do not create a separate microservice when a well-defined domain object can remain inside an existing service without creating unnecessary coupling.
- Do not expose passwords, secrets, or internal security data through public APIs.
- Do not hard-code secrets, JWT keys, passwords, API keys, or credentials in source code.
- Preserve existing service contracts unless a change is required and all consumers are considered.
- Prefer explicit, understandable code over excessive abstraction.







## Product Service

Port in current project: 8081

Responsibilities:

- Create products
- Update products
- Get a product
- List products
- Maintain product identity and ownership of product data
- Reference a category through categoryId
- Request inventory creation when a product is created
- Aggregate category and inventory information into product responses

Current service-to-service communication:

- Product -> Category Service
- Product -> Inventory Service

Design patterns:

- Repository Pattern
- DTO Pattern
- Factory Pattern
- Facade/Aggregation-style application service behavior where useful

Current implementation notes:

- Product owns the Product UUID.
- Inventory references productId but does not share Product database tables.
- Product creation automatically requests Inventory creation with quantity 0.
- Do not move remote service calls into entities or DTOs.

Dependencies:

- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-validation
- spring-cloud-starter-openfeign
- com.h2database:h2 (development)
- org.projectlombok:lombok
- spring-boot-starter-actuator
- spring-boot-starter-test
- springdoc-openapi (recommended/optional)
- spring-cloud-starter-circuitbreaker-resilience4j (later, when resilience is introduced)

## Category Service

Port in current project: 8082

Responsibilities:

- Create categories
- Get categories
- Maintain parent-child relationships
- Build hierarchical category trees
- Provide category information to Product Service

Design patterns:

- Composite Pattern
- Repository Pattern
- DTO Pattern

Current category model:

- id
- name
- parentCategoryId

Dependencies:

- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-validation
- com.h2database:h2 (development)
- org.projectlombok:lombok
- spring-boot-starter-actuator
- spring-boot-starter-test
- springdoc-openapi (recommended/optional)

## Inventory Service

Port in current project: 8083

Responsibilities:

- Create/update inventory
- Get inventory
- Check stock availability
- Reserve stock
- Release stock
- Maintain product stock quantity

Design patterns:

- Strategy Pattern
- Repository Pattern
- DTO Pattern
- Service Layer Pattern

Current Strategy:

- StockStrategy
- DefaultStockStrategy

Dependencies:

- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-validation
- com.h2database:h2 (development)
- org.projectlombok:lombok
- spring-boot-starter-actuator
- spring-boot-starter-test
- springdoc-openapi (recommended/optional)
- spring-cloud-starter-circuitbreaker-resilience4j (later if remote dependencies are introduced)


## API Gateway

Port in current project: 8080

Responsibilities:

- Single client-facing entry point
- Route requests to internal services
- Centralize cross-cutting concerns
- Later validate JWTs
- Later support rate limiting and other gateway concerns

Design patterns / architecture:

- API Gateway Pattern
- Proxy-style routing
- Circuit Breaker integration later where useful

Main dependencies:

- spring-cloud-starter-gateway
- spring-boot-starter-actuator
- spring-boot-starter-test
- spring-boot-starter-security (later when Gateway JWT validation is added)
- spring-security-oauth2-resource-server (later)
- spring-security-oauth2-jose (later)

Notes:

- Gateway should not contain business logic.
- Gateway should not directly access service databases.
- Current routing uses localhost service URLs.
- Service discovery can replace localhost URLs later.


## Role Service

Responsibilities:

- Manage roles
- Manage role metadata
- Manage permissions as the authorization model evolves
- Provide role information to Auth Service / User flows

Possible roles:

- USER
- ADMIN

Design patterns:

- Repository Pattern
- DTO Pattern
- Strategy Pattern later if permission evaluation becomes complex

Dependencies:

- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-validation
- com.h2database:h2 (development)
- org.projectlombok:lombok
- spring-boot-starter-actuator
- spring-boot-starter-test
- springdoc-openapi (recommended/optional)


## User Service

Planned port: 8084

Responsibilities:

- Register users
- Maintain user profile data
- Store password hashes
- Store roleId/reference
- Provide user lookup for Auth Service
- Manage user lifecycle

Must NOT:

- Generate JWT tokens
- Validate JWTs as its primary responsibility
- Handle authentication flow

Design patterns:

- Repository Pattern
- DTO Pattern
- Service Layer Pattern
- Facade-style application service where appropriate

Dependencies:

- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-validation
- spring-boot-starter-security
- com.h2database:h2 (development)
- org.projectlombok:lombok
- spring-boot-starter-actuator
- spring-boot-starter-test
- spring-security-test
- springdoc-openapi (recommended/optional)

Security:

- Use BCryptPasswordEncoder for password hashing.
- Never return password hashes in public UserResponse DTOs.



## Auth Service

Responsibilities:

- Authenticate users
- Verify passwords through User Service data
- Obtain role information
- Generate JWT access tokens
- Validate/inspect JWTs where appropriate for Auth responsibilities
- Provide login-related APIs

Communication:

- Auth -> User Service
- Auth -> Role Service

Design patterns:

- Strategy Pattern for authentication strategies
- Facade Pattern for the login/authentication workflow
- DTO Pattern
- Repository Pattern only if Auth owns persistent authentication data
- Adapter Pattern later if an external identity provider is introduced

JWT:

- Use JJWT for learning-oriented token creation/verification.
- Never hard-code JWT secrets.
- Load secrets from secure configuration/environment variables.

Dependencies:

- spring-boot-starter-web
- spring-boot-starter-security
- spring-boot-starter-validation
- spring-cloud-starter-openfeign
- io.jsonwebtoken:jjwt-api
- io.jsonwebtoken:jjwt-impl (runtime)
- io.jsonwebtoken:jjwt-jackson (runtime)
- spring-boot-starter-actuator
- spring-boot-starter-test
- spring-security-test
- springdoc-openapi (recommended/optional)



## API Gateway JWT Security

After Auth Service is implemented, the Gateway will validate JWTs before forwarding protected requests.

Additional Gateway dependencies:

- spring-boot-starter-security
- spring-security-oauth2-resource-server
- spring-security-oauth2-jose

Design:

Client
  -> API Gateway
  -> JWT validation
  -> protected microservice

Public routes such as login/registration should be explicitly configured.



## Pricing Service

Responsibilities:

- Calculate product prices
- Calculate cart totals
- Apply pricing rules
- Apply discounts
- Coordinate multiple pricing strategies

Design patterns:

- Strategy Pattern
- Chain of Responsibility
- DTO Pattern
- Repository Pattern if pricing rules are persisted

Possible strategies:

- Regular price
- Seasonal discount
- Bulk discount
- Member discount

Dependencies:

- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-validation
- spring-cloud-starter-openfeign
- com.h2database:h2 (development)
- org.projectlombok:lombok
- spring-boot-starter-actuator
- spring-boot-starter-test
- springdoc-openapi (recommended/optional)



## Coupon Service

Responsibilities:

- Create/manage coupons
- Validate coupon codes
- Check expiry
- Check eligibility
- Calculate coupon discount
- Enforce usage limits

Design patterns:

- Strategy Pattern
- Chain of Responsibility
- Repository Pattern
- DTO Pattern

Dependencies:

- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-validation
- com.h2database:h2 (development)
- org.projectlombok:lombok
- spring-boot-starter-actuator
- spring-boot-starter-test
- springdoc-openapi (recommended/optional)


## Cart Service

Responsibilities:

- Create and manage a user's cart
- Add items
- Update quantities
- Remove items
- Clear cart
- Retrieve cart
- Validate product availability
- Associate carts with user identity

Communication:

- Cart -> Product Service
- Cart -> Pricing Service

Design patterns:

- Builder Pattern for cart construction where helpful
- Strategy Pattern for cart rules if multiple policies are needed
- Repository Pattern
- DTO Pattern

Dependencies:

- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-validation
- spring-cloud-starter-openfeign
- com.h2database:h2 (development)
- org.projectlombok:lombok
- spring-boot-starter-actuator
- spring-boot-starter-test
- springdoc-openapi (recommended/optional)
- spring-cloud-starter-circuitbreaker-resilience4j (later)

Redis:

- Optional later for cart caching / performance.
- Do not add Redis until there is a concrete learning goal for it.

Potential later dependency:

- spring-boot-starter-data-redis




## Order Service

The project will keep Order, OrderItem, and OrderStatus inside the Order Service instead of creating unnecessary microservices for each entity.

Responsibilities:

- Create orders
- Convert cart to order
- Maintain order items
- Maintain order status
- Coordinate inventory reservation
- Coordinate payment
- Coordinate shipping
- Cancel orders
- Apply compensation when a later synchronous step fails

Communication:

- Order -> Cart Service
- Order -> Inventory Service
- Order -> Payment Service
- Order -> Shipping Service

Design patterns:

- State Pattern for Order lifecycle
- Command Pattern for order actions
- Saga / Saga Orchestration concepts for distributed compensation
- Repository Pattern
- DTO Pattern
- Facade/Orchestrator application service

Example flow:

Create Order
  -> Reserve Inventory
  -> Process Payment
  -> Create Shipment

Failure example:

Payment fails
  -> Release Inventory
  -> Cancel Order

Dependencies:

- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-validation
- spring-cloud-starter-openfeign
- com.h2database:h2 (development)
- org.projectlombok:lombok
- spring-boot-starter-actuator
- spring-boot-starter-test
- springdoc-openapi (recommended/optional)
- spring-cloud-starter-circuitbreaker-resilience4j

Important:

- This project intentionally uses synchronous orchestration.
- Do not add Kafka.
- Use timeouts, retries, circuit breakers, and compensation where appropriate.



## Payment Service

Responsibilities:

- Create payment attempt
- Process payment
- Handle success/failure
- Track payment status
- Support idempotent payment requests
- Coordinate with a mock/external payment provider

Design patterns:

- Adapter Pattern
- Template Method
- Strategy Pattern
- Retry / resilience mechanisms
- Repository Pattern
- DTO Pattern

External provider integration should initially be mocked.

Dependencies:

- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-validation
- spring-cloud-starter-openfeign (when downstream integrations are required)
- com.h2database:h2 (development)
- org.projectlombok:lombok
- spring-boot-starter-actuator
- spring-boot-starter-test
- springdoc-openapi (recommended/optional)
- spring-cloud-starter-circuitbreaker-resilience4j



## Transaction Service

Responsibilities:

- Store payment transaction records
- Track transaction state
- Reconcile payment attempts
- Maintain audit-friendly payment history

Design patterns:

- Repository Pattern
- DTO Pattern
- State Pattern
- Adapter/Facade where external reconciliation is introduced

Dependencies:

- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-validation
- com.h2database:h2 (development)
- org.projectlombok:lombok
- spring-boot-starter-actuator
- spring-boot-starter-test
- springdoc-openapi (recommended/optional)




## Shipping Service

Responsibilities:

- Create shipment
- Assign shipping method
- Generate shipment/label information
- Track shipment status
- Integrate with a mock courier provider initially

Design patterns:

- Adapter Pattern
- Proxy Pattern
- Strategy Pattern
- Repository Pattern
- DTO Pattern

Dependencies:

- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-validation
- spring-cloud-starter-openfeign
- com.h2database:h2 (development)
- org.projectlombok:lombok
- spring-boot-starter-actuator
- spring-boot-starter-test
- springdoc-openapi (recommended/optional)
- spring-cloud-starter-circuitbreaker-resilience4j


## Tracking Service

Responsibilities:

- Track shipment status
- Maintain tracking history
- Provide tracking lookup APIs

Design patterns:

- State Pattern
- Repository Pattern
- DTO Pattern
- Observer-style update handling where useful

Dependencies:

- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-validation
- com.h2database:h2 (development)
- org.projectlombok:lombok
- spring-boot-starter-actuator
- spring-boot-starter-test
- springdoc-openapi (recommended/optional)



## Notification Service

Responsibilities:

- Send email notifications
- Provide notification templates
- Track notification status
- Support notification preferences later

Design patterns:

- Observer Pattern
- Strategy Pattern for notification channels
- Template Method
- Adapter Pattern for external providers

Important:

- No Kafka.
- Notification Service will be called synchronously when a notification is required.

Dependencies:

- spring-boot-starter-web
- spring-boot-starter-data-jpa (only if notification history is persisted)
- spring-boot-starter-validation
- spring-boot-starter-mail
- com.h2database:h2 (development)
- org.projectlombok:lombok
- spring-boot-starter-actuator
- spring-boot-starter-test
- springdoc-openapi (recommended/optional)




## Service Discovery / Eureka

Do not add immediately.

Introduce when the number of services makes localhost routing difficult.

Responsibilities:

- Service registration
- Service discovery

Dependencies:

Server:

- spring-cloud-starter-netflix-eureka-server

Clients:

- spring-cloud-starter-netflix-eureka-client
- 

## Config Server

Do not add immediately.

Introduce when configuration is duplicated across many services.

Responsibilities:

- Centralized configuration
- Environment-specific configuration

Dependencies:

Server:

- spring-cloud-config-server

Clients:

- spring-cloud-starter-config


## Cross-Cutting Dependencies

### Actuator

Use across services as the platform grows.

Dependency:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Purpose:

- Health
- Readiness
- Basic application metrics
- Operational visibility

### OpenAPI / Swagger

Recommended for REST services.

Use the Springdoc OpenAPI starter compatible with the project's Spring Boot version.

Purpose:

- API documentation
- Interactive API testing
- Contract visibility

### Testing

Every service should eventually have meaningful tests.

Base dependency:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

For secured services:

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

Testing expectations:

- Service unit tests
- Controller tests
- Repository tests where useful
- Integration tests
- Remote communication tests where important
- Negative/error-path tests

Do not consider `contextLoads()` alone sufficient.

## 6. Resilience Policy

This project intentionally avoids Kafka.

Use synchronous REST/OpenFeign.

When a service calls another service for an important workflow, consider:

- Connection timeout
- Read timeout
- Retry only when safe
- Circuit breaker
- Fallback
- Bulkhead
- Idempotency

Primary technology:

- Spring Cloud CircuitBreaker with Resilience4j

Do not add Resilience4j everywhere automatically. Add it to services with meaningful remote-call failure scenarios.

## 7. Database Strategy

Current learning setup:

- H2 per microservice

Example:

- product-db
- category-db
- inventory-db
- user-db
- role-db
- etc.

Each microservice has its own database.

Later, PostgreSQL can replace H2:

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

Use Spring Profiles when moving between development and production-style databases.

## 8. Technologies Intentionally Not Used

### Kafka

Do NOT introduce Kafka.

This project intentionally uses synchronous communication and orchestration.

Do not add:

- spring-kafka
- Kafka producers
- Kafka consumers
- Kafka topics
- event-driven infrastructure

unless the project direction is explicitly changed later.

### RabbitMQ

Also not required.

### Kubernetes

Not part of the current learning scope. Add only as a separate future infrastructure phase.

## 9. Docker

Docker is intentionally deferred.

When Docker is introduced, containerize:

- API Gateway
- All business microservices
- Supporting infrastructure such as PostgreSQL/Redis/Eureka/Config Server if those are introduced

Docker itself does not require a Maven dependency.

## 10. Optional Redis

Redis is optional and should only be added when there is a concrete use case.

Potential uses:

- Cart caching
- Product/category caching
- Rate limiting
- Temporary data
- Performance improvements

Dependency when actually required:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

Do not add Redis to every service.

## 11. Final Technology Direction

The final learning stack is intentionally centered on:

- Java 17
- Spring Boot 3.2.5
- Spring Cloud 2023.0.3 where needed
- Spring Web
- Spring Data JPA
- Spring Validation
- Spring Security
- Spring Cloud Gateway
- OpenFeign
- H2 initially
- PostgreSQL later
- JJWT
- Resilience4j
- Actuator
- Springdoc OpenAPI
- JUnit / Mockito through Spring Boot Test
- Lombok
- Docker later
- Eureka later
- Config Server later
- Redis only where justified

Not part of the core project:

- Kafka
- RabbitMQ
- Kubernetes

## 12. Development Philosophy

This is a learning project, not a framework showcase.

When implementing a feature:

1. Understand the business responsibility.
2. Check whether the behavior belongs in the current service.
3. Define the API contract.
4. Choose the simplest appropriate design.
5. Add a design pattern only when it improves the solution.
6. Implement validation and error handling.
7. Add meaningful tests.
8. Consider downstream failure for remote calls.
9. Update documentation.
10. Run build/tests before considering the task complete.

The goal is to learn why an architecture works, not merely to accumulate microservices or dependencies.
