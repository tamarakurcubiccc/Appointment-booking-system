# Appointment Booking System

This project aims to create a microservices application for appointment booking and scheduling. The application architecture encompasses a frontend implemented in React, an API Gateway using Spring Cloud Gateway, a service registry - Eureka, a RabbitMQ message broker, and five microservices developed using Java Spring:

- **User Service**
- **Catalog Service**
- **Reservation Service**
- **Notification Service**
- **Loyalty Service**

## Overview

This project is a comprehensive Appointment Scheduling System designed to streamline the booking and management of appointments between clients and service providers. It consists of five main services: User Service, Catalog Service, Reservation Service, Notification Service and Loyalty Service. These services collectively handle user authentication and authorization, service and availability management, reservation handling, notification dispatch and loyalty rewards. Each service owns its own MySQL database, calls between services go over REST through Eureka, and everything that does not have to be synchronous travels as an event on RabbitMQ.

## Features

### User Service

- **User Types:** Supports multiple user roles (admin, client, provider) with specific privileges and data attributes.
  - **Admin:** All privileges, no additional attributes.
  - **Client:** Books appointments with providers.
  - **Provider:** Owns services and publishes the time slots they are available for.
- **Registration:** Clients and providers can register and receive activation emails via the Notification Service. Registration does not return a token - the account first has to confirm its email address. The admin role cannot be self-assigned, only granted by an existing admin.
- **Activation:** The activation link carries a single-use token valid for 24 hours. Activating the account logs the user in directly, so there is no second step.
- **Login:** Users log in with email and password, receiving a JWT upon successful authentication. An unactivated account and an account disabled by an admin are refused with different messages.
- **Password Reset:** A reset request is answered identically whether or not the address is registered, so the endpoint cannot be used to discover which emails exist. The reset token is single-use and valid for one hour.
- **Profile Management:** Users can retrieve their own profile; admins can list all accounts.
- **Access Control:** Admins can change any user's role and can disable and re-enable user access. An admin cannot disable their own account.
- **Initial Admin:** The first admin account is created at startup from the configured credentials, since the admin role is neither self-assignable nor grantable by anyone else.

### Catalog Service

- **Service Management:** Providers can create and update the services they offer, including duration and price. Deactivation is a soft delete, so existing reservations keep their history.
- **Slot Generation:** Providers can create individual slots or bulk-generate them for a date range, given working hours, service duration and an option to skip weekends. Generation is idempotent - re-running it will not duplicate existing slots.
- **Availability Listing:** Clients can browse slots with filters for service, time window and status. Browsing requires no authentication.
- **Booking:** Slots are marked as booked atomically using a pessimistic database lock, so two concurrent requests for the same slot cannot both succeed - the second one receives a conflict response.
- **Blocking:** Providers can block or unblock a slot without deleting it, and can delete slots that are still free.
- **Ownership Rules:** Only the provider who owns a service can modify it or its slots.

### Reservation Service

- **Booking:** Clients can book available slots, triggering a synchronous call to the Catalog Service that reserves the slot before the reservation is persisted.
- **Compensation:** If anything fails after the slot was taken - persisting the reservation, or redeeming a loyalty reward - a compensating action releases the slot and removes the half-finished reservation.
- **Cancellation:** Both clients and providers can cancel bookings, which releases the slot back into availability and triggers asynchronous notifications.
- **Loyalty Redemption:** A booking can spend an earned reward. The reward is validated before the slot is taken and confirmed after the reservation exists, so one reward can never be spent twice.
- **Availability Check:** Clients can check whether a slot can still be booked before attempting to book it.
- **Booking History:** Clients can list their own reservations and providers can list the reservations booked with them, both paginated.
- **Client Details:** Providers can retrieve the contact details of the client who made a booking, fetched from the User Service over REST.

### Notification Service

- **Notification Types:** Admins can define, update, deactivate and delete notification templates, which include parameters for customization. The parameter list of a template is derived from its text on every save, and a preview endpoint renders a template with test values before any user sees it.
- **Activation Email:** Sent upon user registration via an asynchronous event.
- **Password Reset Email:** Dispatched upon request for password change.
- **Booking Confirmation:** Sent to both client and provider when an appointment is successfully booked, each from its own template.
- **Cancellation Notification:** Sent to both parties upon cancellation, naming who cancelled.
- **Notification History:** Every dispatched notification is stored with its rendered text, so users can read what they were sent and admins can audit it. Editing a template later does not rewrite what was already sent.
- **Retry and Dead-Letter:** Transient failures are retried with exponential backoff; a message that can never succeed is rejected straight onto a dead-letter queue instead of burning retries.
- **Idempotent Consumption:** A redelivered event re-sends a pending notification rather than duplicating one that already went out.

### Loyalty Service

- **Loyalty Rules:** Providers can define loyalty rewards, such as a free appointment after a specified number of bookings, along with how long the reward stays valid. A provider has at most one active rule at a time, and rules are publicly readable so a visitor can see what a provider offers.
- **Appointment Counters:** Maintained purely from reservation events; the service never reads another service's database. Issuing a reward subtracts the threshold instead of resetting the counter, so appointments past the threshold carry over to the next reward.
- **Rewards:** Issued with the rule's description copied in, so a later edit to the rule cannot change what a client was already promised. Clients see their own counters and rewards; providers see which of their clients are close to one.
- **Redemption:** Handled synchronously during booking, so the outcome is known while the reservation is still in progress. Cancelling a booking that used a reward releases the reward back to the client.
- **Idempotent Consumption:** Processed events are recorded in the same transaction as the counter change, so a redelivered message cannot inflate a counter and hand out an unearned reward.

### API Gateway

- **Single Entry Point:** All external traffic is routed through the gateway; the microservices are not exposed directly.
- **Authentication:** The JWT is validated in one place. The gateway strips any identity headers sent by the client and injects the verified user id, email and role for the downstream services.
- **Internal API Protection:** Service-to-service endpoints are blocked at the gateway and are only reachable from inside the container network.
- **Resilience:** Circuit breakers return a fallback response instead of leaving the caller hanging when a service is unavailable.

### Web Client

- **Public Browsing:** The service catalogue and each provider's loyalty offer are visible without logging in.
- **Registration and Activation:** The activation and password-reset links in the emails open the client, which exchanges the token for a session, so no token is ever pasted by hand.
- **Booking:** Picking a service shows its free upcoming slots, and a client holding a reward can spend it in the same step.
- **My Appointments:** Clients see their own bookings and can cancel them; providers see the appointments booked with them.
- **Loyalty:** Progress towards the next reward per provider, and the rewards themselves with their status and expiry.
- **Notifications:** The history of everything the system sent to the current user.
- **Provider Tools:** Create services, bulk-generate slots, define or retire the loyalty rule, and see which clients are close to a reward.
- **Admin Tools:** Revoke and restore user access, and edit the text of any notification template without a redeploy.
- **Access Control:** Hiding a screen by role is only convenience - every rule is enforced again by the gateway and by the service that owns the data.

## Technologies Used

- **Frameworks/Libraries:** Spring Boot 3.3, Spring Cloud Gateway (API Gateway), Eureka (Service Discovery), Spring Cloud LoadBalancer, OpenFeign, Resilience4j, Spring AMQP, Spring Security, Spring Data JPA, jjwt (JWT), springdoc-openapi (Swagger), React 18, TypeScript, Vite, React Router
- **Tools:** Java 21, MySQL 8 (a separate database per service), RabbitMQ (message broker), Mailpit (local SMTP), Docker & Docker Compose, Kubernetes, nginx, Maven, JUnit 5, Mockito, AssertJ
