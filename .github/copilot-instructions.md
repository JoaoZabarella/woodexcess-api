# GitHub Copilot Code Review Instructions

## Project Context
**WoodExcess API** - Spring Boot 3.3.6 SaaS marketplace for construction materials (wood, metal, glass)

**Tech Stack:**
- Java 21
- Spring Boot (Web, Security, Data JPA)
- PostgreSQL + Testcontainers
- MapStruct, Lombok
- JWT Authentication
- AWS S3 (images)
- WebSocket (real-time chat)

---

## Review Priorities

### 🔴 **Critical (MUST FIX before merge)**

**Security:**
- SQL injection vulnerabilities
- XSS attacks in user inputs
- Exposed credentials or API keys
- Authentication/Authorization bypasses
- Insecure password handling

**Performance:**
- N+1 query problems
- Missing database indexes
- Unbounded queries (no pagination)
- Resource leaks (connections, streams)
- Memory leaks

**Critical Bugs:**
- NullPointerException risks
- Race conditions
- Data integrity violations
- Transaction boundaries

---

### 🟡 **High Priority (fix soon)**

**Code Quality:**
- Unused imports/variables/methods
- Dead code
- Magic numbers (use constants)
- Hardcoded strings (use enums/constants)
- Missing error handling

**Architecture:**
- Layer violations (Controller → Repository direct)
- Entity exposure (return DTOs, not Entities)
- Business logic in Controllers
- Missing validation (@Valid, @Validated)

**Testing:**
- Missing tests for critical paths
- Low test coverage (<80%)
- Tests with wrong assertions
- Flaky tests

---

### 🟢 **Low Priority (nice to have)**

**Style:**
- Empty lines
- Code formatting
- Inconsistent spacing

**Documentation:**
- Missing Javadoc
- Unclear variable names
- Missing @Schema descriptions

---

## Ignore List

**Don't flag these:**
- Lombok annotations (@Builder, @Data, @RequiredArgsConstructor)
- Records without explicit getters (auto-generated)
- Testcontainers usage in integration tests
- @Transactional in service layer
- Spring Boot auto-configuration

---

## Project Patterns

### Language
- **Code:** English (classes, methods, variables)
- **Comments:** Portuguese (team communication)
- **Commits:** English (Conventional Commits)

### Commit Conventions
feat: new feature
fix: bug fix
perf: performance improvement
refactor: code refactoring
test: add/update tests
docs: documentation
chore: maintenance tasks


### Code Standards
- **Tests:** Minimum 80% coverage
- **Validation:** Always use @Valid in controllers
- **DTOs:** Never expose entities in REST responses
- **Exceptions:** Use custom exceptions (ResourceNotFoundException, etc.)
- **Pagination:** Always paginate list endpoints

### Naming
- **Endpoints:** `/api/{resource}` (plural)
- **DTOs:** `{Action}{Resource}Request/Response`
- **Services:** `{Resource}Service`
- **Tests:** `should{Behavior}When{Condition}`

---

## Response Format

When reviewing, please:
1. **Categorize** issues by priority (🔴/🟡/🟢)
2. **Explain WHY** it's a problem
3. **Suggest** concrete fix with code example
4. **Reference** best practices when applicable

---

## Examples

### ❌ Bad Review:
> "This code is wrong"

### ✅ Good Review:
> 🔴 **N+1 Query Problem**
>
> The mapper calls `repository.countByListing()` for each favorite, causing N+1 queries.
>
> **Fix:** Implement bulk count fetch:
> ```java
> Map<UUID, Long> counts = repository.countByListingIds(listingIds);
> ```
>
> **Impact:** Performance - 98% faster for 100+ items

