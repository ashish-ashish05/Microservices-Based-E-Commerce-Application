---
name: create-spring-project
description: Create an empty Spring Boot microservice project from a single service-name argument. Read the root CLAUDE.md to determine the service's responsibilities and required dependencies, reuse the repository's existing Maven/package/class naming conventions, and generate only a clean runnable project skeleton.
argument-hint: <project-name>
disable-model-invocation: true
---

# Create Spring Boot Project

Create an empty Spring Boot project for the service name supplied as `$ARGUMENTS`.

## Inputs

The command takes exactly one argument:

```text
/create-spring-project <project-name>
```

Examples:

```text
/create-spring-project role-service
/create-spring-project user-service
/create-spring-project auth-service
/create-spring-project cart-service
/create-spring-project pricing-service
```

If no argument is supplied, stop and ask for the project name.

Do not invent a different name.

## Source of truth

Before creating anything:

1. Read the repository-root `CLAUDE.md`.
2. Read the existing repository structure and at least these existing projects:
   - `product-service`
   - `category-service`
   - `inventory-service`
   - `api-gateway`
3. Use `CLAUDE.md` to determine:
   - service responsibilities
   - intended dependencies
   - whether Spring Cloud is required
   - whether Security is required
   - whether OpenFeign is required
   - whether JPA/Validation/Actuator/Test/etc. are required
4. Use the existing codebase to determine naming/style conventions.
5. Never override a service-specific dependency decision from `CLAUDE.md` merely because another service uses a different stack.

## Naming conventions to preserve

The current codebase uses:

- `groupId`: `com.ecommerce`
- `artifactId`: lowercase kebab-case
- service directory: same as artifactId
- base package: `com.ecommerce.<artifactId with hyphens replaced by underscores>`
- main class: PascalCase service name + `Application`

Examples:

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

api-gateway
-> com.ecommerce.api_gateway
-> ApiGatewayApplication
```

Preserve these conventions for all newly generated services.

## Project creation rules

Create a new independent Maven Spring Boot project.

Do NOT convert the repository into a Maven multi-module project.

Create:

```text
<project-name>/
├── .mvn/
│   └── wrapper/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/ecommerce/<package>/
│   │   │       └── <PascalCaseName>Application.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/
│           └── com/ecommerce/<package>/
│               └── <PascalCaseName>ApplicationTests.java
├── .gitattributes
├── .gitignore
├── mvnw
├── mvnw.cmd
└── pom.xml
```

The project must be empty except for the generated Spring Boot application class and the minimal context-load test.

Do NOT generate:
- controllers
- services
- repositories
- entities
- DTOs
- clients
- business logic
- exception handlers
- design-pattern implementations
- Dockerfiles
- CI pipelines
- database schema/data
- sample APIs

The purpose of this command is only to create the starting project.

## Maven baseline

Use the project's current baseline from `CLAUDE.md`:

- Java: 17
- Spring Boot: 3.2.5
- Spring Cloud: 2023.0.3 when needed

Use the Spring Boot Maven parent.

Use Spring Cloud dependency management only when the generated project needs Spring Cloud dependencies.

Do not add dependency versions for dependencies managed by Spring Boot or Spring Cloud dependency management.

## Maven baseline and strict version policy

Use the project's exact version baseline from the root `CLAUDE.md`.

Current baseline:

- Java: 17
- Spring Boot: 3.2.5
- Spring Cloud: 2023.0.3 when a Spring Cloud component is required

### Non-negotiable version rules

1. The generated project MUST use the Spring Boot version defined in `CLAUDE.md`.
2. The generated project MUST use the Java version defined in `CLAUDE.md`.
3. If Spring Cloud is required, the generated project MUST use the Spring Cloud release train defined in `CLAUDE.md`.
4. Never silently upgrade or downgrade Spring Boot.
5. Never silently upgrade or downgrade Spring Cloud.
6. Never choose a "latest" Spring Boot or Spring Cloud version.
7. Never mix Spring Boot and Spring Cloud release trains.
8. Use the Spring Boot Maven parent for Spring Boot-managed dependency versions.
9. Use Spring Cloud dependency management/BOM when Spring Cloud dependencies are required.
10. Do NOT hard-code versions for dependencies managed by the Spring Boot parent or Spring Cloud BOM.
11. Third-party libraries that are not managed by Spring Boot/Spring Cloud must use a deliberate compatible version and must not conflict with versions already established by the project.
12. If a compatible third-party version cannot be determined confidently, stop and report the ambiguity instead of guessing.
13. Do not update the project's baseline versions as part of this command. Version upgrades are a separate explicit task.

### Required Maven verification

A generated project is NOT considered successful until all applicable checks pass.

Run from the newly created project:

```bash
./mvnw clean test
```

Then:

```bash
./mvnw help:effective-pom
./mvnw dependency:tree
```

Inspect the results for:

- Spring Boot version drift
- Spring Cloud version drift
- duplicate/conflicting dependency versions
- unresolved dependencies
- dependency convergence problems
- incompatible transitive dependencies
- failed test compilation
- failed application context startup

If the dependency tree or effective POM reveals a version conflict, fix the `pom.xml` and rerun the checks.

If Maven cannot verify the dependency graph, do NOT report the project as successfully generated.

## Dependency selection

The dependency list MUST be determined from the service in `CLAUDE.md`.

Map dependencies as follows:

### Common business microservice baseline

Use when the service exposes HTTP APIs and persists data:

```xml
spring-boot-starter-web
spring-boot-starter-data-jpa
spring-boot-starter-validation
```

Add:

```xml
com.h2database:h2
```

for the current development baseline unless `CLAUDE.md` says otherwise.

Add:

```xml
org.projectlombok:lombok
```

as optional.

Add:

```xml
spring-boot-starter-actuator
```

for operational endpoints.

Add:

```xml
spring-boot-starter-test
```

with scope `test`.

### OpenFeign

If the service communicates synchronously with other services:

```xml
spring-cloud-starter-openfeign
```

and import the Spring Cloud BOM.

Do not add Feign to services that do not need remote synchronous communication.

### Security

If `CLAUDE.md` requires security:

```xml
spring-boot-starter-security
```

For security tests:

```xml
spring-security-test
```

with scope `test`.

### JWT Auth Service

If creating `auth-service`, include the JJWT modules specified by `CLAUDE.md`:

- `io.jsonwebtoken:jjwt-api`
- `io.jsonwebtoken:jjwt-impl` with runtime scope
- `io.jsonwebtoken:jjwt-jackson` with runtime scope

Do not hard-code JWT versions in multiple places if the project has a central dependency property. If there is no property, use a current compatible JJWT release only after checking the existing project's dependency policy or the user's explicit version choice.

### API Gateway

If creating `api-gateway`, use:

```xml
spring-cloud-starter-gateway
spring-boot-starter-actuator
spring-boot-starter-test
```

When JWT validation is required by `CLAUDE.md`, add:

```xml
spring-boot-starter-security
spring-security-oauth2-resource-server
spring-security-oauth2-jose
```

Do not add `spring-boot-starter-web` to Spring Cloud Gateway unless the project's chosen Gateway architecture explicitly requires it. Keep Gateway reactive.

### Resilience4j

Only add:

```xml
spring-cloud-starter-circuitbreaker-resilience4j
```

when the service has meaningful remote-call resilience requirements according to `CLAUDE.md`.

Do not add it to every service automatically.

### Redis

Do NOT add Redis by default.

Only add:

```xml
spring-boot-starter-data-redis
```

when `CLAUDE.md` explicitly says the newly created service needs Redis.

### Mail

Only add:

```xml
spring-boot-starter-mail
```

when creating a notification/email service that actually sends email.

### PostgreSQL

Do NOT add PostgreSQL to newly created services if the current project baseline is H2 development.

Use H2 unless the service's current `CLAUDE.md` requirements explicitly call for PostgreSQL.

### Explicitly forbidden by current project decision

Do NOT add:

```text
spring-kafka
rabbitmq
Kafka clients
RabbitMQ clients
```

This project intentionally uses synchronous REST/OpenFeign communication and orchestration.

Do not introduce a message broker.

## Application configuration

Generate a minimal `application.properties` for business services.

The port should be derived from `CLAUDE.md` if one is assigned. If there is no defined port for the new service:

1. Inspect existing service ports.
2. Choose the next unused port.
3. State the chosen port in the final summary.
4. Do not modify unrelated services.

For H2-backed services, use the project's existing convention:

```properties
server.port=<port>
spring.application.name=<artifact-id>
spring.datasource.url=jdbc:h2:mem:<service-db-name>
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

Use `<artifact-id>` as the Spring application name unless `CLAUDE.md` specifies another value.

Use `<artifact-id with hyphens replaced by hyphens>` as the H2 database name, e.g.:

```text
role-service -> role-db
user-service -> user-db
auth-service -> auth-db
```

Do not add remote service URLs until the service actually has clients.

## Main application class

Follow the existing repository style:

```java
@SpringBootApplication
public class <PascalCaseName>Application {

    public static void main(String[] args) {
        SpringApplication.run(<PascalCaseName>Application.class, args);
    }
}
```

If OpenFeign is required, add:

```java
@EnableFeignClients
```

only when this service will actually contain Feign clients.

If the service does not need Feign, do not add `@EnableFeignClients`.

## Test

Generate only a context-load test matching the repository style:

```java
@SpringBootTest
class <PascalCaseName>ApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

Do not generate fake business tests for an empty project.

## Maven/compiler/Lombok style

Match the existing services.

If the existing project uses an explicit Lombok annotation processor configuration for that type of service, preserve that convention.

Do not add unnecessary Maven plugins.

## Validation

After generation:

1. Verify the project structure and naming conventions match the existing repository.
2. Verify `pom.xml` uses the exact Java/Spring Boot/Spring Cloud versions required by `CLAUDE.md`.
3. Verify no dependency managed by Spring Boot or Spring Cloud has an unnecessary explicit version.
4. Run:

   ```bash
   ./mvnw clean test
   ```

5. Run:

   ```bash
   ./mvnw help:effective-pom
   ```

6. Run:

   ```bash
   ./mvnw dependency:tree
   ```

7. Verify:
   - the build succeeds
   - tests pass
   - application context loads
   - there are no unresolved dependencies
   - there is no Spring Boot version drift
   - there is no Spring Cloud release-train mismatch
   - there are no obvious dependency conflicts
   - Kafka/RabbitMQ was not accidentally added
   - dependency choices match `CLAUDE.md`
8. If any verification step fails, fix the project and rerun the checks.
9. Do not report success until the verification steps pass.

If Maven cannot complete dependency resolution, clearly report the failure instead of claiming the project is ready.

## Safety / scope

- Never overwrite an existing service directory without explicit user instruction.
- If `<project-name>` already exists, stop and report that it already exists.
- Do not modify existing services.
- Do not change root `CLAUDE.md`.
- Do not create implementation code.
- Do not install dependencies globally.
- Do not commit changes.

## Final output

After successful generation, report:

1. Project name
2. Directory created
3. Base package
4. Main application class
5. Port
6. Dependencies included
7. Whether Spring Cloud dependency management was added
8. Build/test result

Keep the response concise.
