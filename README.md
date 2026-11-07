# Appointment Booking System

This project aims to create a microservices application for appointment booking and scheduling. The application architecture encompasses an API Gateway using Spring Cloud Gateway, a service registry - Eureka, and three microservices developed using Java Spring:

- **User Service**
- **Catalog Service**
- **Reservation Service**

## Overview

This project is a comprehensive Appointment Scheduling System designed to streamline the booking and management of appointments between clients and service providers. It consists of three main services: User Service, Catalog Service, and Reservation Service. These services collectively handle user authentication and authorization, service and availability management, and reservation handling.

## Features

### User Service

- **User Types:** Supports multiple user roles (admin, client, provider) with specific privileges and data attributes.
  - **Admin:** All privileges, no additional attributes.
  - **Client:** Books appointments with providers.
  - **Provider:** Owns services and publishes the time slots they are available for.
- **Registration:** Clients and providers can register and receive a JWT upon successful registration. The admin role cannot be self-assigned - only an existing admin can grant it.
- **Login:** Users log in with email and password, receiving a JWT upon successful authentication.
- **Profile Management:** Users can retrieve their own profile; admins can list all accounts.
- **Access Control:** Admins can change the role of any user. Disabled accounts are refused at login.

### Catalog Service

- **Service Management:** Providers can create and update the services they offer, including duration and price. Deactivation is a soft delete, so existing reservations keep their history.
- **Slot Generation:** Providers can create individual slots or bulk-generate them for a date range, given working hours, service duration and an option to skip weekends. Generation is idempotent - re-running it will not duplicate existing slots.
- **Availability Listing:** Clients can browse slots with filters for service, time window and status. Browsing requires no authentication.
- **Booking:** Slots are marked as booked atomically using a pessimistic database lock, so two concurrent requests for the same slot cannot both succeed - the second one receives a conflict response.
- **Blocking:** Providers can block or unblock a slot without deleting it, and can delete slots that are still free.
- **Ownership Rules:** Only the provider who owns a service can modify it or its slots.

### Reservation Service

- **Booking:** Clients can book available slots, triggering a synchronous call to the Catalog Service that reserves the slot before the reservation is persisted.
- **Compensation:** If persisting the reservation fails after the slot was taken, a compensating action releases the slot back to available.
- **Cancellation:** Both the client and the provider of a booking can cancel it, which releases the slot back into availability.
- **Availability Check:** Clients can check whether a slot can still be booked before attempting to book it.
- **Booking History:** Clients can list their own reservations and providers can list the reservations booked with them, both paginated.
- **Client Details:** Providers can retrieve the contact details of the client who made a booking, fetched from the User Service over REST.

### API Gateway

- **Single Entry Point:** All external traffic is routed through the gateway; the microservices are not exposed directly.
- **Authentication:** The JWT is validated in one place. The gateway strips any identity headers sent by the client and injects the verified user id, email and role for the downstream services.
- **Internal API Protection:** Service-to-service endpoints are blocked at the gateway and are only reachable from inside the container network.
- **Resilience:** Circuit breakers return a fallback response instead of leaving the caller hanging when a service is unavailable.

## Technologies Used

- **Frameworks/Libraries:** Spring Boot 3.3, Spring Cloud Gateway (API Gateway), Eureka (Service Discovery), Spring Cloud LoadBalancer, OpenFeign, Resilience4j, Spring Security, Spring Data JPA, jjwt (JWT), springdoc-openapi (Swagger)
- **Tools:** Java 21, MySQL 8 (a separate database per service), Docker & Docker Compose, Maven, JUnit 5, Mockito, AssertJ
