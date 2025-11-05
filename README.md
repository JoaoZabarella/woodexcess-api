# WoodExcess

A micro SaaS platform that connects woodworkers and store owners, enabling them to list, manage, and trade surplus materials. The goal is to reduce waste, optimize inventory, and promote sustainable practices within the woodworking industry.

## Main Features

- User registration and login
- CRUD for listing surplus materials
- Search and filter for material listings
- Detailed ad/listing view
- Basic chat between users
- Notifications for user activity and interests
- User dashboard for profile and ad management

## Technologies & Dependencies

- Java 21 (also compatible with Java 17)
- Spring Boot
- Spring Web (REST API)
- Spring Data JPA (ORM)
- Spring Security (authentication & authorization)
- Validation (data validation)
- Lombok (to reduce boilerplate code)
- Flyway (for database migrations)
- Database: H2 (development/testing) or MySQL/PostgreSQL (production)
- Spring Boot DevTools (automatic reload during development)
- Spring Boot Actuator (monitoring & production metrics — optional, but included)
- Spring WebSocket (real-time chat — optional)

## How to run locally

1. **Clone this repository**
    ```
    git clone https://github.com/JoaoZabarella/woodexcess.git
    cd woodexcess
    ```

2. **Configure the properties file**
    - Copy `src/main/resources/application.properties.example` to `application.properties`.
    - Fill in with your local configuration, or set the required environment variables on your machine.

3. **Start the application**
    - Using Maven:
        ```
        mvn spring-boot:run
        ```
    - Or run directly from IntelliJ/Eclipse.

## Recommended environment variables

- `DB_URL` — Database connection string (e.g., `jdbc:h2:mem:testdb` for development)
- `DB_USER` — Database user
- `DB_PASS` — Database password

## Database scripts & migrations

- Migrations are managed by Flyway and placed in `src/main/resources/db/migration`. They are executed automatically at app startup.

## PR Guidelines

- Follow the project PR template for every Pull Request.
- Never commit or push sensitive data/secrets to the repository.
- Use descriptive branch names (e.g., `feature/user-registration`).

## Contribution

This is a portfolio and internal team project. External contributions are not being accepted.  
The repository is public for demonstration and recruitment purposes only.

---

Developed by [Z&C TECH team]. Feedback is always welcome!
