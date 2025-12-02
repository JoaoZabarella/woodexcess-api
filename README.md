# 🪵 WoodExcess API

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
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

### **Address Management**
- ✅ Multiple addresses per user (up to 5)
- ✅ Automatic address filling via ViaCEP API
- ✅ Primary address designation
- ✅ Address validation (no duplicates)
- ✅ Soft delete for addresses

### **Material Listings**
- ✅ Create, read, update, and deactivate material listings
- ✅ **Image Upload**: Upload, manage, and reorder listing images (AWS S3 integration)
- ✅ Advanced filtering (material type, location, price range, condition)
- ✅ Pagination and sorting
- ✅ Owner/Admin authorization
- ✅ Public listing browsing (GET endpoints)
- ✅ Denormalized location fields for fast queries

### **Coming Soon**
- 🔄 Real-time chat between users
- 🔄 Negotiation system
- 🔄 Notifications system
- 🔄 User dashboard

---

## 🛠 Tech Stack

### **Core**
- **Java 21** (LTS)
- **Spring Boot 3.3.6**
- **Maven** (dependency management)

### **Frameworks & Libraries**
- **Spring Web** - REST API
- **Spring Data JPA** - ORM with Hibernate
- **Spring Security** - Authentication & Authorization
- **Spring Validation** - Bean validation
- **Spring Cloud OpenFeign** - HTTP client for ViaCEP integration
- **Spring Cache** - Caching support
- **SpringDoc OpenAPI** - Swagger UI & API Documentation

### **Database**
- **PostgreSQL 16** (production)
- **H2** (testing with Testcontainers)
- **Flyway** - Database migrations

### **Security & Auth**
- **JWT** (JSON Web Tokens)
- **BCrypt** - Password hashing
- **Bucket4j** - Rate limiting

### **Cloud & Storage**
- **AWS S3** - Image storage
- **Thumbnailator** - Image processing & resizing

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

```
src/
├── main/
│   ├── java/com/z/c/woodexcess_api/
│   │   ├── config/         # Configuration classes
│   │   ├── controller/     # REST endpoints
│   │   ├── dto/            # Data Transfer Objects
│   │   ├── exception/      # Custom exceptions & handlers
│   │   ├── mapper/         # MapStruct mappers
│   │   ├── model/          # JPA entities
│   │   ├── repository/     # Spring Data repositories
│   │   ├── security/       # Security filters & configs
│   │   └── service/        # Business logic
│   └── resources/
│       ├── db/migration/   # Flyway migrations
│       └── application.yml
└── test/
    ├── java/
    │   ├── integration/    # Integration tests
    │   └── service/        # Unit tests
    └── resources/
        └── application-test.yml
```

---

## 🚀 Getting Started

### **Prerequisites**
- Java 21+ ([Download](https://www.oracle.com/java/technologies/downloads/))
- Maven 3.9+ ([Download](https://maven.apache.org/download.cgi))
- Docker Desktop (for Testcontainers) ([Download](https://www.docker.com/products/docker-desktop))
- PostgreSQL 16+ (production) ([Download](https://www.postgresql.org/download/))

### **1. Clone the repository**
```bash
git clone https://github.com/JoaoZabarella/woodexcess-api.git
cd woodexcess-api
```

### **2. Configure database**

**Option A: Docker (recommended for development)**
```bash
docker run -d \
  --name woodexcess-postgres \
  -e POSTGRES_DB=woodexcess \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:16-alpine
```

**Option B: Local PostgreSQL**
```sql
CREATE DATABASE woodexcess;
```

### **3. Configure environment variables**

Create a `.env` file or set environment variables:

```properties
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/woodexcess
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres

# JWT
JWT_SECRET=your-secret-key-min-256-bits-long-replace-this-in-production
JWT_ACCESS_EXPIRATION_MS=900000
JWT_REFRESH_EXPIRATION_MS=604800000

# ViaCEP
VIACEP_URL=https://viacep.com.br/ws

# AWS S3 (For Image Upload)
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key
AWS_S3_BUCKET=woodexcess-listings
AWS_REGION=us-east-1
```

### **4. Run the application**

**Using Maven:**
```bash
mvn spring-boot:run
```

**Using Docker:**
```bash
docker build -t woodexcess-api .
docker run -p 8080:8080 woodexcess-api
```

### **5. Access the API**
- **Base URL:** `http://localhost:8080`
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **Health Check:** `http://localhost:8080/actuator/health`

---

## 📍 API Endpoints

### **Authentication**

#### Register
`POST /api/users/register`
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "phone": "11987654321",
  "password": "StrongPass123!@#"
}
```

#### Login
`POST /api/auth/login`
```json
{
  "email": "john@example.com",
  "password": "StrongPass123!@#"
}
```

#### Refresh Token
`POST /api/auth/refresh`

---

### **Address Management**

- `POST /api/addresses` - Create Address (Manual)
- `POST /api/addresses/from-cep` - Create Address (via ViaCEP)
- `GET /api/addresses` - List User Addresses
- `GET /api/addresses/primary` - Get Primary Address
- `PUT /api/addresses/{id}` - Update Address
- `PATCH /api/addresses/{id}/set-primary` - Set as Primary
- `DELETE /api/addresses/{id}` - Delete Address

---

### **Material Listings**

#### Create Listing
`POST /api/listings`
```json
{
  "title": "Sobra de Madeira de Lei - Ipê",
  "description": "Tábuas de ipê em excelente estado",
  "materialType": "WOOD",
  "price": 150.50,
  "quantity": 10,
  "condition": "USED",
  "addressId": "address-uuid"
}
```

#### List Listings (with filters)
`GET /api/listings?materialType=WOOD&city=São Paulo&minPrice=100`

#### Listing Images
- `POST /api/listings/{listingId}/images` - Upload image (Multipart)
- `GET /api/listings/{listingId}/images` - Get all images
- `PUT /api/listings/{listingId}/images/{imageId}/primary` - Set image as primary
- `PUT /api/listings/{listingId}/images/reorder` - Reorder images
- `DELETE /api/listings/{listingId}/images/{imageId}` - Delete image

---

## 🧪 Testing

### **Run all tests**
```bash
mvn clean test
```

### **Run with coverage**
```bash
mvn clean test jacoco:report
```
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
| `AWS_ACCESS_KEY_ID` | AWS Access Key | - |
| `AWS_SECRET_ACCESS_KEY` | AWS Secret Key | - |
| `AWS_S3_BUCKET` | S3 Bucket Name | `woodexcess-listings` |
| `AWS_REGION` | AWS Region | `us-east-1` |

---

## 🗄 Database Migrations

Migrations are managed by **Flyway** and located in `src/main/resources/db/migration/`.

### **Migration naming convention**
`V{version}__{description}.sql`

### **Run migrations manually**
```bash
mvn flyway:migrate
```

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
