# 17. Notification Service Implementation Plan

## 1. Plan Metadata

- Service: Notification Service
- Build Order: 17
- Assignment: Core Infrastructure / Communications
- Specification: .claude/specs/17-notification-service.md
- Status: Planned
- Plan Generated: 2026-10-03
- Prerequisite Services: None

## 2. Objective

The Notification Service will provide a centralized mechanism for sending communications to users across the platform, decoupling business logic from the technical details of email delivery and template management.

## 3. Existing Context

The Notification Service is a provider service, called by other microservices (e.g., Order, User) to trigger user notifications.

## 4. Scope

### In Scope
- Email notification delivery (primary channel).
- Implementation of the Strategy pattern for different delivery channels (Email, SMS, etc.).
- Implementation of the Template Method pattern for the notification processing flow.
- Management of notification templates and user preferences.
- Durable logging of notification history (Sent, Failed, Pending).
- Unit, Controller, and Integration tests.

### Out of Scope
- Decision logic for *when* to notify (handled by calling services).
- User profile management (handled by User Service).
- Complex retry scheduling beyond basic synchronous retries.

## 5. Architecture

Client
  |
API Gateway
  |
[Other Services (e.g., Order, User)]
  |
Notification Service
  |
SMTP / Mail Provider

The Notification Service owns the `NotificationLog` and `NotificationTemplate` entities. It uses a `NotificationStrategy` to send the message through the chosen channel.

## 6. Implementation Dependencies

- **Maven Dependencies**:
    - `spring-boot-starter-web`
    - `spring-boot-starter-data-jpa`
    - `spring-boot-starter-validation`
    - `spring-boot-starter-mail`
    - `com.h2database:h2`
    - `org.projectlombok:lombok`
    - `spring-boot-starter-actuator`
- **Configuration**:
    - `server.port=8085` (Note: check port conflict with Role/Auth services)
    - `spring.mail.host=smtp.example.com`
    - `spring.mail.port=587`
    - `spring.mail.username=user@example.com`
    - `spring.mail.password=${MAIL_PASSWORD}`

## 7. Data Model Plan

| Entity | Field | Type | Constraint | Notes |
|--------|-------|------|------------|-------|
| NotificationLog | id | UUID | Primary Key | Internal ID |
| NotificationLog | userId | UUID | Indexed | User who received the notification |
| NotificationLog | recipientEmail | String | Not Null | Email used |
| NotificationLog | templateId | String | Not Null | Reference to template |
| NotificationLog | status | String | Not Null | SENT, FAILED, PENDING |
| NotificationLog | errorMessage | String | - | Error details if FAILED |
| NotificationLog | sentAt | LocalDateTime | Not Null | Timestamp |
| NotificationLog | correlationId | String | Unique | ID from calling service |
| NotificationTemplate | id | String | Primary Key | e.g., "WELCOME_EMAIL" |
| NotificationTemplate | subject | String | Not Null | Email subject |
| NotificationTemplate | body | Text | Not Null | Body with placeholders |
| NotificationTemplate | description | String | - | Template purpose |

## 8. API Implementation Plan

### Send Notification
- HTTP method: `POST`
- path: `/api/notifications/send`
- request DTO: `NotificationRequest` (userId, recipientEmail, templateId, templateParams, correlationId)
- validation: `recipientEmail` (@Email), `templateId` (@NotBlank)
- response DTO: `NotificationResponse`
- status codes: `202 Accepted`, `400 Bad Request`, `500 Internal Server Error`
- authentication/authorization: Internal Service only
- downstream calls: None (Calls SMTP provider)
- error behavior: Return 400 if template is missing; 500 if mail server fails.

### Get Notification History
- HTTP method: `GET`
- path: `/api/notifications/history/{userId}`
- request DTO: N/A
- validation: UUID format for `{userId}`
- response DTO: `List<NotificationResponse>`
- status codes: `200 OK`, `404 Not Found`
- authentication/authorization: USER/ADMIN
- downstream calls: None
- error behavior: Return empty list if no history exists.

## 9. Service Layer Plan

1. **Send Notification Workflow (Template Method)**:
    - **Step 1: Log Attempt**: Create `NotificationLog` entry with status `PENDING`.
    - **Step 2: Populate Template**: Use `NotificationTemplate` to replace placeholders in the body and subject.
    - **Step 3: Send via Strategy**: Select `NotificationStrategy` (e.g., `EmailNotificationStrategy`) and execute the send.
    - **Step 4: Update Log**: Update status to `SENT` or `FAILED` based on the outcome.
2. **Retrieve History**:
    - Retrieve all `NotificationLog` entries for the `userId`.
    - Map to `NotificationResponse`.

## 10. Design Pattern Implementation Plan

### Strategy Pattern
- Where: `NotificationStrategy` (Interface) and `EmailNotificationStrategy` (Implementation).
- Why: To support multiple delivery channels (Email, SMS, etc.) in the future.
- Main participants: `NotificationStrategy`, `EmailNotificationStrategy`.

### Template Method
- Where: `NotificationService` processing flow.
- Why: The sequence (Log $\rightarrow$ Populate $\rightarrow$ Send $\rightarrow$ Update) is the same for all channels.
- Main participants: `NotificationService` (base class/method), `EmailNotificationStrategy` (concrete implementation).

## 11. Remote Communication Plan

No remote calls are made to other microservices.

- **Timeout**: Strict connection timeouts for SMTP calls.
- **Retry**: Idempotent retries allowed if `correlationId` is provided.
- **Circuit Breaker**: Resilience4j used for the SMTP provider integration.

## 12. Exception Handling Plan

- **TemplateNotFoundException**: 400 Bad Request.
- **NotificationDeliveryException**: 500 Internal Server Error.
- **InvalidRecipientException**: 400 Bad Request.
- **Global Exception Handler**: Standardized JSON response.

## 13. Security Plan

- **Authentication**: All `/api/notifications/send` calls must be authenticated via API Gateway.
- **Authorization**: Only internal microservices (or ADMIN role) should be able to trigger notifications.
- **Sensitive Data**: Mail passwords must be loaded from environment variables.

## 14. Testing Plan

### Unit Tests
- Template population logic (placeholder replacement).
- Notification Strategy selection.

### Controller Tests
- Validate `NotificationRequest` and response status codes.

### Repository Tests
- Notification log persistence and retrieval.

### Integration Tests
- End-to-end flow from API call to log entry creation and mail sending (mocked).

### Failure Tests
- SMTP server timeout handling.
- Missing template handling.
- Idempotency check using `correlationId`.

## 15. Files to Create

```text
src/main/java/com/ecommerce/notification_service/
├── NotificationServiceApplication.java
├── controller/NotificationController.java
├── service/NotificationService.java
├── service/NotificationServiceImpl.java
├── repository/NotificationLogRepository.java
├── repository/NotificationTemplateRepository.java
├── entity/NotificationLog.java
├── entity/NotificationTemplate.java
├── dto/NotificationRequest.java
├── dto/NotificationResponse.java
├── strategy/NotificationStrategy.java
├── strategy/EmailNotificationStrategy.java
└── exception/TemplateNotFoundException.java
└── exception/NotificationDeliveryException.java
└── exception/GlobalExceptionHandler.java
```

## 16. Files to Modify

None.

## 17. Implementation Sequence

1. Create Maven project and add dependencies (`web`, `jpa`, `validation`, `mail`, `actuator`, `h2`, `lombok`).
2. Implement `NotificationLog` and `NotificationTemplate` entities.
3. Create repositories for both.
4. Implement `NotificationStrategy` and `EmailNotificationStrategy`.
5. Implement `NotificationService` to handle the workflow (Log $\rightarrow$ Send $\rightarrow$ Update).
6. Create `NotificationRequest` and `NotificationResponse` DTOs.
7. Implement `NotificationController` with the `send` endpoint.
8. Implement global exception handling.
9. Configure `application.properties` with mail server settings.
10. Add unit, controller, and integration tests.
11. Run `./mvnw clean test` to verify.

## 18. Verification Plan

- Run `./mvnw clean test`.
- Run `./mvnw help:effective-pom`.

## 19. Risks and Failure Scenarios

- **Mail Server Down**: Handled by circuit breaker and 500 error.
- **Template Mismatch**: Handled by `TemplateNotFoundException`.
- **Idempotency Failure**: If `correlationId` is not provided or duplicated, duplicate emails might be sent.

## 20. Acceptance Criteria

- [ ] `/api/notifications/send` endpoint successfully triggers an email.
- [ ] Every attempt is recorded in the `notification_log` table.
- [ ] Notification status is updated to `SENT` or `FAILED` based on the outcome.
- [ ] Providing the same `correlationId` prevents duplicate emails.
- [ ] Placeholders in templates are correctly replaced with `templateParams`.
- [ ] `./mvnw clean test` passes.
- [ ] No Kafka/RabbitMQ introduced.

## 21. Planning Issues

None.

## 22. Implementation Prompt

> Implement `Notification Service` according to `.claude/specs/17-notification-service.md` and this plan. Read the root `CLAUDE.md`. Follow the implementation sequence and acceptance criteria. Do not introduce Kafka/RabbitMQ. Verify the build and tests before completion.
