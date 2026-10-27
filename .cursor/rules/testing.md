# Testing Guidelines & Standards

## Test Pyramids & Test Types
- **Service Layer Unit Tests**:
  - Use **JUnit 5** and **Mockito** (`@ExtendWith(MockitoExtension.class)`).
  - Mock dependencies (`@Mock`) and inject them via `@InjectMocks`.
  - Fast, isolated, with no Spring ApplicationContext spun up.
- **Repository Tests**:
  - Use `@DataJpaTest` to verify custom queries, mappings, and database constraints.
  - Run against the local in-memory H2 database.
- **API & Integration Tests**:
  - Use `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)` or `@WebMvcTest`.
  - Use **MockMvc** to verify endpoints, HTTP status codes, Bean Validation, serialization/deserialization, and error response shapes.
  - Verify full-stack flows against H2 database instances.

## Ticket State Machine Coverage
- **Exhaustive Transitions**: The ticket lifecycle / state machine must have explicit, exhaustive test cases:
  - Every valid transition must be tested (e.g., `OPEN -> IN_PROGRESS`, `IN_PROGRESS -> RESOLVED`, etc.).
  - Every invalid transition must be tested and confirmed to fail with a specific domain exception or error response (e.g., `CLOSED -> IN_PROGRESS`).
  - Edge cases, unauthorized actor transitions, and state pre-conditions must be covered.

## Naming Conventions & Structure
- **Method Naming**: Follow clear `given_when_then` or `should_expectedBehavior_when_stateUnderTest` naming style.
  - Example: `givenOpenTicket_whenAssignAgent_thenStateTransitionsToInProgress()`
  - Example: `shouldThrowConflictException_whenTransitioningFromClosedToOpen()`
- **Arrange-Act-Assert (AAA)**: Clearly delineate test steps using comments or blank lines:
  - `// Given` / `// Arrange`
  - `// When` / `// Act`
  - `// Then` / `// Assert`

## Test Data & Isolation
- **Clean Test Data Creation**:
  - Prefer explicit Test Builder patterns, factory fixtures, or static helper methods over raw, verbose constructors.
  - Avoid sharing mutable state across tests.
- **Independence & Order**:
  - Tests must never depend on execution order or side effects of preceding tests.
  - Ensure every test cleans up or runs inside a rolled-back transaction (`@Transactional` on test classes where appropriate).

## Regression Testing & Bug Fixes
- **Mandatory Bug Fix Tests**: Every reported bug fix must be accompanied by at least one dedicated regression test reproducing the original issue and asserting the fix.
- **Continuous Validation**: All unit and integration test suites must pass before any feature branch or task is considered complete.
