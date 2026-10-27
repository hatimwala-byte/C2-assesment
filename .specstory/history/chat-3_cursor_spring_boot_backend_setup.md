# Spring Boot backend setup
_Exported on 10/26/2026 at 15:46:45 GMT+5:30 from Cursor (3.17.19)_

---

**User**

Let's start implementing, strictly following task 1 of the plan. Set up the Spring Boot backend in the backend folder with Java 21, Spring Web, Spring Data JPA, Validation, H2, the PostgreSQL driver, and test dependencies. Create application.yml with a default H2 profile that uses a file-based database so data survives a restart, and a postgres profile that reads the URL, username and password from environment variables. No hardcoded credentials anywhere. Add a minimal health check test to prove the context loads, and tell me how to run it.

---

**Cursor**

Reviewing the plan and project structure to implement Task 1.




Generating the Spring Boot project with the Maven wrapper and required dependencies.




Aligning with the plan (Spring Boot 3.x) and adding configuration plus the smoke test.




Fixing profile activation so the H2 profile applies at runtime (tests fell back to Spring's built-in `default` profile).




Using `${SPRING_PROFILES_ACTIVE:h2}` so the default is explicit and PostgreSQL activates via the environment.




Task 1 is done: a Maven Spring Boot 3.4.5 backend on Java 21 with the dependencies you asked for (Flyway is deferred to Task 2 per the plan).

### What was added

| Area | Details |
|------|---------|
| **Build** | `backend/pom.xml` — Web, Data JPA, Validation, H2, PostgreSQL driver, `spring-boot-starter-test`; Maven Wrapper |
| **App** | `TicketApiApplication` in `com.support.ticket` |
| **Config** | `application.yml` — default profile `h2` (`SPRING_PROFILES_ACTIVE` overrides); file H2 at `jdbc:h2:file:./data/tickets`; `postgres` profile uses only `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` (no DB passwords in repo) |
| **Tests** | `BackendContextSmokeTest` — `@SpringBootTest` context load; `src/test/resources/application.yml` uses in-memory H2 with profile `test` so tests do not touch the file DB |

H2’s local `sa` / empty password is only for the embedded dev database, not PostgreSQL.

### How to run

**Prerequisites:** JDK 21 on your PATH.

**Smoke test (Task 1 DoD):**

```powershell
cd "c:\Users\Hatim Saifee\Work\2026\October\C2-assesment\backend"
.\mvnw.cmd test
```

**Run the API (default `h2`, data under `backend/data/`):**

```powershell
cd "c:\Users\Hatim Saifee\Work\2026\October\C2-assesment\backend"
.\mvnw.cmd spring-boot:run
```

**PostgreSQL profile:**

```powershell
$env:SPRING_PROFILES_ACTIVE = "postgres"
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/tickets"
$env:SPRING_DATASOURCE_USERNAME = "your_user"
$env:SPRING_DATASOURCE_PASSWORD = "your_password"
.\mvnw.cmd spring-boot:run
```

`.\mvnw.cmd test` was run successfully here. Optional health endpoint from the plan was not added yet; the smoke test only checks that the context loads. Say if you want `GET /api/v1/health` in this task.

---

**User**

Now task 2: implement the Ticket and Comment entities, the status and priority enums, and the Spring Data repositories exactly as described in spec/data-model.md. Add a repository test using @DataJpaTest that saves a ticket with comments and reads it back. Please don't touch services or controllers yet.


---

**Cursor**

Implementing Task 2 per the data model spec. Reading the spec and current backend structure.




Checking the plan for Flyway and reviewing the backend configuration.




Implementing domain entities, Flyway migration (per Task 2), repositories, and repository tests.




Fixing the repository query method (Spring Data needs `ticket.id`) and adding the test plus config updates.




Removing global identifier quoting (it broke schema validation) and renaming the comment table to `ticket_comment` for H2 compatibility.




Fixing the repository test so Flyway runs reliably in the `@DataJpaTest` slice (it doesn't load a `DataSource` when run alone).




Task 2 is in place: domain model, Flyway schema, repositories, and a `@DataJpaTest` — no services or controllers.

### Domain (`com.support.ticket.domain`)
- **`TicketStatus`** — `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `CANCELLED`
- **`TicketPriority`** — `LOW`, `MEDIUM`, `HIGH`
- **`Ticket`** — fields, lengths, defaults (`OPEN` / `MEDIUM`), `Instant` timestamps via `@CreationTimestamp` / `@UpdateTimestamp`, lazy `OneToMany` to comments with `addComment()` helper
- **`Comment`** — `ManyToOne` to `ticket`, `body`, `created_at` only

### Schema
- **`V1__create_ticket_and_comment.sql`** — DDL from `data-model.md` §8 (CHECK constraints, indexes, FK `ON DELETE RESTRICT`)
- Table **`ticket_comment`** instead of `comment` (H2 reserves `COMMENT`; spec §5 allows this)
- Runtime: **Flyway** + `ddl-auto: validate` on `h2` and `postgres` profiles

### Repositories
- **`TicketRepository`** — `JpaRepository<Ticket, Long>`
- **`CommentRepository`** — `findByTicket_IdOrderByCreatedAtAscIdAsc(Long ticketId)` for thread ordering per spec §5

### Tests
- **`TicketCommentRepositoryTest`** — `@DataJpaTest` saves a ticket with two comments, reloads ticket fields, loads comments via repository
- Uses its own in-memory H2 DB + `create-drop` (Flyway is not loaded in the JPA test slice); full context smoke test still runs Flyway on `backend-test`

### Run tests
```powershell
cd "c:\Users\Hatim Saifee\Work\2026\October\C2-assesment\backend"
.\mvnw.cmd test
```

Both `BackendContextSmokeTest` and `TicketCommentRepositoryTest` pass locally.

**Files touched:** `pom.xml` (Flyway), `application.yml`, `src/test/resources/application.yml`, `BackendContextSmokeTest` (`@ActiveProfiles("test")`), plus new domain, repository, migration, and test classes under `backend/`.

---

**User**

Task 3: implement the ticket state machine as a small, isolated and easily testable component, based strictly on spec/state-machine.md. It should expose a way to check whether a transition is allowed and a way to enforce it by throwing a dedicated exception with a clear message that includes the current and requested states. Write unit tests covering every from-state and to-state pair, so the full matrix is exercised. Walk me through the allowed transitions table in your own words once it's done so I can double-check you've got it right.

---

**Cursor**

Implementing the transition policy, domain exception, and a parameterized 5×5 unit test matrix.




Task 3 (state machine) is implemented as a plain Java component—no Spring, services, or controllers.

### What was added

| Piece | Location |
|--------|-----------|
| **`TicketStatusTransitionPolicy`** | `domain/policy/` — `isTransitionAllowed(from, to)` and `validateTransition(from, to)` |
| **`InvalidStateTransitionException`** | `domain/exception/` — message `Cannot transition ticket from {CURRENT} to {TARGET}.` plus getters for both statuses |
| **`TicketStatusTransitionPolicyTest`** | 25 parameterized cases (5×5), asserting allowance, `validateTransition` behavior, and exception details |

**Semantics (per spec §7.5):** Same current and target status is **allowed** (idempotent; service layer can no-op later). Any other pair must be one of the five edges below, or it is rejected.

Run unit tests only:

```powershell
cd backend
.\mvnw.cmd test -Dtest=TicketStatusTransitionPolicyTest
```

---

### Allowed transitions (in plain language)

**Starting state: `OPEN` (every new ticket)**  
- You may **start work** → `IN_PROGRESS`.  
- You may **cancel without progressing** → `CANCELLED`.  
- You may **not** jump to `RESOLVED` or `CLOSED` (no shortcuts).  
- **Staying** `OPEN` is fine (retry / already there).

**From `IN_PROGRESS`**  
- You may **finish work** → `RESOLVED`.  
- You may **cancel while in progress** → `CANCELLED`.  
- You may **not** go back to `OPEN`, skip to `CLOSED`, or do anything else.  
- **Staying** `IN_PROGRESS` is allowed.

**From `RESOLVED`**  
- You may **close after resolution** → `CLOSED` (only forward step).  
- You may **not** reopen, go back to `IN_PROGRESS`, cancel to `CANCELLED`, etc.  
- **Staying** `RESOLVED` is allowed.

**Terminal: `CLOSED` and `CANCELLED`**  
- **No** outbound transitions to any other status (including reopening to `OPEN`).  
- **Staying** in the same terminal state is allowed.

**Across the board**  
- Transitions **to `OPEN`** are never allowed except **idempotent** `OPEN` → `OPEN`.  
- There are exactly **five** distinct allowed moves:  
  `OPEN→IN_PROGRESS`, `OPEN→CANCELLED`, `IN_PROGRESS→RESOLVED`, `IN_PROGRESS→CANCELLED`, `RESOLVED→CLOSED`.  
- All other **different** from/to pairs are forbidden and `validateTransition` throws `InvalidStateTransitionException`.

That matches the spec table in `state-machine.md` §3–§4 and the 5×5 matrix in `test-strategy.md` §6.1 (diagonal = allowed idempotent; five off-diagonal ✓ cells = the edges above; everything else = forbidden).

---

**User**

Task 4: implement the ticket service for create, get, list, and update of title, description, priority and assignee, plus the status change operation which must use the state machine. Add request DTOs with Bean Validation (required title with a max length, description length limit, valid priority). Make sure updates can never change status through the normal field update path. Write service unit tests with Mockito, including the failure paths.

---

**Cursor**

Implementing the ticket service layer per spec. Reviewing API contracts and existing code.




Implementing DTOs, mapper, service, exceptions, and Mockito unit tests.




Task 4 is done: ticket service, DTOs with validation, and Mockito unit tests. No controllers yet.

### DTOs (`com.support.ticket.dto`)
| DTO | Role |
|-----|------|
| **`CreateTicketRequest`** | Required `@NotBlank` title (max 200) and description (max 5000); optional `@Size` assignee; optional priority (service defaults to `MEDIUM`) |
| **`UpdateTicketRequest`** | Optional title, description, priority; `assignee` + `assigneePresent` so the API can clear assignee (`assigneePresent=true`, blank → `null`) without a `status` field |
| **`UpdateTicketStatusRequest`** | `@NotNull` target `status` |
| **`TicketResponse`** | Full ticket for API responses |

### Service (`TicketService`)
- **`create`** — trim strings; `OPEN`; default `MEDIUM`; normalize assignee  
- **`getById`** — `ResourceNotFoundException` if missing  
- **`listTickets(Pageable)`** — paged `TicketResponse` (search/filters come later)  
- **`updateTicket`** — only title, description, priority, assignee; throws `NoFieldsToUpdateException` when nothing to patch  
- **`updateStatus`** — uses `TicketStatusTransitionPolicy`; idempotent same-state (no `save`, no `updatedAt` bump)

### Other
- **`TicketMapper`** — entity → `TicketResponse`  
- **`ResourceNotFoundException`**, **`NoFieldsToUpdateException`**  
- **`TicketStatusTransitionPolicy`** — `@Component` for injection  

### Unit tests (`TicketServiceTest`)
Covers create (default priority, trim), get not found, list, field update on a `CLOSED` ticket (status unchanged), assignee clear, empty patch, update not found, allowed status change, idempotent status (no save), forbidden transition, status update not found.

Run service tests only:
```powershell
cd backend
.\mvnw.cmd test -Dtest=TicketServiceTest
```

**Note:** `assigneePresent` is for the controller layer when mapping JSON (`"assignee": null` vs omitting the key). Rejecting `status` on general PATCH belongs in the controller (Task 5+); `UpdateTicketRequest` has no `status` field, so the service cannot change status through `updateTicket`.

---

**User**

Task 5: add comments. A user can add a comment to an existing ticket and list the comments for a ticket. Validate that the comment text isn't blank and has a sensible maximum length, and return a proper not-found error if the ticket doesn't exist. Write the service tests for it.

---

**Cursor**

Implementing comment DTOs, service, repository pagination, and unit tests.




Task 5 (comments) is implemented at the service layer with DTO validation and Mockito tests.

### Added

| Piece | Purpose |
|--------|---------|
| **`CreateCommentRequest`** | `@NotBlank` + `@Size(max = 2000)` on `body` (api-contract §2) |
| **`CommentResponse`** | `id`, `ticketId`, `body`, `createdAt` |
| **`CommentMapper`** | Entity → response |
| **`CommentService`** | `addComment`, `listComments` |
| **`CommentRepository`** | `Page<Comment> findByTicket_IdOrderByCreatedAtAscIdAsc(Long, Pageable)` for paginated lists |

### Behavior

- **`addComment`** — Loads ticket or throws `ResourceNotFoundException` (`Ticket not found with id: {id}`); trims body; appends comment; **does not** change ticket `status`.
- **`listComments`** — Same not-found check; returns a `Page<CommentResponse>` ordered by `createdAt` asc, then `id` (sort overrides via `Pageable` later in the controller).

Blank/length rules are on the DTO for `@Valid` at the controller; the service trims before persist.

### Service tests (`CommentServiceTest`)

- Add comment when ticket exists (trim, status unchanged)
- Add comment → not found (no repository interaction)
- List comments when ticket exists
- List comments → not found

```powershell
cd backend
.\mvnw.cmd test -Dtest=CommentServiceTest
```

`CommentServiceTest` passed locally. Run `.\mvnw.cmd test` for the full suite when you want. Controllers and global 404 mapping are still for a later task.

---

**User**

Task 6: implement keyword search across title and description (case-insensitive) and status filtering, which should also work in combination with each other, with pagination and a stable sort order such as newest first. Please choose the simplest sound approach, either derived queries, a @Query, or Specifications, and explain your choice in two or three sentences. Write tests that cover keyword only, status only, both together, no results, and an invalid status value.

---

**Cursor**

Implementing JPA Specifications for composable filters and adding repository + service tests.




Fixing test data setup so `created_at` persists for sort assertions.




### Implementation

- **`TicketSpecifications`** — optional keyword (`LOWER(title)` / `LOWER(description)` `LIKE`) and optional `status IN (...)` predicates, combined with `AND`.
- **`TicketRepository`** — now extends `JpaSpecificationExecutor<Ticket>`.
- **`TicketService.listTickets(query, statusTokens, pageable)`** — trims/ignores blank `query`, parses comma-separated status values, builds the spec, returns a paged `TicketResponse`. Invalid status → **`InvalidQueryParameterException`** (`parameter` = `"status"`).
- **Sort** — not hard-coded in the service; callers pass `Pageable` (e.g. `Sort.by(DESC, "createdAt").and(DESC, "id")`) per api-contract defaults.

### Why Specifications?

I used **JPA Specifications** instead of a single `@Query` or derived method names because keyword and status filters are optional and must combine with **AND** without a combinatorial explosion of repository methods. Specs keep each concern in one small predicate and stay easy to extend (e.g. priority/assignee later). A one-off `@Query` with many nullable parameters would work but is harder to read and grow.

### Tests

| Test | Class |
|------|--------|
| Keyword only (case-insensitive) | `TicketSearchRepositoryTest` |
| Status only | `TicketSearchRepositoryTest` |
| Keyword + status (AND) | `TicketSearchRepositoryTest` |
| No matches | `TicketSearchRepositoryTest` |
| Newest-first ordering | `TicketSearchRepositoryTest` |
| Invalid status | `TicketServiceTest` |

```powershell
cd backend
.\mvnw.cmd test "-Dtest=TicketSearchRepositoryTest,TicketServiceTest"
```

Both test classes passed locally. The list endpoint controller should pass `query`, parsed `status`, and a `Pageable` with default `createdAt,desc` (+ `id` tiebreaker when you wire it).

---

**User**

Task 7: now implement the REST controllers and the global exception handler according to spec/api-contract.md and our api-standards rule. Validation failures should return field-level errors, a missing ticket should return 404, and an invalid state transition should return the status code defined in the spec with a clear message. Do not return entities directly. After that, give me a few curl commands I can use to try each endpoint by hand.

---

**Cursor**

Implementing REST controllers and global exception handling per the API contract. Reviewing specs and existing code.




Implementing the API layer: error DTOs, exception handler, request parsing, and controllers.




Task 7 is done: REST API under `/api/v1`, standard error envelope, and DTO-only responses.

### What was added

| Layer | Details |
|--------|---------|
| **Controllers** | `TicketController` — create, list (query + status + pagination), get, PATCH fields, PATCH status; `CommentController` — add + list comments |
| **Errors** | `ApiErrorResponse`, `ApiFieldError`, `GlobalExceptionHandler` — 400 validation/params, 404 `NOT_FOUND`, 409 `INVALID_STATE_TRANSITION`, 500 generic |
| **Support** | `PagedResponse`, `PageableSupport` (page/size/sort validation; default `createdAt` desc + `id` tiebreaker), `UpdateTicketRequestFactory` (PATCH JSON, rejects `status`, supports clearing `assignee`) |
| **Config** | `spring.jackson.deserialization.fail-on-unknown-properties: true`; create rejects `status` via `@Null` on `CreateTicketRequest` |

All tests pass (`.\mvnw.cmd test`).

---

### Manual checks with curl

Start the API first:

```powershell
cd "c:\Users\Hatim Saifee\Work\2026\October\C2-assesment\backend"
.\mvnw.cmd spring-boot:run
```

**Create ticket (201)**

```bash
curl -s -X POST http://localhost:8080/api/v1/tickets ^
  -H "Content-Type: application/json" ^
  -d "{\"title\":\"Cannot reset password\",\"description\":\"User reports expired link.\",\"priority\":\"HIGH\",\"assignee\":\"Ada Lovelace\"}"
```

**List** (search + status + newest first)

```bash
curl -s "http://localhost:8080/api/v1/tickets?query=password&status=OPEN&page=0&size=20&sort=createdAt,desc"
```

**Get by id** (use id from create response)

```bash
curl -s http://localhost:8080/api/v1/tickets/1
```

**PATCH fields** (not status)

```bash
curl -s -X PATCH http://localhost:8080/api/v1/tickets/1 ^
  -H "Content-Type: application/json" ^
  -d "{\"title\":\"Cannot reset password (updated)\",\"assignee\":null}"
```

**PATCH status** (state machine)

```bash
curl -s -X PATCH http://localhost:8080/api/v1/tickets/1/status ^
  -H "Content-Type: application/json" ^
  -d "{\"status\":\"IN_PROGRESS\"}"
```

**Add comment (201)**

```bash
curl -s -X POST http://localhost:8080/api/v1/tickets/1/comments ^
  -H "Content-Type: application/json" ^
  -d "{\"body\":\"Reproduced in staging with Chrome 141.\"}"
```

**List comments**

```bash
curl -s "http://localhost:8080/api/v1/tickets/1/comments?page=0&size=20&sort=createdAt,asc"
```

**Error samples**

```bash
curl -s http://localhost:8080/api/v1/tickets/99999
```

```bash
curl -s -X PATCH http://localhost:8080/api/v1/tickets/1/status ^
  -H "Content-Type: application/json" ^
  -d "{\"status\":\"OPEN\"}"
```

(After the ticket is `CLOSED`, expect **409** with `INVALID_STATE_TRANSITION`.)

```bash
curl -s -X PATCH http://localhost:8080/api/v1/tickets/1 ^
  -H "Content-Type: application/json" ^
  -d "{\"status\":\"IN_PROGRESS\"}"
```

(Expect **400** with a field error on `status`.)

On PowerShell you can use `curl.exe` explicitly or `Invoke-RestMethod` with the same URLs and JSON bodies.

---

**User**

Task 8: write the state machine integration tests with @SpringBootTest and MockMvc, against the real HTTP layer. Every valid transition must succeed and persist, and every invalid one must be rejected with the expected error response, including CLOSED to OPEN, RESOLVED to OPEN, and CANCELLED to OPEN. Also verify that after a rejected transition the ticket's status in the database hasn't changed. Run the whole backend test suite and show me the results.

---

**Cursor**

Writing the full 5×5 MockMvc integration test matrix and running the backend test suite.




### Task 8: State machine integration tests

Added `TicketStatusTransitionIntegrationTest` (`@SpringBootTest` + `@AutoConfigureMockMvc`, `test` profile):

- **25 parameterized cases** — full 5×5 matrix from `test-strategy.md` §6.1 (SM-01–SM-25).
- **Setup** — each case creates a ticket via `POST /api/v1/tickets` and reaches the `from` state using only valid `PATCH .../status` calls (canonical `OPEN → CANCELLED` for `CANCELLED`).
- **Exercise** — `PATCH /api/v1/tickets/{id}/status` with `{"status":"<to>"}`.
- **Assertions**
  - Allowed / idempotent: **200** and DB status matches expected (including persisted transitions).
  - Forbidden: **409**, `error` = `INVALID_STATE_TRANSITION`, message `Cannot transition ticket from {FROM} to {TO}.`
  - After **409**: DB status unchanged (still `from`) — covers **CLOSED→OPEN**, **RESOLVED→OPEN**, **CANCELLED→OPEN** and all other rejected edges.

Run only this class:

```powershell
cd backend
.\mvnw.cmd test -Dtest=TicketStatusTransitionIntegrationTest
```

---

### Full backend test suite results

```
Tests run: 75, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Total time: 18.364 s
```

| Test class | Tests |
|------------|------:|
| `BackendContextSmokeTest` | 1 |
| `TicketStatusTransitionPolicyTest` | 25 |
| `TicketCommentRepositoryTest` | 1 |
| `TicketSearchRepositoryTest` | 5 |
| `CommentServiceTest` | 4 |
| `TicketServiceTest` | 14 |
| **`TicketStatusTransitionIntegrationTest`** | **25** |

**Total: 75 tests, all green.**

