---
name: generate-all-plans
description: Read all numbered service specifications in .claude/specs/ order, validate their dependencies and build order, and generate one implementation plan per specification under .claude/plans/. Planning only; do not implement code.
argument-hint: "[optional start-spec]"
---

# Generate Plans for All Service Specifications

Use:

```text
/generate-all-plans
```

Optional:

```text
/generate-all-plans 05-role-service
```

The command reads the service specifications in `.claude/specs/` in numeric sequence and creates a corresponding plan file under `.claude/plans/`.

Example:

```text
.claude/specs/
├── 05-role-service.md
├── 06-user-service.md
├── 07-auth-service.md
├── 08-pricing-service.md
└── ...

.claude/plans/
├── 05-role-service.md
├── 06-user-service.md
├── 07-auth-service.md
├── 08-pricing-service.md
└── ...
```

## Critical planning rule

This command is **planning-only**.

Do not:

- modify Java source code
- modify `pom.xml`
- modify application configuration
- create service implementation files
- modify tests
- modify existing services
- create Git commits
- stage files

The only files this workflow may create or update are:

```text
.claude/plans/*.md
```

Create `.claude/plans/` when necessary.

## Plan Mode

This workflow is intended to run using Claude Code's native Plan Mode.

Before planning the first specification, use Claude Code's native Plan Mode behavior so the session remains read-only with respect to application source code and implementation work.

The current Claude Code CLI provides `/plan <description>` as the native way to enter Plan Mode. A custom Skill itself is a workflow prompt, not a separate permission mode. Therefore, when this Skill is invoked from a session that is not already in Plan Mode:

1. Instruct the user to invoke the native `/plan` mode if the current CLI cannot switch modes programmatically.
2. Do not use shell commands, prompt tricks, or implementation tools to bypass Plan Mode.
3. Continue only with planning-only work and only write the allowed `.claude/plans/*.md` artifacts when the session's permissions allow those files to be written.

Do not attempt to implement services as a substitute for Plan Mode.

## 1. Read project context

Before processing specifications:

1. Read root `CLAUDE.md`.
2. Read the build order.
3. Read the current architecture rules.
4. Read the dependency policy.
5. Read all available files in `.claude/specs/` in numeric order.
6. Inspect only the existing services that are relevant to understanding prerequisites, contracts, and conventions.

Do not scan the entire repository unnecessarily.

## 2. Discover specifications

List:

```text
.claude/specs/*.md
```

Sort by the numeric prefix in ascending order.

Example:

```text
01-product-service.md
02-category-service.md
03-inventory-service.md
04-api-gateway.md
05-role-service.md
06-user-service.md
07-auth-service.md
...
```

The numeric prefix is the authoritative execution order.

Do not rely on filesystem creation time or alphabetical order alone.

## 3. Validate specifications before planning

For every specification:

- [ ] File follows `<NN>-<service-name>.md`
- [ ] Number matches the build order in `CLAUDE.md`
- [ ] Service exists in `CLAUDE.md`
- [ ] Assignment is identified
- [ ] Dependencies are documented
- [ ] Prerequisite services are identifiable
- [ ] API contract is defined
- [ ] Data model is defined
- [ ] Design patterns are justified
- [ ] Dependency list is defined
- [ ] Testing expectations are defined
- [ ] No Kafka/RabbitMQ is introduced
- [ ] Synchronous REST/OpenFeign architecture is respected
- [ ] Version policy matches `CLAUDE.md`

If a specification is invalid, DO NOT invent missing requirements.

Record the issue in that specification's plan file under `## Planning Issues` and continue with later independent specifications where possible.

## 4. Processing order

Process one specification at a time in numeric order.

For each specification:

1. Read the specification completely.
2. Read only the directly relevant prerequisite services.
3. Identify the implementation scope.
4. Identify dependencies that must exist before implementation.
5. Identify API/data contracts that must remain compatible.
6. Identify design patterns to implement.
7. Identify files/classes that will likely be created or modified.
8. Identify tests that must be created.
9. Identify configuration changes.
10. Identify remote-call and failure-handling requirements.
11. Produce the implementation plan.
12. Save the plan immediately to `.claude/plans/<NN>-<service-name>.md`.
13. Move to the next specification.

Do NOT start implementing the next specification while planning.

## 5. Required plan file format

Every generated file MUST use this structure:

```md
# <NN>. <Service Name> Implementation Plan

## 1. Plan Metadata

- Service:
- Build Order:
- Assignment:
- Specification:
- Status: Planned
- Plan Generated:
- Prerequisite Services:

## 2. Objective

State the intended business outcome.

## 3. Existing Context

Summarize only the relevant existing services, contracts, and conventions discovered during planning.

## 4. Scope

### In Scope
- ...

### Out of Scope
- ...

## 5. Architecture

Show the intended interaction:

Client
  |
API Gateway
  |
<Service>
  |
+--> <Dependency Service>
+--> <Dependency Service>

Explain the direction of calls and data ownership.

## 6. Implementation Dependencies

List:

- Maven dependencies
- prerequisite services
- API contracts
- configuration requirements

Use the exact versions/policies from `CLAUDE.md`.

## 7. Data Model Plan

For each entity:

| Entity | Field | Type | Constraint | Notes |
|--------|-------|------|------------|-------|

Include:
- IDs
- indexes
- unique constraints
- status fields
- audit fields
- cross-service references

Do not create cross-service database foreign keys.

## 8. API Implementation Plan

For each endpoint:

- HTTP method
- path
- request DTO
- validation
- response DTO
- status codes
- authentication/authorization
- downstream calls
- error behavior

Do not write controller implementation code.

## 9. Service Layer Plan

Describe the required business operations and their order.

Do not write Java implementation code.

## 10. Design Pattern Implementation Plan

For each required pattern:

### <Pattern>

- Where it will be applied
- Why it is justified
- Main participants/classes
- Expected behavior
- What should NOT be abstracted

Do not add patterns that the specification does not justify.

## 11. Remote Communication Plan

For each synchronous remote call:

| Caller | Provider | Endpoint | Purpose | Failure Handling |
|--------|----------|----------|---------|------------------|

Document:
- timeout expectations
- retry safety
- circuit breaker requirements
- fallback behavior
- idempotency

No Kafka/RabbitMQ.

## 12. Exception Handling Plan

Document:

- domain exceptions
- validation failures
- conflict cases
- not-found cases
- downstream failures
- global exception handler behavior

## 13. Security Plan

When applicable:

- authentication
- authorization
- password handling
- JWT expectations
- sensitive fields
- secret handling

## 14. Testing Plan

### Unit Tests
- ...

### Controller Tests
- ...

### Repository Tests
- ...

### Integration Tests
- ...

### Remote/Failure Tests
- ...

### Security Tests
- ...

## 15. Files to Create

List expected files and their responsibilities.

Example:

```text
src/main/java/com/ecommerce/role_service/
├── controller/RoleController.java
├── service/RoleService.java
├── service/RoleServiceImpl.java
├── repository/RoleRepository.java
├── entity/Role.java
├── dto/RoleRequest.java
├── dto/RoleResponse.java
└── exception/...
```

Do not create these files during this command.

## 16. Files to Modify

List existing files that implementation will need to change.

For each file explain why.

Do not modify them during this command.

## 17. Implementation Sequence

Give a precise ordered sequence, for example:

1. Create/update Maven project using the approved dependency set.
2. Add entity/model.
3. Add repository.
4. Add DTOs and validation.
5. Add service interface.
6. Implement business rules.
7. Implement design pattern components.
8. Add Feign clients.
9. Add controllers.
10. Add exception handling.
11. Add security configuration if required.
12. Add tests.
13. Verify API contracts.
14. Run Maven verification.

## 18. Verification Plan

Implementation must eventually run:

```bash
./mvnw clean test
./mvnw help:effective-pom
./mvnw dependency:tree
```

For services with integration requirements, also identify the appropriate integration checks.

## 19. Risks and Failure Scenarios

List important risks such as:

- downstream service unavailable
- invalid state
- duplicate requests
- incompatible API changes
- transaction inconsistency
- security mistakes
- dependency/version conflicts

## 20. Acceptance Criteria

Use a checklist:

- [ ] Required functionality implemented
- [ ] API contract satisfied
- [ ] Validation implemented
- [ ] Exception handling implemented
- [ ] Design patterns implemented and justified
- [ ] Remote communication implemented correctly
- [ ] Security implemented where required
- [ ] Tests added
- [ ] Maven verification passes
- [ ] No Kafka/RabbitMQ introduced
- [ ] No version drift
- [ ] Documentation/contracts updated

## 21. Planning Issues

Record any unresolved issue, ambiguity, or prerequisite problem discovered during planning.

Do not invent a resolution unless it follows from `CLAUDE.md`, the specification, or the existing codebase.

## 22. Implementation Prompt

At the end, produce a compact prompt that can be pasted into a new Claude Code implementation session:

> Implement `<service>` according to `.claude/specs/<spec>.md` and this plan. Read the root `CLAUDE.md`. Inspect only the prerequisite services named by the plan. Follow the implementation sequence and acceptance criteria. Do not introduce Kafka/RabbitMQ or unrelated infrastructure. Verify the build and tests before completion.

Do not execute this prompt.
```

## 6. Existing completed services are reference implementations

Use the actual existing repository as the coding-style reference.

Current services:

- Product Service
- Category Service
- Inventory Service
- API Gateway

Preserve conventions such as:

- `com.ecommerce.<service_name>` packages
- `<PascalCaseServiceName>Application`
- controller/service/repository/entity/dto organization
- constructor injection
- JPA repositories
- UUID identifiers
- H2 development configuration
- synchronous OpenFeign calls
- explicit response mapping

Do not blindly copy implementation details where the new service has different business requirements.

## 7. Cross-specification dependency handling

When planning later specifications, use plans already generated for prerequisite services.

Example:

```text
05-role-service
       ↓
06-user-service
       ↓
07-auth-service
```

The User Service plan must consider the Role Service contract.

The Auth Service plan must consider both User Service and Role Service contracts.

Do not repeat full earlier plans inside later files. Summarize only the prerequisite contract needed by the current plan.

## 8. Existing plan handling

If `.claude/plans/<NN>-<service-name>.md` already exists:

- read it first
- compare it with the current specification
- update it only if the current specification or `CLAUDE.md` has changed
- preserve valuable decisions
- do not blindly overwrite stable planning decisions

If nothing changed, leave the existing plan untouched and report it as already current.

## 9. Optional start argument

If an argument is supplied, for example:

```text
/generate-all-plans 07-auth-service
```

still read and validate all specification files, but generate/update plans starting from that specification.

Do not regenerate earlier plans unless they are missing or stale.

## 10. Completion criteria

The command is complete when:

- all discovered specifications have been processed in sequence, or a clear planning issue prevents a particular plan
- each applicable `.claude/plans/<NN>-<service-name>.md` exists
- every plan references its specification
- build-order dependencies are respected
- dependency/version rules match `CLAUDE.md`
- no implementation files were changed
- no Kafka/RabbitMQ was introduced
- plans are specific enough for later Claude Code implementation sessions

## Final response

Return:

```text
Generated/updated plans:
- .claude/plans/05-role-service.md
- .claude/plans/06-user-service.md
- ...

Skipped/already current:
- ...

Planning issues:
- ...

Mode:
Planning only — no service implementation performed.
```

Keep the final response concise.
