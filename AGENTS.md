# AGENTS.md - Blog Backend

## Build & Run Commands

```bash
# Build
./mvnw clean compile          # Maven wrapper (Windows: mvnw.cmd)
mvn clean compile             # System Maven

# Run
mvn spring-boot:run           # Run app (default port 8080)
java -jar target/blog-backend-1.0.0.jar  # Run packaged JAR

# Test
mvn test                      # Run all tests
mvn test -Dtest=BlogBackendApplicationTests  # Run single test class
mvn test -Dtest=BlogBackendApplicationTests#contextLoads  # Run single test method

# Package
mvn clean package             # Create JAR in target/
mvn clean package -DskipTests  # Package without running tests

# Kill port 8080 (Windows)
netstat -ano | findstr :8080
taskkill //PID <pid> //F
```

## Environment Setup

- Java 17 required
- MySQL 8+ (localhost:3306/blog_db)
- Copy `.env.example` to `.env` and set `DB_PASSWORD` and `JWT_SECRET`
- App loads `.env` before Spring context starts (via `BlogApplication.loadEnvFile()`)

## Code Style

### Architecture Layers
- `entity/` - JPA entities with Lombok (`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`)
- `dto/request/` - Request DTOs with `@Valid` constraints
- `dto/response/` - Response DTOs with builder pattern
- `controller/` - REST controllers (`@RestController` + `@RequestMapping`)
- `service/` - Business logic (`@Service` + `@RequiredArgsConstructor`)
- `repository/` - Spring Data JPA (`@Repository`)
- `mapper/` - Manual entity-DTO converters
- `exception/` - Custom exceptions + `GlobalExceptionHandler` (`@RestControllerAdvice`)
- `security/` - JWT auth filter, `JwtUtil`, `CustomUserDetailsService`
- `config/` - `@Configuration` classes (Security, JPA, OpenAPI)

### Naming Conventions
- Entities: `User`, `Post`, `Comment`, `BaseEntity`
- DTOs: `PostRequest`, `PostResponse`, `ApiResponse<T>`
- Services: `AuthService`, `PostService`, `CommentService`
- Controllers: `AuthController`, `PostController`, `CommentController`
- Custom exceptions: `ResourceNotFoundException`, `BadRequestException`, `UnauthorizedException`

### Imports
- Group: Java standard → Jakarta → Spring → Third-party → Project
- Use `jakarta.*` (not `javax.*`) for JPA, Validation
- Prefer explicit imports over wildcards (except `@RestController` handler)

### Dependency Injection
- Constructor injection via `@RequiredArgsConstructor` (Lombok)
- Fields must be `private final`

### Entity Conventions
- Extend `BaseEntity` for `createdAt`/`updatedAt` audit fields
- Use `@Builder.Default` for default field values
- Use `@Column(nullable = false)` for required fields
- Use `@Enumerated(EnumType.STRING)` for enum columns

### Exception Handling
- Throw `ResourceNotFoundException` for missing entities (404)
- Throw `BadRequestException` for validation/business errors (400)
- Throw `UnauthorizedException` for auth failures (401)
- All return wrapped in `ApiResponse<T>` with `{success, message, data}`

### API Response Format
```java
// Success: ApiResponse.success("message", data)
// Error: ApiResponse.error("message")
// Validation: ApiResponse.error("Validation failed", fieldErrors)
```

### Transactions
- `@Transactional` on mutating service methods
- `@Transactional(readOnly = true)` on read-only methods

### Testing
- JUnit 5 (`@SpringBootTest`, `@Test`)
- Spring Security Test available in test scope
- No existing test patterns beyond `BlogBackendApplicationTests` - add tests when modifying services

### Configuration Files
- `application.yaml` (not `.properties`)
- Properties use `${ENV_VAR}` for secrets (`.env` file)
- JWT config: `jwt.secret`, `jwt.expiration`

### Code Conventions
- No comments in code (add only if requested)
- Use Java 17 features (streams, records where appropriate)
- Keep controllers thin - delegate to services
- Use `@Valid` on `@RequestBody` for request validation
