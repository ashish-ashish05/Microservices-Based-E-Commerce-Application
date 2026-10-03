# Microservices-Based E-Commerce Application

A learning-focused, distributed e-commerce platform built with Java, Spring Boot, and Spring Cloud. This project demonstrates the practical application of microservices architecture, design patterns, and distributed system resilience.

## 🚀 Architecture Overview

The system is composed of multiple decoupled microservices, each owning its own data and communicating via synchronous REST APIs (OpenFeign).

### Core Services
- **API Gateway**: Single entry point for all clients, handling routing and JWT-based security.
- **Product Service**: Manages product catalogs and aggregates data from Category and Inventory services.
- **Category Service**: Handles hierarchical product categories using the Composite pattern.
- **Inventory Service**: Manages stock levels and availability with a Strategy pattern for stock management.
- **User & Role Services**: Manage user profiles, authentication metadata, and RBAC (Role-Based Access Control).
- **Auth Service**: Centralized authentication provider issuing and validating JWT tokens.
- **Cart Service**: Manages user shopping carts and coordinates with Product and Pricing services.
- **Order Service**: Orchestrates the order lifecycle (creation, payment, shipment) using a Saga-like synchronous orchestration pattern.
- **Payment & Transaction Services**: Handle payment processing and audit trails for financial transactions.
- **Shipping & Tracking Services**: Manage logistics, courier integration, and real-time shipment tracking.
- **Notification Service**: Centralized system for sending user alerts via various channels.
- **Pricing & Coupon Services**: Calculate dynamic pricing and validate discount coupons.

### Infrastructure
- **Service Discovery (Eureka)**: Enables services to find each other dynamically without hardcoded URLs.
- **Config Server**: Centralized management of application configurations across environments.
- **Databases**: Each service uses its own H2 (development) or PostgreSQL (production) instance to ensure data sovereignty.

## 🛠️ Tech Stack

- **Language**: Java 17
- **Framework**: Spring Boot 3.2.5
- **Cloud Stack**: Spring Cloud 2023.0.3 (Gateway, OpenFeign, Eureka, Config Server)
- **Security**: Spring Security, JJWT (JSON Web Tokens)
- **Persistence**: Spring Data JPA, H2 / PostgreSQL
- **Resilience**: Resilience4j (Circuit Breaker, Retry, Timeout)
- **Documentation**: Springdoc OpenAPI (Swagger)
- **Build Tool**: Maven

## 🎨 Design Patterns Implemented

This project serves as a living catalog of software design patterns:
- **Structural**: API Gateway, Composite (Category), Facade (Aggregation services), Adapter (Payment/Shipping providers).
- **Behavioral**: Strategy (Inventory/Pricing), State (Order/Shipment lifecycle), Chain of Responsibility (Pricing/Coupons), Observer (Notifications).
- **Creational**: Factory, Builder.
- **Architectural**: Database-per-Service, DTO Pattern, Repository Pattern, Saga Orchestration (Synchronous).

## 🚦 Getting Started

### Prerequisites
- JDK 17
- Maven 3.8+

### Running the Application
1. **Service Registry**: Start `service-discovery` first.
2. **Config Server**: Start `config-server` (if applicable).
3. **Business Services**: Start the required microservices (e.g., `category-service` $\rightarrow$ `product-service` $\rightarrow$ `inventory-service`).
4. **Gateway**: Start `api-gateway` to access the system via port `8080`.

### API Documentation
Once a service is running, you can access its Swagger UI at:
`http://localhost:<port>/swagger-ui.html`

## 📝 Project Goals
- Understand microservice boundaries and data ownership.
- Implement synchronous service-to-service communication.
- Apply resilience patterns to handle distributed failures.
- Learn the transition from a monolithic mindset to a distributed architecture.
