# Copilot Instructions - SOBU Website Repository

This is a full-stack Spring Boot + Vite application for SOBU website. The repository is organized into `backend` (Spring Boot REST API) and `frontend` (Vite-based SPA) directories.

## Build & Test Commands

### Backend (Spring Boot)

**Maven wrapper is available** - use `mvnw` (Windows) or `mvnw.sh` (Unix) instead of `mvn` for reproducible builds.

```bash
cd backend

# Full build and test
mvnw clean test

# Build only (skip tests)
mvnw clean package -DskipTests

# Run specific test class
mvnw test -Dtest=AuthServiceTest
mvnw test -Dtest=JwtServiceTest

# Run tests matching a pattern
mvnw test -Dtest=*Service*Test

# Start development server
mvnw spring-boot:run

# Generate coverage reports
mvnw clean test jacoco:report
# Report generated at: target/site/jacoco/index.html
```

**Profile-based configuration**: Use Spring profiles via `-Dspring.profiles.active=dev` or environment variable. Existing profiles:
- `dev` (default) - application-dev.properties with localhost DB
- `prod` - application-prod.properties

### Frontend (Vite)

Frontend is a placeholder (`README.md` file only) - currently no build configuration. When implementing:
- Check if package.json exists
- Likely uses Vite with TypeScript given the backend is modern Java

## Architecture Overview

### Backend Architecture

**Package structure**: `com.vn.sodu`

- **`user/`** - User account and authentication
  - `Account` (entity), `Role` entities
  - `AccountRepo`, `RoleRepo` (Spring Data JPA repositories)
  - `AuthService` - core auth logic (login, register, refresh, activate)
  - `UserService` - user operations
  - `AuthController` - REST endpoints (`/api/auth/*`)
  - `dto/` - request/response DTOs with Lombok builders
  - `mapper/` - entity ↔ DTO converters (MapStruct-like manual pattern)

- **`security/`** - JWT and Spring Security
  - `JwtService` - token generation/validation, claim extraction (JJWT library v0.12.6)
  - `JwtAuthFilter` - request filter for Bearer token validation
  - `UserDetailsServiceImpl` - Spring Security UserDetails loader
  - `SecurityConfig` - Spring Security configuration

- **`customer/`** - Customer management
  - `Customer`, `LoyaltyTier`, `LoyaltyRule`, `LoyaltyTransaction` entities
  - Loyalty points system tied to customer accounts

- **`mail/`** - Email functionality
  - `EmailService` interface with `SmtpEmailService` implementation
  - Used for account activation emails
  - Config via properties: `spring.mail.host`, `spring.mail.port`, etc.

- **`global/`** - Shared utilities
  - DTOs like `ApiResponseDTO`, `PageResponse`, `ErrorResponse` (reused across modules)
  - Generic pagination/filtering request objects

- **`config/`** - Spring configuration
  - `WebConfig` - CORS, interceptors, etc.
  - `MailAutoConfiguration` - mail bean setup
  - `SecurityConfig` - authentication and authorization

- **`utilites/`** - Helpers
  - `PasswordEncrypt` - encryption/decryption for passwords (custom implementation)

### Key Patterns

1. **Service Layer Pattern**: All business logic in `*Service` classes with `@Service` annotation
2. **Dependency Injection**: Constructor injection with Lombok `@RequiredArgsConstructor`
3. **Entity-DTO Mapping**: Manual mappers (not MapStruct) that handle null checks and type conversions
4. **Global DTO Pattern**: Shared response wrappers (`ApiResponseDTO`) for all endpoints
5. **JWT with Claims**: Access tokens contain username, refresh tokens contain `type: refresh` claim
6. **Account Status Enum**: `ACTIVE`, `INACTIVE`, `BANNED` states with authorization checks
7. **Logging**: SLF4J with `@Slf4j` annotation, structured logging in service methods
8. **Repository Pattern**: Spring Data JPA with custom query methods in `*Repo` interfaces

### Database Schema

- **Account** - user login info, status (ACTIVE/INACTIVE/BANNED), linked to Role
- **Role** - permissions/roles for users
- **ActivationToken** - email verification tokens with expiration
- **Customer** - extended customer profile tied to Account
- **LoyaltyTier, LoyaltyRule, LoyaltyTransaction** - loyalty/points system

MySQL is the database (`application-dev.properties` shows localhost:3306/sodu).

## Code Conventions

### JWT & Authentication
- **Access Token**: 15 minutes expiration (900000 ms) - contains username
- **Refresh Token**: 7 days expiration (604800000 ms) - contains `type: refresh` claim
- **Token Format**: Bearer token in Authorization header: `Authorization: Bearer <token>`
- **Account Activation**: One-time tokens valid for 24 hours, deleted after use

### Exception Handling
- Use generic `RuntimeException` throughout (can be improved with custom exceptions)
- Controllers should catch and convert to `ApiResponseDTO` error wrapper
- All exceptions logged with context (email, action) in catch blocks

### DTOs & Requests
- DTOs use Lombok: `@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`
- Request/response objects suffixed: `Request`, `Response`, `DTO`
- Mappers manually handle null checks and field conversions
- No validation annotations used - validation done in service methods

### API Response Format
- All endpoints return `ApiResponseDTO` (generic wrapper with status, message, data)
- Success responses include data, error responses include error message
- HTTP status codes: 200 (success), 401 (auth fail), 400 (bad request)

### SQL Initialization
- `spring.sql.init.mode=always` in dev config - runs init scripts on startup
- `spring.jpa.hibernate.ddl-auto=update` - auto-updates schema from entities

### Password Handling
- Passwords stored as encrypted strings (not hashed) using custom `PasswordEncrypt`
- Login flow decrypts stored password and compares with request password
- **Note**: This is non-standard; consider bcrypt for production

## Testing

Tests use **JUnit 5** + **Mockito** + **Spring Boot Test**:
- `@SpringBootTest` with `TestPropertySource` for integration tests
- `@ExtendWith(MockitoExtension.class)` for unit tests with mocks
- Naming convention: `Test[ComponentName][Scenario]` e.g., `testLoginWithInactiveAccount()`

**Test files location**: `src/test/java/com/vn/sodu/**/*Test.java`

Current test coverage includes:
- JwtServiceTest (22 tests) - token generation, validation, claims
- AuthServiceTest (22 tests) - login, registration, refresh, activation
- JwtAuthFilterTest (14 tests) - bearer token validation, security context

## Configuration Files

**Properties Files** (in `src/main/resources/`):
- `application.properties` - shared config (server port 8081, multipart limits)
- `application-dev.properties` - dev defaults (MySQL localhost, dev JWT secrets, mail config)
- `.env` / `.env.example` - environment variable overrides (optional)

**Key Properties to Override**:
- `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password`
- `jwt.secret`, `jwt.access-token-expiration`, `jwt.refresh-token-expiration`
- `encryption.secret-key` - for password encryption
- `spring.mail.*` - email configuration

## API Documentation

Swagger/OpenAPI documentation available via **SpringDoc**:
- Access at: `http://localhost:8081/swagger-ui.html` (when running)
- Configuration: `@Tag` and `@Operation` annotations on controllers
- JSON schema: `http://localhost:8081/v3/api-docs`

All endpoints tagged with module (e.g., `@Tag(name = "Authentication", ...)`) for organization.

## IDE Setup Notes

- **Java Version**: 17 (configured in pom.xml)
- **Build Tool**: Maven 3.x (use mvnw)
- **Lombok**: Enabled for annotations - IDE needs Lombok plugin
- **Spring DevTools**: Enabled for hot reload in dev mode

## Working with Existing Code

### Adding a New Endpoint
1. Create DTO classes (Request/Response) with Lombok annotations
2. Create controller method with `@PostMapping`, `@GetMapping`, etc.
3. Implement logic in service layer
4. Add Swagger annotations (`@Operation`, `@ApiResponse`)
5. Map entities to DTOs using mapper component

### Modifying Authentication
- Token expiration times are in `application-dev.properties`
- JWT claims are set in `JwtService.buildToken()` - add claims via `extraClaims` map
- Refresh token logic is in `AuthService.refreshToken()`

### Adding Database Entities
- Create entity class with JPA annotations in `user/`, `customer/`, etc.
- Create `*Repo extends JpaRepository<Entity, ID>` interface
- Create service with business logic
- Ensure Hibernate DDL-auto is set appropriately (currently `update`)

## Common Issues

- **JWT Signature Errors**: Check `jwt.secret` length (must be sufficient for HS256)
- **Database Connection**: Verify MySQL is running on localhost:3306 and database `sodu` exists
- **Email Service**: Configure `spring.mail.*` properties with valid SMTP credentials
- **Account Activation**: Tokens expire after 24 hours; regenerate if needed
