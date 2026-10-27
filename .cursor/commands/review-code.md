# Command: Review Code

Perform a comprehensive code review on the latest code changes or specified files against our project standards:
- `.cursor/rules/java-springboot.md`
- `.cursor/rules/api-standards.md`
- `.cursor/rules/testing.md`

## Review Process
1. Inspect the modified or specified files.
2. Verify adherence to:
   - **Spring Boot & Java 21 conventions**: Layered architecture, constructor injection only, record DTOs, Bean Validation, no entities leaked, service transaction boundaries, clean exception handling.
   - **REST API standards**: `/api/v1` prefix, plural naming, HTTP status codes (201 with `Location`, 400, 404, 409/422), uniform error response shape, proper query parameter naming.
   - **Testing rules**: Unit tests for services (JUnit 5 + Mockito), repository tests (`@DataJpaTest`), integration tests (`@SpringBootTest` + `MockMvc`), state machine coverage, and `given_when_then` naming.
   - **Security**: No secrets or credentials, safe logging without sensitive/PII data, input sanitization.

## Output Format
Group findings clearly by severity:
- 🔴 **Bug**: Functional defect, unintended side effect, or broken business logic.
- 🟡 **Standards Violation**: Deviation from project rules (e.g., field injection, missing `@Valid`, exposed entity, incorrect HTTP code).
- 🔒 **Security**: Hardcoded credentials, insecure defaults, improper data exposure in logs or responses.
- 💡 **Suggestion**: Non-blocking improvements for maintainability, clarity, or performance.

If no issues are found in a category, state that clearly.

## Action Policy
**Do not modify or edit any files automatically.** Present your findings and proposed fixes, then explicitly ask the user for confirmation before changing any files.
