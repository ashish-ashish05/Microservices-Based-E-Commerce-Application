# 17. Notification Service Specification

## 1. Document Status

- Service: notification-service
- Build Order: 17
- Assignment: Core Infrastructure / Communications
- Status: Proposed
- Scope: Specification only

## 2. Purpose

The Notification Service provides a centralized mechanism for sending communications to users across the e-commerce platform. It decouples the business logic of other microservices from the technical details of notification delivery (e.g., email protocols, template management).

## 3. Responsibilities

- Send email notifications (primary channel).
- Provide and manage notification templates.
- Track notification delivery status and history.
- Manage user notification preferences (e.g., opt-in/out of certain notification types).
- Expose APIs for other services to trigger notifications.

## 4. Non-Responsibilities

- **User Profile Management:** Does not own user email addresses or names; it receives this data from the calling service or fetches it.
- **Business Logic for Triggering:** Does not decide *when* an order should trigger an email; it only executes the request to send one.
- **Authentication/Authorization:** Does not manage user logins; it relies on the API Gateway/Auth service for request validation.

## 5. Service Boundary

- **Data Ownership:** Owns notification history, delivery logs, templates, and user communication preferences.
- **Domain Ownership:** Communication delivery and template orchestration.
- **External Dependencies:** Relies on an SMTP server or external mail provider (initially mocked/Spring Boot Mail).
- **Boundary Justification:** Centralizing notifications ensures consistent branding, single-point configuration for mail servers, and a unified audit trail for all outgoing communications.

## 6. Architecture

Client
  |
API Gateway
  |
[Other Services (e.g., Order, User)]
  |
Notification Service
  |
SMTP / Mail Provider

## 7. Service-to-Service Communication

| Consumer | Provider | Method | Endpoint | Purpose | Failure Consideration |
|----------|----------|--------|----------|---------|-----------------------|
| Any Service | Notification Service | POST | `/api/notifications/send` | Trigger a notification | Timeout, Retry, Circuit Breaker |

**Synchronous Communication Details:**
- **Timeouts:** Short connection timeouts to prevent calling services from hanging.
- **Retry:** Safe to retry if a unique `correlationId` is provided to ensure idempotency.
- **Circuit Breaker:** If the mail provider is down, the circuit should open to prevent cascading failures.
- **Fallback:** Log the failure and potentially queue it internally (though this project avoids Kafka, a simple DB-based retry table is acceptable).

## 8. API Specification

### Send Notification

- Method: `POST`
- Path: `/api/notifications/send`
- Purpose: Sends a notification to a specific user.
- Authentication: Service-to-Service (via Gateway)
- Authorization: Internal Services only
- Request body:
```json
{
  "userId": "uuid",
  "recipientEmail": "user@example.com",
  "templateId": "ORDER_CONFIRMATION",
  "templateParams": {
    "userName": "Ashish",
    "orderId": "ORD-123"
  },
  "correlationId": "uuid"
}
```
- Success response: `202 Accepted` (Notification queued/sent)
- Error responses:
    - `400 Bad Request`: Invalid parameters or template not found.
    - `500 Internal Server Error`: Mail server unavailable.

### Get Notification History

- Method: `GET`
- Path: `/api/notifications/history/{userId}`
- Purpose: Retrieve a list of notifications sent to a user.
- Authentication: JWT (USER/ADMIN)
- Success response: `200 OK` with list of `NotificationResponse` DTOs.

## 9. Data Model

### NotificationLog
| Field | Type | Required | Constraints | Description |
|------|------|----------|-------------|-------------|
| id | UUID | Yes | PK | Unique ID of the log entry |
| userId | UUID | Yes | Index | User who received the notification |
| recipientEmail | String | Yes | Not Null | Email address used |
| templateId | String | Yes | Not Null | Identifier of the template used |
| status | String | Yes | Not Null | SENT, FAILED, PENDING |
| errorMessage | String | No | - | Error details if status is FAILED |
| sentAt | LocalDateTime | Yes | Not Null | Timestamp of attempt |
| correlationId | String | Yes | Unique | ID from the calling service for idempotency |

### NotificationTemplate
| Field | Type | Required | Constraints | Description |
|------|------|----------|-------------|-------------|
| id | String | Yes | PK | e.g., "WELCOME_EMAIL" |
| subject | String | Yes | Not Null | Email subject line |
| body | Text | Yes | Not Null | HTML/Text body with placeholders |
| description | String | No | - | Description of the template's purpose |

## 10. DTOs

### NotificationRequest
- **Purpose:** Request to send a notification.
- **Fields:** `userId`, `recipientEmail`, `templateId`, `templateParams` (Map), `correlationId`.
- **Validation:** Email must be valid; `templateId` and `userId` required.

### NotificationResponse
- **Purpose:** Details of a sent notification.
- **Fields:** `id`, `recipientEmail`, `status`, `sentAt`.

## 11. Business Rules

- A notification must be logged before the attempt to send is made.
- The status must be updated to `FAILED` if the SMTP provider returns an error.
- If a `correlationId` already exists with a `SENT` status, the service should return success without resending (idempotency).
- Templates are read-only for the API and managed via database/configuration.

## 12. Design Patterns

### Pattern: Strategy Pattern
- **Where:** Notification Delivery Logic.
- **Why:** To support multiple channels (Email, SMS, Push) in the future.
- **Problem it solves:** Avoids large if/else blocks when adding new delivery methods.
- **Key participants:** `NotificationStrategy` (Interface), `EmailNotificationStrategy` (Impl).

### Pattern: Template Method
- **Where:** Notification processing flow.
- **Why:** The flow (Log -> Populate Template -> Send -> Update Log) is the same regardless of the channel.
- **Problem it solves:** Ensures consistent logging and processing across different notification types.

## 13. Dependencies

### Required
- `spring-boot-starter-web`: For REST APIs.
- `spring-boot-starter-data-jpa`: For persistence of logs and templates.
- `spring-boot-starter-validation`: For request validation.
- `spring-boot-starter-mail`: For email delivery functionality.
- `com.h2database:h2`: Development database.
- `org.projectlombok:lombok`: To reduce boilerplate.
- `spring-boot-starter-actuator`: For health and monitoring.

### Optional / Later
- `spring-cloud-starter-openfeign`: If the service needs to call User Service to verify emails.

## 14. Configuration

```properties
server.port=8085
spring.application.name=notification-service
# Mail Server Config
spring.mail.host=smtp.example.com
spring.mail.port=587
spring.mail.username=user@example.com
spring.mail.password=${MAIL_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

## 15. Security

- **Authentication:** All `/api/notifications/send` calls must be authenticated via the API Gateway.
- **Authorization:** Only internal microservices (or ADMIN role) should be able to trigger notifications.
- **Sensitive Info:** Mail passwords must be loaded from environment variables, never hard-coded.

## 16. Exception and Error Handling

- `TemplateNotFoundException`: Triggered when `templateId` doesn't exist. HTTP 400.
- `NotificationDeliveryException`: Triggered when the mail server fails. HTTP 500.
- `InvalidRecipientException`: Triggered when email format is incorrect. HTTP 400.

## 17. Transaction Boundaries

- Logging the initial notification attempt and sending the mail should be handled carefully. The log should be committed first to ensure we have a record of the attempt even if the send fails.

## 18. Validation

- **Request Fields:** `recipientEmail` must follow email regex.
- **Business Invariants:** A notification cannot be sent if the template is missing.

## 19. Testing Strategy

### Unit Tests
- Template population logic (placeholder replacement).
- Notification Strategy selection.

### Controller Tests
- Validation of `NotificationRequest`.
- Correct HTTP status codes for success and failure.

### Repository Tests
- Notification log persistence and retrieval.

### Integration Tests
- End-to-end flow from API call to log entry creation.

### Failure Tests
- SMTP server timeout handling.
- Missing template handling.
- Idempotency check using `correlationId`.

## 20. Observability

- **Logs:** Log every sent notification with its `correlationId`.
- **Metrics:** Count of successful vs failed notifications.
- **Health:** Actuator `/health` endpoint to monitor DB and Mail server connectivity.

## 21. Implementation Sequence

1. Create Maven project using existing conventions.
2. Add dependencies (`web`, `jpa`, `validation`, `mail`, `actuator`, `h2`, `lombok`).
3. Implement `NotificationLog` and `NotificationTemplate` entities.
4. Create repositories for both entities.
5. Implement the `NotificationStrategy` interface and `EmailNotificationStrategy`.
6. Implement the `NotificationService` to handle the workflow (Log -> Send -> Update).
7. Create `NotificationRequest` and `NotificationResponse` DTOs.
8. Implement `NotificationController` with the `send` endpoint.
9. Implement global exception handling for notification-specific errors.
10. Configure `application.properties` with mail server settings.
11. Add unit, controller, and integration tests.
12. Run `./mvnw clean test` to verify.

## 22. Acceptance Criteria

- [ ] `/api/notifications/send` endpoint successfully triggers an email.
- [ ] Every attempt is recorded in the `notification_log` table.
- [ ] Notification status is updated to `SENT` or `FAILED` based on the outcome.
- [ ] Providing the same `correlationId` prevents duplicate emails.
- [ ] Placeholders in templates are correctly replaced with `templateParams`.
- [ ] `./mvnw clean test` passes.
- [ ] No Kafka/RabbitMQ introduced.
- [ ] Dependency versions follow `CLAUDE.md`.

## 23. Out of Scope

- Implementation of a GUI for template management.
- Integration with SMS or Push notification providers (handled by Strategy pattern in future).
- Complex retry scheduling (e.g., exponential backoff via a task scheduler) beyond basic synchronous retries.

## 24. Decisions and Open Questions

- **Fixed Decision:** Synchronous REST communication for triggering notifications.
- **Assumption:** The calling service provides the recipient's email address to avoid an extra synchronous call to the User Service.
- **Open Question:** Should the service support asynchronous sending via `@Async` internally to avoid blocking the calling service? (Recommended for implementation).

## 25. Recommended Git Branch

Suggested Branch: `feature/notification-service`
Please switch to this branch before starting the implementation.
