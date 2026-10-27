# Command: Generate Tests

Generate a comprehensive test suite for a specified class, service, repository, or feature based on `.cursor/rules/testing.md` and project test strategy documentation.

## Test Generation Workflow
1. Identify the target class or feature under test.
2. Determine the appropriate test level:
   - **Service Layer**: JUnit 5 + Mockito unit tests (`@ExtendWith(MockitoExtension.class)`), no Spring context, mocking all external repositories/clients.
   - **State Machine / Ticket Lifecycle**: Exhaustive tests for every valid transition and every invalid transition (asserting correct exceptions/HTTP codes).
   - **Repository Layer**: `@DataJpaTest` against local H2 for custom queries and constraints.
   - **Controller / API Layer**: `@SpringBootTest` or `@WebMvcTest` + `MockMvc` testing status codes, payload serialization, `@Valid` bean validations, and `Location` headers.
3. Apply conventions:
   - Follow `given_when_then` or `should_expectedBehavior_when_stateUnderTest` naming style.
   - Structure tests with clear Arrange-Act-Assert blocks.
   - Use test data builders or static factory fixtures to avoid brittle or repetitive setup.
   - Guarantee test independence (no shared mutable state, order-agnostic).

## Delivery Format
- Provide the complete, compiling test code ready for insertion.
- List which scenarios and edge cases are covered by the generated tests.

## Action Policy
**Do not write or overwrite any test files automatically.** Present the proposed test plan and generated test code in the chat, then explicitly ask the user for confirmation before saving any files.
