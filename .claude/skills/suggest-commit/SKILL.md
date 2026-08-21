---
name: suggest-commit
description: Analyze the current Git working tree and staged/unstaged changes, infer the intent of the changes, and suggest a concise Conventional Commit message without modifying files or committing anything.
argument-hint: "[optional context]"
disable-model-invocation: true
---

# Suggest Commit Message

Use:

```text
/suggest-commit
```

Optional context is allowed:

```text
/suggest-commit completed Role Service
```

The purpose of this command is to inspect the actual repository changes and suggest an accurate commit message.

## Core rule

Never invent a commit message solely from the user's last task description.

Use the actual Git state as the primary source of truth.

---

## 1. Inspect Git state

Run:

```bash
git status --short
git diff --stat
git diff
git diff --cached --stat
git diff --cached
```

If useful, inspect recent commit style:

```bash
git log -10 --oneline
```

Determine:

- staged files
- unstaged files
- untracked files
- files modified/deleted/added
- affected service(s)
- functional vs refactor/config/test/docs changes
- whether the work is a new feature, bug fix, refactor, test, docs, build/dependency, or chore
- whether the change affects one service or the whole system

Do not inspect or print secrets.

---

## 2. Commit message convention

Use Conventional Commits:

```text
type(scope): description
```

Preferred types:

- `feat` — new functionality
- `fix` — bug fix
- `refactor` — code restructuring without changing behavior
- `test` — tests only
- `docs` — documentation only
- `build` — dependencies/build tooling
- `ci` — CI/CD changes
- `chore` — maintenance
- `perf` — performance improvement
- `style` — formatting/style only

Scopes should match the repository where possible:

```text
product-service
category-service
inventory-service
api-gateway
user-service
role-service
auth-service
cart-service
pricing-service
coupon-service
order-service
payment-service
transaction-service
shipping-service
tracking-service
notification-service
gateway
core
```

For cross-service changes use a meaningful system-level scope such as:

```text
core
security
architecture
microservices
```

Prefer the narrowest accurate scope.

---

## 3. Message rules

The suggested subject MUST:

- be concise
- use imperative mood
- start with a lowercase letter after `:`
- avoid a trailing period
- describe the actual change
- avoid vague wording such as:
  - `update code`
  - `changes`
  - `done`
  - `completed`
  - `final`
  - `misc updates`
- avoid mentioning implementation details that are not important
- mention the major architectural change when one exists

Examples:

```text
feat(role-service): add role management APIs
feat(auth-service): implement JWT authentication
fix(product-service): handle missing inventory during product lookup
test(inventory-service): add stock reservation and release tests
refactor(category-service): simplify category tree construction
build(product-service): add OpenFeign and validation dependencies
feat(api-gateway): add JWT authentication for protected routes
```

---

## 4. Analyze the change as a whole

Do not generate one commit message per changed file.

Group the changes into one logical change.

For example:

If the diff contains:

```text
User.java
UserController.java
UserService.java
UserRepository.java
UserResponse.java
```

the message should be something like:

```text
feat(user-service): add user registration and profile APIs
```

not:

```text
feat: add User.java
feat: add UserController.java
...
```

---

## 5. Detect the dominant change

Use this decision order:

### New business capability

Use:

```text
feat(...)
```

### Existing behavior corrected

Use:

```text
fix(...)
```

### Code structure improved without behavior change

Use:

```text
refactor(...)
```

### Tests added/changed only

Use:

```text
test(...)
```

### Dependencies/build configuration

Use:

```text
build(...)
```

### Documentation

Use:

```text
docs(...)
```

### CI/CD

Use:

```text
ci(...)
```

### Maintenance without a functional change

Use:

```text
chore(...)
```

If a change has both code and tests, choose the type based on the primary purpose.

Example:

```text
feat(user-service): add user registration with password hashing
```

rather than:

```text
test(user-service): add registration tests
```

when the implementation itself is new.

---

## 6. Use recent Git history for style

Inspect recent commits and follow the repository's established style when reasonable.

If existing history consistently uses:

```text
feat(product): ...
```

do not suddenly use a completely different format unless the current change requires it.

Do not blindly copy the wording of older commits.

---

## 7. Optional body

Normally return only one subject line.

If the change is broad or architectural, optionally provide a body:

```text
feat(api-gateway): centralize service routing

- add routes for product, category, and inventory services
- expose actuator health endpoints
```

Keep the body short and factual.

Do not generate a body for simple commits unless it adds real value.

---

## 8. Multiple candidate messages

Return:

### Recommended

One best commit message.

### Alternatives

At most two alternatives when there is genuine ambiguity.

Examples:

```text
Recommended:
feat(role-service): add role management APIs

Alternative:
feat(role-service): implement role creation and lookup
```

Do not produce five or ten nearly identical options.

---

## 9. Validate against current changes

Before finalizing:

- confirm the service name from paths
- confirm the change type from the diff
- confirm the message does not claim work that is not present
- confirm the message does not omit a major architectural change
- confirm it follows Conventional Commits
- confirm there is no trailing period

If the working tree has no changes:

Report:

```text
No working-tree changes detected. There is nothing to commit.
```

If only untracked files exist, inspect them before suggesting a message.

---

## 10. Do not modify Git state

This command MUST NOT:

- create commits
- stage files
- unstage files
- amend commits
- reset changes
- delete files
- modify source code

It is suggestion-only.

---

## 11. Final response format

Use:

```text
Recommended:
<commit message>

Why:
<one concise sentence based on the actual diff>

Alternatives:
<optional alternative 1>
<optional alternative 2>
```

If there is only one obvious message, omit Alternatives.

If the user provided optional context, use it only as supporting information after checking the actual Git diff.
