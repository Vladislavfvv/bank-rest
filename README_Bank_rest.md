# 🏦 Bank Card Management System

## 📋 Project Description

Backend application built with Java (Spring Boot) for managing bank cards with full functionality for creating, managing cards, and executing transfers between user cards.

## 🎯 Key Features

### 💳 Card Management
- **Card Creation** - administrators can create new cards for users
- **Card Viewing** - users see their cards, administrators see all cards
- **Block/Activate** - card status management
- **Transfers** - execute transfers between cards with balance verification

### 🔐 Security
- **JWT Authentication** - secure user authorization
- **Role-based Model** - access rights separation (ADMIN/USER)
- **Data Encryption** - card numbers and CVV encrypted in database
- **Masking** - card numbers displayed as `**** **** **** 1234`

## 🏗️ System Architecture

### 📊 Card Attributes
- **Card Number** - encrypted in DB, displayed with masking
- **Holder** - cardholder name (generated from user name)
- **Expiration Date** - card expiration date
- **Status** - ACTIVE, BLOCKED, EXPIRED
- **Balance** - current card balance
- **CVV** - encrypted security code

### 👥 User Roles

#### 🔧 Administrator (ADMIN)
- ✅ Create, block, activate, delete cards
- ✅ User management (create, update, delete)
- ✅ View all cards in the system
- ✅ Process card blocking requests
- ✅ View transfer statistics
- ✅ Access to administrative functions

#### 👤 User (USER)
- ✅ View own cards with filtering and pagination
- ✅ Request blocking of own cards
- ✅ Execute transfers between own cards
- ✅ View card balances
- ✅ View transfer history
- ✅ Update profile

## 🛠️ Technology Stack

### Backend
- **Java 17+** - main development language
- **Spring Boot 3.x** - application framework
- **Spring Security** - security and authentication
- **Spring Data JPA** - database operations
- **JWT** - authentication tokens

### Database
- **PostgreSQL** - main database
- **Liquibase** - database migrations
- **HikariCP** - connection pool

### Documentation and Testing
- **OpenAPI/Swagger** - API documentation
- **JUnit 5** - unit testing
- **Mockito** - mocking for tests

### Deployment
- **Docker** - containerization
- **Docker Compose** - service orchestration

## 🔧 Main Components

### 📡 API Endpoints

#### Authentication
- `POST /api/v1/auth/register` - user registration
- `POST /api/v1/auth/login` - system login
- `POST /api/v1/auth/refresh` - token refresh

#### User Management
- `GET /api/v1/users/self` - get current user data
- `PUT /api/v1/users/me` - update profile
- `GET /api/v1/users` - user list (admin)
- `DELETE /api/v1/users/{id}` - delete user (admin)

#### Card Management
- `GET /api/v1/cards` - card list with filtering
- `GET /api/v1/cards/{id}` - get card by ID
- `GET /api/v1/cards/{id}/cvv` - get CVV for verification
- `POST /api/v1/cards/admin/create-for-user/{userId}` - create card (admin)
- `PUT /api/v1/cards/admin/{id}/block` - block card (admin)
- `PUT /api/v1/cards/admin/{id}/activate` - activate card (admin)
- `POST /api/v1/cards/{id}/request-block` - request blocking (user)

#### Transfers
- `POST /api/v1/transfers` - execute transfer
- `GET /api/v1/transfers/my` - user transfer history
- `GET /api/v1/transfers/card/{cardId}` - transfers by card
- `GET /api/v1/transfers/stats/card/{cardId}` - card statistics (admin)
- `GET /api/v1/transfers/stats/user/{userId}` - user statistics (admin)

### 🗄️ Database Structure

#### users table
- `id` - unique identifier
- `first_name`, `last_name` - first and last name
- `email` - email (unique)
- `password` - hashed password
- `role` - user role
- `birth_date` - birth date
- `phone_number` - phone number
- `created_at` - creation date
- `is_active` - activity status

#### cards table
- `id` - unique identifier
- `number` - encrypted card number
- `holder` - cardholder name
- `expiration_date` - expiration date
- `cvv` - encrypted CVV
- `balance` - balance
- `status` - card status
- `user_id` - user relationship

#### transfers table
- `id` - unique identifier
- `from_card_id` - sender card
- `to_card_id` - recipient card
- `amount` - transfer amount
- `transfer_date` - transfer date
- `status` - transfer status

#### card_block_requests table
- `id` - unique identifier
- `card_id` - card to block
- `user_id` - user requesting blocking
- `reason` - blocking reason
- `status` - request status
- `admin_comment` - administrator comment
- `created_at` - creation date

## 🔒 Security

### Data Encryption
- **AES-256** - encryption of card numbers and CVV
- **BCrypt** - password hashing
- **JWT** - secure access tokens

### Access Control
- **Role-based Authorization** - ADMIN/USER rights separation
- **Ownership Verification** - users can only work with their own data
- **Input Data Validation** - protection against incorrect data

### Data Masking
- **Card Numbers** - displayed as `**** **** **** 1234`
- **CVV** - hidden in API responses
- **Logging** - sensitive data doesn't appear in logs

## 📈 Additional Features

### Automation
- **Task Scheduler** - automatic status update for expired cards
- **Data Validation** - verification of card numbers and CVV correctness
- **Error Handling** - centralized exception handling

### Monitoring and Logging
- **Structured Logging** - detailed operation logs
- **Metrics** - performance tracking
- **Health Checks** - application health monitoring

### Performance
- **Pagination** - efficient handling of large data volumes
- **Caching** - database query optimization
- **Connection Pool** - efficient DB resource utilization

## 🚀 Deployment

### Docker Compose
The application is fully containerized and can be launched with a single command:
```bash
docker-compose up --build
```

### Configuration
- **Spring Profiles** - different settings for dev/prod
- **Environment Variables** - flexible configuration
- **Liquibase Migrations** - automatic DB schema creation

## 📊 Code Quality

### Testing
- **Unit Tests** - coverage of key business logic
- **Integration Tests** - API endpoint testing
- **Mocking** - component isolation during testing

### Architecture
- **Layered Architecture** - clear layer separation
- **SOLID Principles** - quality object-oriented design
- **Clean Code** - readable and maintainable code

### Documentation
- **OpenAPI/Swagger** - interactive API documentation
- **JavaDoc** - code documentation
- **README Files** - system component descriptions

