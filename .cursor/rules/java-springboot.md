# Java 21 & Spring Boot Conventions

## Architecture & Layering
Strict layered architecture with one-way dependencies (`Controller -> Service -> Repository -> Database`):
- **`controller`**: Handles HTTP requests/responses, status codes, and input validation. No business logic.
- **`service`**: Implements business logic and orchestration. Transaction boundaries live here.
- **`repository`**: Spring Data interfaces for database access. No business rules.
- **`domain` / `model`**: JPA entities representing persistent state. Never expose directly to API clients.
- **`dto`**: Request and response representations.

## Dependency Injection & Clean Code
- **Constructor Injection only**: Use `final` fields and explicit constructors (or `@RequiredArgsConstructor`).
- **No field injection**: Never use `@Autowired` on private fields.

## DTOs & Validation
- **Java Records for DTOs**: Use immutable Java 21 `record` types for all request bodies and response payloads.
- **Bean Validation**: Annotate request records with `jakarta.validation.constraints` (`@NotNull`, `@NotBlank`, `@Size`, `@Email`, etc.).
- **Controller Validation**: Always annotate request parameters with `@Valid` or `@Validated`.
- **Entity Decoupling**: Map entities to DTOs in service/mapper layers before returning to controllers.

## Transactions
- **Service Layer Boundaries**: Place `@Transactional` on public service methods requiring transactional scope.
- **Read-Only Optimization**: Use `@Transactional(readOnly = true)` on query/read methods.
- Never place `@Transactional` on controllers.

## Error Handling
- **Global Exception Handler**: Use `@RestControllerAdvice` to intercept exceptions across the app.
- **Consistent Error Payload**: Return a standard problem payload (e.g., timestamp, status code, error, message, validation field errors).
- **Domain Exceptions**: Define custom unchecked exceptions (extending `RuntimeException`) for business failures (e.g., `ResourceNotFoundException`, `ConflictException`).

## Security & Configuration
- **Externalised Configuration**: Store environment-specific values in `application-dev.yml` (H2) and `application-prod.yml` (PostgreSQL).
- **Credentials & Secrets**: Never hardcode credentials. Use environment variables (e.g., `${DB_PASSWORD}`) or Spring Cloud Vault / Config.
- **Safe Logging**: Use SLF4J (`log.info`, `log.error`). Never log PII, passwords, authentication tokens, or sensitive user details.
