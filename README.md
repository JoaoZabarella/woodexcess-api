# 🪵 WoodExcess API

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![CI/CD](https://github.com/JoaoZabarella/woodexcess-api/actions/workflows/ci.yml/badge.svg)](https://github.com/JoaoZabarella/woodexcess-api/actions)

A **micro SaaS platform** that connects woodworkers and store owners, enabling them to list, manage, and trade surplus materials. The goal is to reduce waste, optimize inventory, and promote sustainable practices within the woodworking industry.

---

## 📋 Table of Contents

- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Architecture](#-architecture)
- [Getting Started](#-getting-started)
- [API Endpoints](#-api-endpoints)
- [Testing](#-testing)
- [CI/CD Pipeline](#-cicd-pipeline)
- [Environment Variables](#-environment-variables)
- [Database Migrations](#-database-migrations)
- [Contributing](#-contributing)

---

## ✨ Features

### **Authentication & Authorization**
- ✅ User registration with email validation
- ✅ JWT-based authentication (access + refresh tokens)
- ✅ Role-based access control (USER, ADMIN)
- ✅ Refresh token rotation with security context validation
- ✅ Login rate limiting (protection against brute-force attacks)

### **User Management**
- ✅ Complete user profile management
- ✅ Password strength validation
- ✅ Phone number validation (Brazilian format)
- ✅ User activation/deactivation (soft delete)

### **Address Management** 🆕
- ✅ Multiple addresses per user (up to 5)
- ✅ Automatic address filling via ViaCEP API
- ✅ Primary address designation
- ✅ Address validation (no duplicates)
- ✅ Soft delete for addresses

### **Coming Soon**
- 🔄 Material listing CRUD
- 🔄 Search and filter for listings
- 🔄 Real-time chat between users
- 🔄 Notifications system
- 🔄 User dashboard

---

## 🛠 Tech Stack

### **Core**
- **Java 21** (LTS)
- **Spring Boot 3.3.5**
- **Maven** (dependency management)

### **Frameworks & Libraries**
- **Spring Web** - REST API
- **Spring Data JPA** - ORM with Hibernate
- **Spring Security** - Authentication & Authorization
- **Spring Validation** - Bean validation
- **Spring Cloud OpenFeign** - HTTP client for ViaCEP integration
- **Spring Cache** - Caching support

### **Database**
- **PostgreSQL 16** (production)
- **H2** (testing with Testcontainers)
- **Flyway** - Database migrations

### **Security & Auth**
- **JWT** (JSON Web Tokens)
- **BCrypt** - Password hashing
- **Bucket4j** - Rate limiting

### **Utilities**
- **Lombok** - Reduce boilerplate code
- **MapStruct** - DTO mapping
- **Jackson** - JSON serialization

### **Testing**
- **JUnit 5** - Unit testing
- **Mockito** - Mocking framework
- **Spring Boot Test** - Integration testing
- **Testcontainers** - PostgreSQL containers for tests
- **MockMvc** - REST API testing

### **DevOps & Quality**
- **GitHub Actions** - CI/CD pipeline
- **Qodana** - Code quality analysis by JetBrains
- **JaCoCo** - Code coverage
- **Docker** - Containerization

---

## 🏗 Architecture

src/
├── main/
│ ├── java/com/z/c/woodexcess_api/
│ │ ├── config/ # Configuration classes
│ │ ├── controller/ # REST endpoints
│ │ ├── dto/ # Data Transfer Objects
│ │ ├── exception/ # Custom exceptions & handlers
│ │ ├── mapper/ # MapStruct mappers
│ │ ├── model/ # JPA entities
│ │ ├── repository/ # Spring Data repositories
│ │ ├── security/ # Security filters & configs
│ │ └── service/ # Business logic
│ └── resources/
│ ├── db/migration/ # Flyway migrations
│ └── application.properties
└── test/
├── java/
│ ├── integration/ # Integration tests
│ └── service/ # Unit tests
└── resources/
└── application-test.properties
---

## 🚀 Getting Started

### **Prerequisites**
- Java 21+ ([Download](https://www.oracle.com/java/technologies/downloads/))
- Maven 3.9+ ([Download](https://maven.apache.org/download.cgi))
- Docker Desktop (for Testcontainers) ([Download](https://www.docker.com/products/docker-desktop))
- PostgreSQL 16+ (production) ([Download](https://www.postgresql.org/download/))

### **1. Clone the repository**
git clone https://github.com/JoaoZabarella/woodexcess-api.git
cd woodexcess-api

### **2. Configure database**

**Option A: Docker (recommended for development)**
docker run -d
--name woodexcess-postgres
-e POSTGRES_DB=woodexcess
-e POSTGRES_USER=postgres
-e POSTGRES_PASSWORD=postgres
-p 5432:5432
postgres:16-alpine

**Option B: Local PostgreSQL**

CREATE DATABASE woodexcess;

### **3. Configure environment variables**

Create a `.env` file or set environment variables:

Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/woodexcess
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres

JWT
JWT_SECRET=your-secret-key-min-256-bits-long-replace-this-in-production
JWT_ACCESS_EXPIRATION_MS=900000
JWT_REFRESH_EXPIRATION_MS=604800000

ViaCEP
VIACEP_URL=https://viacep.com.br/ws

### **4. Run the application**

**Using Maven:**

**Using Docker:**

docker build -t woodexcess-api .
docker run -p 8080:8080 woodexcess-api


### **5. Access the API**
- **Base URL:** http://localhost:8080
- **Health Check:** http://localhost:8080/actuator/health

---

## 📍 API Endpoints

### **Authentication**

#### Register

POST /api/users/register
Content-Type: application/json

{
"name": "John Doe",
"email": "john@example.com",
"phone": "11987654321",
"password": "StrongPass123!@#"
}

#### Login
POST /api/auth/login
Content-Type: application/json

{
"email": "john@example.com",
"password": "StrongPass123!@#"
}

**Response:**
{
"accessToken": "eyJhbGciOiJIUzUxMiJ9...",
"refreshToken": "550e8400-e29b-41d4-a716-446655440000",
"type": "Bearer",
"expiresIn": 900000
}

#### Refresh Token
POST /api/auth/refresh
Content-Type: application/json

{
"refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}

---

### **Address Management** 🆕

#### Create Address (Manual)
POST /api/addresses
Authorization: Bearer {token}
Content-Type: application/json

{
"street": "Rua das Flores",
"number": "123",
"complement": "Apto 45",
"district": "Centro",
"city": "São Paulo",
"state": "SP",
"zipCode": "01310-100",
"country": "Brasil",
"isPrimary": true
}

#### Create Address (via CEP - ViaCEP)
POST /api/addresses/from-cep
Authorization: Bearer {token}
Content-Type: application/json

{
"zipCode": "01310-100",
"number": "456",
"complement": "Bloco B",
"isPrimary": true
}

#### List User Addresses
GET /api/addresses
Authorization: Bearer {token}

#### Get Primary Address
GET /api/addresses/primary
Authorization: Bearer {token}

#### Update Address
PUT /api/addresses/{id}
Authorization: Bearer {token}
Content-Type: application/json

{
"street": "Rua Atualizada",
"number": "789",
"district": "Jardins",
"city": "São Paulo",
"state": "SP",
"zipCode": "01310-100"
}

#### Set as Primary
PATCH /api/addresses/{id}/set-primary
Authorization: Bearer {token}

#### Delete Address (Soft Delete)
DELETE /api/addresses/{id}
Authorization: Bearer {token}

---

## 🧪 Testing

### **Run all tests**
mvn clean test

### **Run with coverage**
mvn clean test jacoco:report

View report: `target/site/jacoco/index.html`

### **Test structure**
- **Unit tests:** `src/test/java/service/`
- **Integration tests:** `src/test/java/integration/`
- **Test coverage:** ~85%

---

## 🔄 CI/CD Pipeline

### **GitHub Actions Workflows**

#### **1. CI/CD Pipeline** (`ci.yml`)
- Runs on every push/PR to `main` and `develop`
- Java 21 + Maven
- Testcontainers with PostgreSQL
- JaCoCo coverage report
- Artifact upload on failure

#### **2. Code Quality** (`qodana_code_quality.yml`)
- Qodana static analysis
- Detects bugs, vulnerabilities, code smells
- Posts comments on PRs

#### **3. Security Scan** (`security.yml`)
- Dependency vulnerability scanning
- SARIF report generation

---

## 🔐 Environment Variables

### **Required**

| Variable | Description | Example |
|----------|-------------|---------|
| `SPRING_DATASOURCE_URL` | Database connection string | `jdbc:postgresql://localhost:5432/woodexcess` |
| `SPRING_DATASOURCE_USERNAME` | Database user | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `postgres` |
| `JWT_SECRET` | JWT signing key (min 256 bits) | `your-secret-key-here` |

### **Optional**

| Variable | Description | Default |
|----------|-------------|---------|
| `JWT_ACCESS_EXPIRATION_MS` | Access token expiration | `900000` (15 min) |
| `JWT_REFRESH_EXPIRATION_MS` | Refresh token expiration | `604800000` (7 days) |
| `VIACEP_URL` | ViaCEP API base URL | `https://viacep.com.br/ws` |

---

## 🗄 Database Migrations

Migrations are managed by **Flyway** and located in `src/main/resources/db/migration/`.

### **Migration naming convention**
V{version}__{description}.sql

Examples:
V1__create_users_table.sql
V2__create_refresh_tokens_table.sql
V3__create_addresses_table.sql

### **Run migrations manually**
mvn flyway:migrate

---

## 📊 Code Quality Metrics

- **Test Coverage:** ~85%
- **Code Quality:** Qodana scan passing
- **Security:** No critical vulnerabilities
- **Performance:** Rate limiting enabled

---

## 👥 Contributing

This is a **portfolio and internal team project**. External contributions are not being accepted.

### **PR Guidelines** (for team members)
1. Create a feature branch: `feature/your-feature-name`
2. Follow conventional commits: `feat:`, `fix:`, `refactor:`, `test:`, `docs:`
3. All tests must pass
4. Code coverage must not decrease
5. Qodana scan must pass
6. Request review from at least 1 team member

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 📞 Contact

**Z&C TECH Team**
- GitHub: [@JoaoZabarella](https://github.com/JoaoZabarella)
- Repository: [woodexcess-api](https://github.com/JoaoZabarella/woodexcess-api)

---

**Developed with ❤️ for sustainable woodworking practices.**

