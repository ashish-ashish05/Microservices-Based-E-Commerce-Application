---
name: create-service-spec
description: Create a durable implementation specification for one planned microservice. The service is supplied as a numbered argument such as "05-role-service". The Skill validates the service against CLAUDE.md, the project build order, existing services, and current architecture, then writes a plan-mode-ready specification to .claude/specs/<argument>.md.
argument-hint: "<NN-service-name>"
disable-model-invocation: true
---

# Create Service Specification

Create a single implementation specification document for the microservice supplied as `$ARGUMENTS`.

Example:

```text
/create-service-spec 05-role-service
/create-service-spec 06-user-service
/create-service-spec 07-auth-service
```

The input MUST contain:

- a two-digit numeric sequence: `01` through `99`
- a service name using the repository's lowercase kebab-case naming convention

The output file MUST be:

```text
.claude/specs/<argument>.md
```

Example:

```text
/create-service-spec 05-role-service
```

creates:

```text
.claude/specs/05-role-service.md
```

## Primary goal

The generated document is a **design/specification document**, not implementation code.

It must be suitable for use later in Claude Code Plan Mode.

The specification must answer:

- Why does this service exist?
- What does it own?
- What does it not own?
- What APIs will it expose?
- What data will it store?
- Which other services will it call?
- Which design patterns are justified?
- Which dependencies are required?
- What validation and errors are expected?
- How will the service be tested?
- What implementation steps should Claude follow?
- What decisions must remain unchanged during implementation?

Do NOT implement the service when generating the specification.

---

# 1. Required context inspection

Before writing the specification:

1. Read the repository-root `CLAUDE.md`.
2. Read the project's current build order from `CLAUDE.md`.
3. Read the current service list from `CLAUDE.md`.
4. Inspect the actual repository structure.
5. Inspect relevant existing services that this service will depend on or resemble.
6. Inspect existing `pom.xml` files to preserve:
   - Java version
   - Spring Boot version
   - Spring Cloud version when relevant
   - Maven conventions
   - common dependency patterns
7. Inspect relevant controllers, DTOs, entities, repositories, clients, and service classes from dependent services.
8. Read existing `.claude/specs/` documents when they exist and use them for consistency.

Do not guess architecture that contradicts `CLAUDE.md`.

---

# 2. Validate the argument

Parse `$ARGUMENTS` as:

```text
<NN>-<service-name>
```

Examples:

```text
01-product-service
05-role-service
06-user-service
07-auth-service
```

Validation rules:

- The argument must be supplied.
- It must start with exactly two digits.
- The service name must be lowercase kebab-case.
- The sequence number must correspond to the service's position in the build order defined by `CLAUDE.md`.
- If the number and service name do not match the project's build order, STOP and report the mismatch.
- Do not silently rename the requested service.
- Do not create a spec for a service that is not defined in the project's roadmap unless the user explicitly changes the roadmap first.

Example:
If CLAUDE.md says:

```text
5. Role Service
6. User Service
7. Auth Service
```

then:

```text
/create-service-spec 05-role-service
```

is valid, while:

```text
/create-service-spec 05-user-service
```

must be rejected because the number does not match the build order.

---

# 3. Output path and overwrite rules

Create:

```text
.claude/specs/
```

if it does not already exist.

Write:

```text
.claude/specs/<argument>.md
```

If the specification already exists:

- Do NOT overwrite it silently.
- Inspect it first.
- Ask the user whether they want to regenerate/update it, unless the current user instruction explicitly requests regeneration.
- If explicitly regenerating, preserve useful decisions from the existing specification and produce a revised version.

Do not modify:

- source code
- pom.xml
- application.properties/application.yml
- CLAUDE.md
- other services
- tests
- Git configuration

This command is specification-only.

---

# 4. Specification quality rules

The specification must be implementation-ready but technology-appropriate.

It must:

- follow `CLAUDE.md`
- preserve existing architecture decisions
- use synchronous REST/OpenFeign because Kafka is intentionally excluded
- identify dependencies explicitly
- identify remote service calls
- identify data ownership
- identify transaction boundaries
- identify failure scenarios
- identify idempotency requirements where relevant
- identify security requirements where relevant
- identify test requirements
- identify design patterns and explain why each is appropriate
- avoid introducing unnecessary microservices
- avoid unnecessary dependencies
- avoid premature infrastructure

Do not add:

- Kafka
- RabbitMQ
- message brokers
- event-driven architecture

unless `CLAUDE.md` has been explicitly changed later.

Do not add Redis, Eureka, Config Server, Docker, or other infrastructure merely because they exist somewhere in the roadmap. Include them only if this specific service needs them at its current phase.

---

# 5. Required specification structure

Every generated specification MUST use this structure.

```md
# <NN>. <Service Name> Specification

## 1. Document Status

- Service:
- Build Order:
- Assignment:
- Status: Proposed
- Scope: Specification only

## 2. Purpose

Explain why this service exists and the business capability it owns.

## 3. Responsibilities

List the capabilities that belong to this service.

## 4. Non-Responsibilities

Explicitly state what this service must NOT own or implement.

## 5. Service Boundary

Explain:
- data ownership
- domain ownership
- external dependencies
- why the boundary exists

## 6. Architecture

Show the service's position in the system using a simple text diagram.

Example:

Client
  |
API Gateway
  |
Auth Service
  |
User Service
  |
Role Service

## 7. Service-to-Service Communication

For every remote dependency specify:

| Consumer | Provider | Method | Endpoint | Purpose | Failure Consideration |
|----------|----------|--------|----------|---------|-----------------------|

Because this project uses synchronous communication, explicitly document:
- timeouts
- retry considerations
- circuit breaker considerations
- fallback behavior
- idempotency where relevant

## 8. API Specification

For every endpoint document:

### Endpoint Name

- Method:
- Path:
- Purpose:
- Authentication:
- Authorization:
- Request headers:
- Path parameters:
- Query parameters:
- Request body:
- Success response:
- Error responses:

Use JSON examples when useful.

Do NOT write Java controller code.

## 9. Data Model

Describe each entity and its fields.

Example:

| Field | Type | Required | Constraints | Description |
|------|------|----------|-------------|-------------|

Document:
- primary keys
- unique constraints
- indexes
- relationships owned by this service
- audit fields
- status fields

Do not create cross-service database foreign keys.

## 10. DTOs

List request/response DTOs.

For every DTO include:
- purpose
- fields
- validation rules
- fields that must never be exposed

## 11. Business Rules

List precise business rules.

Examples:
- email must be unique
- expired coupon cannot be applied
- inventory cannot go below zero
- cancelled order cannot be shipped

## 12. Design Patterns

For each pattern provide:

### Pattern: <Name>

- Where:
- Why:
- Problem it solves:
- Key participants:
- Alternatives considered:

Only include patterns justified by the service.

Never add a pattern purely to satisfy a checklist.

## 13. Dependencies

List dependencies required by this service.

Separate them into:

### Required
### Optional / Later

For each dependency explain why it is needed.

Respect the exact versions and dependency policy from `CLAUDE.md`.

Do not invent versions.

## 14. Configuration

List expected configuration properties without putting secrets into the document.

Examples:

```properties
server.port=
spring.datasource.url=
service.url=
jwt.issuer=
```

For every configuration property explain its purpose.

## 15. Security

Specify:
- authentication requirements
- authorization requirements
- roles/permissions
- password handling
- JWT expectations
- sensitive information that must not be logged

Never include real secrets.

## 16. Exception and Error Handling

List expected domain/application exceptions.

For each:
- trigger
- HTTP status
- response shape
- whether it should be handled locally or globally

Use the project's global exception handling strategy.

## 17. Transaction Boundaries

Identify which operations need transactions and why.

For distributed workflows explicitly state that local transactions do not span databases.

## 18. Validation

List validation rules for:
- request fields
- state transitions
- business invariants
- cross-service validations

## 19. Testing Strategy

Define:

### Unit Tests
Business rules and patterns.

### Controller Tests
Request validation, status codes, response contracts.

### Repository Tests
Persistence behavior where useful.

### Integration Tests
Application context and service integration.

### Contract / Remote Tests
Important OpenFeign interactions.

### Failure Tests
Timeouts, unavailable downstream services, invalid state transitions, duplicates, etc.

Do NOT write test code in the specification.

## 20. Observability

Define:
- useful logs
- health requirements
- metrics that matter
- correlation/trace information later if introduced

Do not introduce an observability platform unless the roadmap requires it.

## 21. Implementation Sequence

Give an ordered implementation plan for Claude Code Plan Mode.

Example:

1. Create/update Maven project using existing conventions.
2. Add exact required dependencies.
3. Create entity/model.
4. Create repository.
5. Create DTOs.
6. Implement business service.
7. Implement design pattern components.
8. Implement remote clients.
9. Implement controllers.
10. Implement exception handling.
11. Implement configuration.
12. Add tests.
13. Run Maven verification.
14. Update API documentation.

Do not generate implementation code here.

## 22. Acceptance Criteria

Create a checklist that can be used after implementation.

Example:

- [ ] All required endpoints exist.
- [ ] Validation rules are enforced.
- [ ] Database constraints are enforced.
- [ ] Remote calls use OpenFeign.
- [ ] Timeouts/failure behavior is handled.
- [ ] Required design patterns are actually used and justified.
- [ ] No direct access to another service's database.
- [ ] Tests cover success and failure cases.
- [ ] `./mvnw clean test` passes.
- [ ] No Kafka/RabbitMQ was introduced.
- [ ] Dependency versions follow `CLAUDE.md`.

## 23. Out of Scope

Explicitly list what should NOT be implemented in this service/specification.

## 24. Decisions and Open Questions

List:
- architecture decisions already fixed by CLAUDE.md
- assumptions
- questions that must be resolved before implementation
```

---

# 6. Service-specific enrichment

Do not use the same generic content for every service.

For each requested service, adapt the specification from the actual `CLAUDE.md` service definition.

Examples:

### Role Service

Focus on:
- roles
- permissions
- role lookup
- authorization model

Do not add JWT generation.

### User Service

Focus on:
- registration
- user profile
- BCrypt password hashing
- roleId
- lookup required by Auth Service

Do not make User Service generate JWTs.

### Auth Service

Focus on:
- authentication
- User Service / Role Service communication
- password verification
- JWT generation
- JJWT
- token claims
- refresh token strategy only if roadmap explicitly requires it

### Cart Service

Focus on:
- cart ownership
- cart items
- quantities
- product validation
- Pricing Service
- Coupon Service
- synchronous calls
- future Redis only if explicitly justified

### Order Service

Focus on:
- Order + OrderItem + OrderStatus within one service
- State Pattern
- Command Pattern
- synchronous Saga/orchestration
- compensation
- Inventory/Payment/Shipping calls
- idempotency

---

# 7. Specification dependencies must be cross-checked

Before finalizing the document:

1. Check `CLAUDE.md`.
2. Check existing service POMs.
3. Check the specifications of prerequisite services, if they exist.
4. Ensure endpoint names and DTO names do not conflict with existing contracts.
5. Ensure the requested service is in the correct build position.
6. Ensure no unnecessary dependency is introduced.
7. Ensure no Kafka/RabbitMQ appears anywhere in the document.
8. Ensure the service is implementable without modifying unrelated services unless the specification explicitly identifies a contract change.

---

# 8. No code implementation

This command generates a **specification**.

It must not:

- create Java classes
- modify `pom.xml`
- implement endpoints
- run broad refactors
- add dependencies to the service
- add Docker configuration

Only the specification file may be created/updated.

---

# 9. Final response

After creating the specification, report:

```text
Created:
.claude/specs/<argument>.md

Service:
<service>

Build order:
<NN>

Assignment:
<assignment>

Key dependencies:
- ...

Key design patterns:
- ...

Prerequisite services:
- ...

Specification status:
Ready for Claude Code Plan Mode
```

Keep the final response concise.

## 25. Recommended Git Branch

The implementation should be developed on a dedicated Git branch.

Use this naming convention:

```text
feature/<service-name>
feature/role-service
feature/user-service
feature/auth-service
feature/pricing-service
feature/coupon-service
feature/cart-service
feature/order-service

The branch name must:

Use the exact service name from the specification.
Use lowercase kebab-case.
Start with feature/.
Not include spaces or special characters.
```
Suggested Branch
feature/<service-name>
Suggest the branch name at the end of the specification and ask user to switch to this branch
Do not switch automatically. Only Suggest.