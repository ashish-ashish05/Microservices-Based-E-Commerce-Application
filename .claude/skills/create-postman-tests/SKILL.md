---
name: create-postman-tests
description: Generate an import-ready Postman Collection JSON for one microservice by inspecting CLAUDE.md, the service specification, controllers, DTOs, configuration, and relevant existing services.
argument-hint: "<service-name>"
disable-model-invocation: true
---

# Create Postman Test Collection

Use:

```text
/create-postman-tests <service-name>
```

Examples:

```text
/create-postman-tests role-service
/create-postman-tests user-service
/create-postman-tests auth-service
/create-postman-tests product-service
```

Create:

```text
.claude/postman/<service-name>.postman_collection.json
```

The generated file must be a valid Postman Collection v2.1 JSON that can be imported directly into Postman.

## Source of truth

Before generating:

1. Read root `CLAUDE.md`.
2. Read `.claude/specs/<service-spec>.md` when available.
3. Inspect the actual service controllers, request/response DTOs, entities, service implementation, Feign clients, and application configuration.
4. Inspect only directly related services needed to understand dependencies and request sequencing.
5. Use actual implementation contracts as the authority for paths, methods, fields, and response shapes.

Never invent endpoints or request fields.

## Output rules

Create `.claude/postman/` if needed.

Do not modify:

- Java source
- pom.xml
- application configuration
- tests
- CLAUDE.md
- Git state

If the collection already exists, inspect it and regenerate only when explicitly requested.

## Collection structure

Generate a Postman Collection v2.1 object:

```json
{
  "info": {
    "name": "<Service Name> API Tests",
    "_postman_id": "<stable uuid>",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "variable": [],
  "item": []
}
```

Use collection variables for reusable values.

At minimum define:

```text
baseUrl
```

Derive the default value from `CLAUDE.md` and the service's application configuration.

Examples:

```text
product-service -> http://localhost:8081
category-service -> http://localhost:8082
inventory-service -> http://localhost:8083
api-gateway -> http://localhost:8080
user-service -> http://localhost:8084
```

If needed, define additional variables such as:

```text
productId
categoryId
inventoryProductId
roleId
userId
accessToken
```

## Request generation

For every public/testable endpoint, create a request with:

- correct HTTP method
- `{{baseUrl}}` URL
- path variables
- query parameters
- headers
- request body when required
- useful Postman test assertions

Use:

```text
Content-Type: application/json
```

for JSON request bodies.

For protected endpoints:

```text
Authorization: Bearer {{accessToken}}
```

## JSON request bodies

Build request bodies from the actual DTOs.

Do not invent fields.

Respect validation rules.

Use realistic values instead of generic `"string"` placeholders.

Use `{{$randomUUID}}` for standalone UUIDs when appropriate.

For unique data, use Postman dynamic variables where useful.

Do not include real credentials, passwords, JWT secrets, API keys, or personal data.

## Dependent requests

When one request depends on an ID returned by another request, automatically capture it with a Postman test script.

Example:

```javascript
const json = pm.response.json();
pm.collectionVariables.set("categoryId", json.id);
```

Then use:

```json
{
  "categoryId": "{{categoryId}}"
}
```

For Product creation:

```javascript
const json = pm.response.json();
pm.collectionVariables.set("productId", json.id);
```

Then downstream inventory requests can use:

```json
{
  "productId": "{{productId}}"
}
```

Do not require manual copy/paste of IDs when Postman can capture them automatically.

## Request ordering

Order requests according to their dependencies.

Use names such as:

```text
01 - Create Category
02 - Create Product
03 - Get Product
04 - Get Inventory
05 - Reserve Stock
06 - Get Product After Reservation
```

Use folders where helpful:

```text
01 - Setup
02 - CRUD
03 - Business Operations
04 - Negative Tests
```

## Known project flows

Use the actual implementation to confirm details.

### Product Service

Current architecture includes:

```text
Product
  -> Category Service
  -> Inventory Service
```

Because Product creation automatically creates Inventory with quantity 0, the collection should normally test:

```text
Create Category
-> Create Product
-> Get Product
-> Get Inventory
-> Reserve Stock
-> Verify Product / Inventory
```

Do not assume manual inventory creation is required if the implementation creates it automatically.

### Category Service

Useful flow:

```text
Create Root Category
-> Create Child Category
-> Get Category Tree
```

Capture category IDs automatically.

### Inventory Service

Useful flow:

```text
Create Inventory
-> Get Inventory
-> Reserve Stock
-> Get Inventory
-> Release Stock
-> Get Inventory
```

### User Service

Useful flow:

```text
Register User
-> Get User
-> Get User By Email
```

Never expose or assert plaintext passwords in responses.

### Role Service

Useful flow:

```text
Create Role
-> Get Role
-> List Roles
```

### Auth Service

Useful flow:

```text
Prepare prerequisite user
-> Login
-> Capture JWT
-> Call authenticated endpoint
```

Inspect the actual login response to determine the token field name. Do not assume it is `token`.

## Postman assertions

Add concise assertions based on the actual contract.

Examples:

```javascript
pm.test("Status is 200", function () {
    pm.response.to.have.status(200);
});
```

```javascript
pm.test("Response contains id", function () {
    const json = pm.response.json();
    pm.expect(json.id).to.exist;
});
```

For validation errors:

```javascript
pm.test("Returns bad request", function () {
    pm.response.to.have.status(400);
});
```

Do not create false assertions.

## Negative tests

Where supported by the API, include useful negative cases:

- missing required field
- invalid UUID
- invalid email
- negative quantity
- not found
- duplicate resource
- invalid state transition
- unauthorized request
- forbidden request

Use `{{$randomUUID}}` for IDs that intentionally should not exist.

Only include cases supported by the actual service.

## Authentication

For Auth Service and protected service testing:

Define:

```text
accessToken
```

Capture the actual token response:

```javascript
const json = pm.response.json();
pm.collectionVariables.set("accessToken", json.<actual-token-field>);
```

Protected requests use:

```text
Authorization: Bearer {{accessToken}}
```

Public registration/login requests must not require the token.

## API Gateway

When generating a collection specifically for `api-gateway`, default to gateway URLs:

```text
{{baseUrl}}/products
{{baseUrl}}/categories
{{baseUrl}}/inventory
```

For an individual business-service collection, test the service directly unless the specification explicitly says to test through Gateway.

## Service prerequisites

If the collection requires other services to be running, put the prerequisite information in the collection description.

Example:

```text
Start before running:
- API Gateway
- Product Service
- Category Service
- Inventory Service
```

Keep this description inside the Postman collection JSON.

## Collection variables vs environments

Use collection variables by default.

Do not create a Postman environment file unless explicitly requested.

The imported collection should have usable localhost defaults.

## Validation before completion

After generating the JSON:

1. Parse the JSON to verify syntax.
2. Verify Postman Collection v2.1 structure.
3. Verify every request has a valid method and URL.
4. Verify referenced variables exist.
5. Verify captured IDs are actually used by dependent requests.
6. Verify request bodies match DTO fields.
7. Verify token capture matches the actual response contract.
8. Verify no nonexistent endpoints were created.
9. Verify no secrets are present.

Do not claim that requests were executed unless they were actually executed against running services.

## Final response

Report:

```text
Created:
.claude/postman/<service-name>.postman_collection.json

Requests:
<number>

Variables:
<list>

Dependent flow:
<short summary>

Import into Postman:
1. Open Postman
2. Import the generated JSON file
3. Run the collection
```

Do not dump the entire collection JSON into the chat unless explicitly requested.
