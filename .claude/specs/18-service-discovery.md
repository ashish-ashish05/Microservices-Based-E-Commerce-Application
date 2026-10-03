# 18. Service Discovery Specification

## 1. Document Status

- Service: service-discovery
- Build Order: 18
- Assignment: Infrastructure / Service Discovery
- Status: Proposed
- Scope: Specification only

## 2. Purpose

The `service-discovery` service acts as the central registry for all microservices in the e-commerce platform. It allows services to find and communicate with each other without having hard-coded localhost URLs, facilitating scalability and resilience.

## 3. Responsibilities

- Provide a central registry for all microservices to register their network locations.
- Maintain a real-time list of available service instances.
- Provide a lookup mechanism for other services to discover the network location of a target service.
- Monitor service health via heartbeats.

## 4. Non-Responsibilities

- This service must NOT implement business logic for the e-commerce application.
- It must NOT handle authentication or authorization for the platform (that is the role of the Auth Service and API Gateway).
- It must NOT act as a load balancer or proxy (that is the role of the API Gateway).

## 5. Service Boundary

- **Data Ownership:** Owns the service registry (mapping of service IDs to network locations).
- **Domain Ownership:** Infrastructure / Service Discovery domain.
- **External Dependencies:** None.
- **Why the boundary exists:** Service discovery is a cross-cutting infrastructure concern that must be decoupled from business services to ensure that infrastructure changes do not affect business logic.

## 6. Architecture

Client
  |
API Gateway
  |
(Discovery Lookup)
  |
service-discovery (Eureka Server)
  |
All Microservices (Register/Heartbeat)

## 7. Service-to-Service Communication

| Consumer | Provider | Method | Endpoint | Purpose | Failure Consideration |
|----------|----------|--------|----------|---------|-----------------------|
| Any Service | service-discovery | POST | /eureka/apps/{appID} | Service Registration | Timeout if discovery server is unavailable |
| Any Service | service-discovery | PUT | /eureka/apps/{appID} | Heartbeat/Renewal | Use retry with exponential backoff |
| Any Service | service-discovery | GET | /eureka/apps | Lookup service instances | Cache locally in clients |

## 8. API Specification

The service uses the standard Netflix Eureka API.

### Service Registration

- Method: POST
- Path: /eureka/apps/{appID}
- Purpose: Register a service instance.
- Authentication: None (Internal infrastructure).
- Authorization: None.
- Request body: Standard Eureka Instance registration JSON.
- Success response: 204 No Content.
- Error responses: 400 Bad Request, 500 Internal Server Error.

### Service Heartbeat

- Method: PUT
- Path: /eureka/apps/{appID}
- Purpose: Renew the lease for a service instance.
- Success response: 200 OK.
- Error responses: 404 Not Found, 500 Internal Server Error.

### Service Discovery Lookup

- Method: GET
- Path: /eureka/apps
- Purpose: Get a list of all registered services and their locations.
- Success response: 200 OK (JSON list of services).
- Error responses: 500 Internal Server Error.

## 9. Data Model

The registry is maintained in memory by Eureka.

| Field | Type | Required | Constraints | Description |
|------|------|----------|-------------|-------------|
| appID | String | Yes | Unique | The unique identifier for the microservice (e.g., `product-service`). |
| instanceId | String | Yes | Unique | Unique identifier for a specific instance of the service. |
| hostName | String | Yes | - | The network host of the service instance. |
| port | Integer | Yes | 1-65535 | The port the service instance is listening on. |
| status | String | Yes | UP, DOWN, STARTING, OUT_OF_SERVICE | The current health status of the instance. |

## 10. DTOs

Standard Eureka DTOs are used. No custom DTOs are required for this infrastructure service.

## 11. Business Rules

- Services must register themselves upon startup.
- Services must send heartbeats periodically to avoid being evicted from the registry.
- The registry must be evicted of instances that fail to send heartbeats within the lease expiration time.

## 12. Design Patterns

### Pattern: Service Registry Pattern

- Where: core implementation.
- Why: To decouple service locations from consumers.
- Problem it solves: Hard-coded URLs make scaling and updating infrastructure difficult.
- Key participants: Eureka Server, Eureka Client.
- Alternatives considered: Kubernetes DNS (not in current scope).

## 13. Dependencies

### Required
- `spring-cloud-starter-netflix-eureka-server` (The core Eureka server implementation).
- `spring-boot-starter-actuator` (Operational health checks).

### Optional / Later
- `spring-cloud-starter-netflix-eureka-client` (If the server needs to cluster with other servers).

## 14. Configuration

```properties
server.port=8761
spring.application.name=service-discovery
eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false
```

- `server.port`: The port for the Eureka server (standard 8761).
- `spring.application.name`: The application name for the identification.
- `eureka.client.register-with-eureka`: Prevents the Eureka server from registering with itself.
- `eureka.client.fetch-registry`: Prevents the Eureka server from fetching the registry from itself.

## 15. Security

- **Authentication:** This service is for internal infrastructure and currently assumes a trusted network.
- **Authorization:** No specific role-based access control is required for registration/heartbeats.
- **Sensitive Information:** Do not log instance IDs or network locations in plain text in public logs.

## 16. Exception and Error Handling

- **Trigger:** Eureka server is down.
- **HTTP Status:** 503 Service Unavailable.
- **Response Shape:** Standard Spring Boot error response.
- **Handling:** Clients should implement local caching of the registry to continue functioning if the discovery server is temporarily unavailable.

## 17. Transaction Boundaries

Not applicable. The service maintains an in-memory registry and does not use a transactional database.

## 18. Validation

- **Request Validation:** Ensure the `appID` and network location are valid.
- **State Transition:** Ensure the status transitions (e.g., STARTING -> UP) are handled correctly.

## 19. Testing Strategy

### Unit Tests
- Verify registration logic.
- Verify heartbeat renewal.

### Controller Tests
- Test registration and heartbeat endpoints.

### Integration Tests
- Verify the application context loads and the Eureka server starts.
- Test the full registration flow: Register a service -> Heartbeat -> Evict.

### Failure Tests
- Test behavior when a service instance fails to send heartbeats.
- Test behavior when the discovery server is restarted.

## 20. Observability

- **Logs:** Log registration and eviction of services.
- **Health:** Actuator `/health` endpoint.
- **Metrics:** Number of registered instances.

## 21. Implementation Sequence

1. Create Maven project with `spring-cloud-starter-netflix-eureka-server`.
2. Configure `application.properties` with `server.port=8761` and disable self-registration.
3. Add `@EnableEurekaServer` to the main application class.
4. Implement a basic context-load test.
5. Verify the Eureka dashboard is accessible at `http://localhost:8761`.
6. Add Actuator endpoints for health monitoring.
7. Implement integration tests for registration/heartbeat flow.

## 22. Acceptance Criteria

- [ ] The service starts on port 8761.
- [ ] The Eureka dashboard is accessible.
- [ ] A microservice can successfully register itself.
- [ ] A microservice can send heartbeats to remain in the registry.
- [ ] Instances that stop sending heartbeats are eventually evicted.
- [ ] No Kafka/RabbitMQ introduced.
- [ ] Dependency versions follow `CLAUDE.md`.

## 23. Out of Scope

- Load balancing implementation (handled by API Gateway / OpenFeign).
- High Availability (HA) clustering of Eureka servers.
- Persistent storage of the registry (standard Eureka is in-memory).

## 24. Decisions and Open Questions

- **Fixed Decisions:** Use Eureka Server for discovery.
- **Assumptions:** Services are running in a trusted internal network.
- **Open Questions:** Whether to introduce a cluster of discovery servers later.

## 25. Recommended Git Branch

Suggested Branch: `feature/service-discovery`
Please switch to this branch before starting implementation.
