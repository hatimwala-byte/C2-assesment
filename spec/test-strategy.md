# Support Ticket Management System — Test Strategy

**Document status:** Approved (synced with implementation)  
**Version:** 0.2  
**Last updated:** 2026-10-26  
**Related:** [.cursor/rules/testing.md](../.cursor/rules/testing.md), [api-contract.md](./api-contract.md), [state-machine.md](./state-machine.md), [requirements.md](./requirements.md)

## 1. Purpose

Define **what** is tested at **which level**, how suites are run, and explicit case lists for the state machine, validation, search/filter, and persistence across restart. Implementation uses JUnit 5 on the backend and Vitest (or equivalent) on the frontend unless the plan chooses otherwise.

## 2. Test pyramid (this project)

```
                    ┌─────────────┐
                    │  Frontend   │  Few: critical flows + error mapping
                    │  (Vitest)   │
                ┌───┴─────────────┴───┐
                │  API integration     │  MockMvc + H2: endpoints, HTTP, JSON
                │  (@SpringBootTest)   │
            ┌───┴─────────────────────┴───┐
            │  Repository (@DataJpaTest) │  Queries, mappings, DB constraints
        ┌───┴─────────────────────────────┴───┐
        │  Unit (JUnit + Mockito)              │  State machine policy, services
        └─────────────────────────────────────┘
```

| Level | Scope | Spring context | Database | Primary tools |
|-------|--------|----------------|----------|----------------|
| **Unit** | Pure logic: transition policy, service orchestration with mocked repos | No | No | JUnit 5, Mockito |
| **Repository** | JPA entities, custom `@Query`, enum mapping, CHECK/FK if exercised | `@DataJpaTest` | In-memory or test H2 | Spring Data, AssertJ |
| **API integration** | HTTP contract, validation, status codes, error JSON shape, end-to-end with real web + DB layer | `@SpringBootTest` + `MockMvc` (or `@AutoConfigureMockMvc`) | H2 (test profile) | MockMvc, JSON path |
| **Frontend** | Components/pages: forms, error display, status button visibility | N/A | Mocked `fetch` / API module | Vitest, React Testing Library |

**Not in v1 scope:** E2E browser automation (Playwright/Cypress) unless added later; load/performance tests.

**Conventions:** `given_when_then` or `should_X_when_Y` naming; AAA structure; independent tests; builders/factories for tickets ([testing.md](../.cursor/rules/testing.md)).

---

## 3. Unit tests

### 3.1 Ticket status transition policy

**Class under test:** e.g. `TicketStatusTransitionPolicy` (or equivalent used by `TicketService`).

| Case ID | Description | Expected |
|---------|-------------|----------|
| SM-U-01 | Each of the 5 allowed **distinct** transitions (see §6.2) | `isAllowed(from, to)` → true |
| SM-U-02 | Representative forbidden pairs (subset) | → false |
| SM-U-03 | Same `from` and `to` | Allowed for idempotent handling (service may no-op without policy rejection) |

No database; no HTTP.

### 3.2 Ticket service (Mockito)

| Area | Examples |
|------|----------|
| Create | Sets `OPEN`, default `MEDIUM`, trims assignee, maps to DTO |
| Update | Does not change `status`; updates `updatedAt` on change |
| Status change | Invokes policy; on deny throws domain exception mapped to 409 in web layer |
| Not found | `getById` / update when missing → `ResourceNotFoundException` |

Mock `TicketRepository`, `CommentRepository`.

### 3.3 Error mapping (optional unit)

Map `ResourceNotFoundException` → 404 payload; `InvalidStateTransitionException` → 409 + `INVALID_STATE_TRANSITION`.

---

## 4. Repository tests (`@DataJpaTest`)

| ID | Focus | Assert |
|----|--------|--------|
| REPO-01 | Persist `Ticket` with all enum values including `CANCELLED` | Round-trip fields |
| REPO-02 | `Comment` FK to `ticket` | Save comment; load by `ticketId` |
| REPO-03 | Optional: invalid enum / constraint | DB rejects if CHECK enforced in migrations |
| REPO-04 | **Search query** (if custom query exists) | `query` matches title case-insensitive |
| REPO-05 | **Search query** | `query` matches description |
| REPO-06 | **Search query** | No match when keyword only in comment body |
| REPO-07 | **Filter** | `status` IN clause returns correct subset |
| REPO-08 | **Combined** | `query` + `status` AND semantics |

Use `@Transactional` rollback per test or `@Sql` cleanup.

---

## 5. API integration tests (MockMvc + H2)

**Base:** `@SpringBootTest(webEnvironment = MOCK)` + `@AutoConfigureMockMvc` + test profile (`application-test.yml` with H2).

**Cross-cutting checks (any mutating test):**

- Error body includes `timestamp`, `status`, `error`, `message`.
- Validation failures include `errors[]` with `field` + `message`.

### 5.1 Endpoint smoke (happy path)

| ID | Endpoint | Assert |
|----|----------|--------|
| API-01 | `POST /api/v1/tickets` | `201`, `Location`, body `status=OPEN` |
| API-02 | `GET /api/v1/tickets` | `200`, pagination envelope |
| API-03 | `GET /api/v1/tickets/{id}` | `200`, full ticket |
| API-04 | `PATCH /api/v1/tickets/{id}` | `200`, fields updated |
| API-05 | `PATCH /api/v1/tickets/{id}/status` | `200`, status updated |
| API-06 | `POST /api/v1/tickets/{id}/comments` | `201`, `Location` |
| API-07 | `GET /api/v1/tickets/{id}/comments` | `200`, paginated comments |

### 5.2 State machine suite

Full matrix §6 — class e.g. `TicketStatusTransitionIntegrationTest` with parameterized or generated cases.

### 5.3 Validation suite

§7.

### 5.4 Search & filter suite

§8.

### 5.5 Contract guards

| ID | Case | Expected |
|----|------|----------|
| API-G-01 | `PATCH /tickets/{id}` body includes `status` | `400`, field error on `status` |
| API-G-02 | `POST /tickets` body includes `status` | `400` |
| API-G-03 | `GET /tickets/{missing}` | `404`, `NOT_FOUND` |

---

## 6. State machine integration test matrix

**Endpoint:** `PATCH /api/v1/tickets/{ticketId}/status`  
**Body:** `{ "status": "<TARGET>" }`  
**Setup:** For each **from-state**, create or move a ticket into that state using only **valid** transitions (factory helper).

**States (5):** `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `CANCELLED`  
**Pairs:** 5 × 5 = **25** (complete grid below).

**Expected HTTP:**

| Result | Meaning |
|--------|---------|
| **200** | Allowed transition **or** idempotent same-state ([state-machine.md](./state-machine.md) §7.5) |
| **409** | `error` = `INVALID_STATE_TRANSITION` |
| **400** | Invalid enum in body (separate test VAL-12) |

### 6.1 Full from × to matrix

Legend: **200** = success; **409** = invalid transition; **200*** = idempotent (same state, unchanged).

| From ↓ / To → | `OPEN` | `IN_PROGRESS` | `RESOLVED` | `CLOSED` | `CANCELLED` |
|---------------|--------|---------------|------------|----------|-------------|
| **`OPEN`** | 200* | **200** ✓ | 409 | 409 | **200** ✓ |
| **`IN_PROGRESS`** | 409 | 200* | **200** ✓ | 409 | **200** ✓ |
| **`RESOLVED`** | 409 | 409 | 200* | **200** ✓ | 409 |
| **`CLOSED`** | 409 | 409 | 409 | 200* | 409 |
| **`CANCELLED`** | 409 | 409 | 409 | 409 | 200* |

**✓** = one of the five **distinct-state** allowed transitions (must assert DB `status` changed).

### 6.2 Explicit test case list (integration IDs)

Use one parameterized test: `(from, to, expectedStatus, expectStatusUnchanged)` or 25 rows.

| ID | From | To | HTTP | DB status after |
|----|------|-----|------|-----------------|
| SM-01 | OPEN | OPEN | 200 | OPEN (unchanged) |
| SM-02 | OPEN | IN_PROGRESS | 200 | IN_PROGRESS |
| SM-03 | OPEN | RESOLVED | 409 | OPEN |
| SM-04 | OPEN | CLOSED | 409 | OPEN |
| SM-05 | OPEN | CANCELLED | 200 | CANCELLED |
| SM-06 | IN_PROGRESS | OPEN | 409 | IN_PROGRESS |
| SM-07 | IN_PROGRESS | IN_PROGRESS | 200 | IN_PROGRESS |
| SM-08 | IN_PROGRESS | RESOLVED | 200 | RESOLVED |
| SM-09 | IN_PROGRESS | CLOSED | 409 | IN_PROGRESS |
| SM-10 | IN_PROGRESS | CANCELLED | 200 | CANCELLED |
| SM-11 | RESOLVED | OPEN | 409 | RESOLVED |
| SM-12 | RESOLVED | IN_PROGRESS | 409 | RESOLVED |
| SM-13 | RESOLVED | RESOLVED | 200 | RESOLVED |
| SM-14 | RESOLVED | CLOSED | 200 | CLOSED |
| SM-15 | RESOLVED | CANCELLED | 409 | RESOLVED |
| SM-16 | CLOSED | OPEN | 409 | CLOSED |
| SM-17 | CLOSED | IN_PROGRESS | 409 | CLOSED |
| SM-18 | CLOSED | RESOLVED | 409 | CLOSED |
| SM-19 | CLOSED | CLOSED | 200 | CLOSED |
| SM-20 | CLOSED | CANCELLED | 409 | CLOSED |
| SM-21 | CANCELLED | OPEN | 409 | CANCELLED |
| SM-22 | CANCELLED | IN_PROGRESS | 409 | CANCELLED |
| SM-23 | CANCELLED | RESOLVED | 409 | CANCELLED |
| SM-24 | CANCELLED | CLOSED | 409 | CANCELLED |
| SM-25 | CANCELLED | CANCELLED | 200 | CANCELLED |

**409 assertions:** JSON `error` equals `INVALID_STATE_TRANSITION`; `message` non-empty.

**Idempotent 200 (SM-01, 07, 13, 19, 25):** Optional strict assert: `updatedAt` unchanged from before request (per state-machine spec).

### 6.3 Setup helper (integration)

Reach non-initial states without invalid API calls:

| Target state | Valid path |
|--------------|------------|
| `OPEN` | `POST /tickets` |
| `IN_PROGRESS` | OPEN → `IN_PROGRESS` |
| `RESOLVED` | OPEN → IN_PROGRESS → RESOLVED |
| `CLOSED` | OPEN → IN_PROGRESS → RESOLVED → CLOSED |
| `CANCELLED` | OPEN → CANCELLED **or** OPEN → IN_PROGRESS → CANCELLED (use one canonical path per test class) |

**Unauthorized actor:** N/A (no auth in v1).

---

## 7. Validation test cases

**API integration** unless noted. Field names match [api-contract.md](./api-contract.md) §2.

### 7.1 Create ticket `POST /api/v1/tickets`

| ID | Input | Expected |
|----|--------|----------|
| VAL-C-01 | Valid minimal (title + description) | `201`; `priority` = `MEDIUM` |
| VAL-C-02 | Missing `title` | `400`; error on `title` |
| VAL-C-03 | Blank `title` (spaces) | `400`; `title` |
| VAL-C-04 | `title` length 201 | `400`; `title` |
| VAL-C-05 | Missing `description` | `400`; `description` |
| VAL-C-06 | Blank `description` | `400`; `description` |
| VAL-C-07 | `description` length 5001 | `400`; `description` |
| VAL-C-08 | Invalid `priority` e.g. `URGENT` | `400`; `priority` |
| VAL-C-09 | Valid `priority` each of LOW, MEDIUM, HIGH | `201` |
| VAL-C-10 | `assignee` length 101 | `400`; `assignee` |
| VAL-C-11 | `assignee` blank / whitespace | `201`; `assignee` null in response |
| VAL-C-12 | Body contains `status` | `400`; `status` field error |

### 7.2 Update ticket `PATCH /api/v1/tickets/{id}`

| ID | Input | Expected |
|----|--------|----------|
| VAL-U-01 | Patch `title` only (valid) | `200` |
| VAL-U-02 | `title` blank | `400`; `title` |
| VAL-U-03 | `title` 201 chars | `400` |
| VAL-U-04 | `description` blank when provided | `400` |
| VAL-U-05 | Invalid `priority` | `400` |
| VAL-U-06 | `assignee: null` | `200`; assignee cleared |
| VAL-U-07 | Empty JSON `{}` | `400` (no fields to update) |
| VAL-U-08 | Body contains `status` | `400`; `status` |
| VAL-U-09 | Unknown ticket id | `404` |

### 7.3 Status `PATCH /api/v1/tickets/{id}/status`

| ID | Input | Expected |
|----|--------|----------|
| VAL-S-01 | Missing `status` | `400` |
| VAL-S-02 | `status`: `NOT_A_STATE` | `400` |
| VAL-S-03 | Unknown ticket id | `404` |

(Illegal transitions covered in §6.)

### 7.4 Comment `POST /api/v1/tickets/{id}/comments`

| ID | Input | Expected |
|----|--------|----------|
| VAL-M-01 | Valid `body` | `201` |
| VAL-M-02 | Missing `body` | `400` |
| VAL-M-03 | Blank `body` | `400`; `body` |
| VAL-M-04 | `body` length 2001 | `400`; `body` |
| VAL-M-05 | Unknown ticket id | `404` |

### 7.5 List query validation `GET /api/v1/tickets`

| ID | Input | Expected |
|----|--------|----------|
| VAL-Q-01 | `page=-1` | `400` |
| VAL-Q-02 | `size=0` or `size=101` | `400` |
| VAL-Q-03 | `status=NOT_A_STATE` | `400` |
| VAL-Q-04 | `status=OPEN,BOGUS` | `400` |
| VAL-Q-05 | Invalid `sort` field | `400` |

---

## 8. Search and filter test cases

**Setup:** Seed tickets with distinct titles, descriptions, statuses, priorities, assignees (test `@Sql` or factory).

| ID | Seed gist | Request | Expected tickets |
|----|-----------|---------|------------------|
| SF-01 | A: title "Alpha login"; B: title "Beta" | `query=login` | A only |
| SF-02 | A: description contains "password"; title unrelated | `query=password` | A |
| SF-03 | Keyword only in comment on ticket B | `query=<that word>` | None (empty content) |
| SF-04 | Case mix "LoGiN" in title | `query=login` | Match (case-insensitive) |
| SF-05 | — | `query=` or whitespace | All (subject to other filters) |
| SF-06 | OPEN and CLOSED tickets | `status=OPEN` | OPEN only |
| SF-07 | Mixed statuses | `status=OPEN,IN_PROGRESS` | Union of both |
| SF-08 | — | `status=` omitted | No status filter |
| SF-09 | HIGH and LOW tickets | `priority=HIGH` | HIGH only |
| SF-10 | assignee "alice" vs "bob" | `assignee=alice` | Exact match alice |
| SF-11 | A matches query + OPEN; B matches query + CLOSED | `query=foo&status=OPEN` | A only (AND) |
| SF-12 | Pagination: 25 tickets, `size=10` | `page=0`, `page=1` | 10 + 10; `totalElements=25` |
| SF-13 | Default sort | `sort=createdAt,desc` | Newest first (assert order) |

Repository-level duplicates for SF-01, 02, 06, 11 if query lives in custom JPA (REPO-04–08).

---

## 9. Persistence across restart

**Requirement:** [requirements.md](./requirements.md) AC-02, NFR-01 — data survives application restart when using durable storage.

### 9.1 What we are proving

Tickets and comments written to **file-backed H2** (or PostgreSQL) remain readable after the JVM process stops and a new instance starts against the **same database files/URL**.

### 9.2 Automated approach (recommended for CI optional / local mandatory)

| Step | Action |
|------|--------|
| 1 | Start app with **dev-like profile** pointing H2 to a **fixed file path** in `target/persistence-test` or temp dir (e.g. `jdbc:h2:file:./target/persistence-test/tickets`). |
| 2 | `POST /tickets` → save `ticketId`; `POST .../comments` → save `commentId`. |
| 3 | Record title, description, status, comment body. |
| 4 | **Stop** Spring context (`SpringApplication.exit` or separate process kill in script test). |
| 5 | **Start** new context with **same JDBC URL** (no drop). |
| 6 | `GET /tickets/{ticketId}` → assert all fields match step 3. |
| 7 | `GET /tickets/{ticketId}/comments` → assert comment present. |

**Test type:** Dedicated `@SpringBootTest` with **two application context lifecycles** in one test class, or a **manual/scripted** check documented in `docs/` if full restart in JUnit is awkward.

**ID:** `PERSIST-01`

### 9.3 Manual verification (assessment / demo)

Document in README or `docs/`:

1. Run backend with file H2 (`SPRING_PROFILES_ACTIVE=dev`, file URL in config).
2. Create ticket + comment via UI or curl.
3. Stop backend (Ctrl+C).
4. Start backend again (same working directory so file path unchanged).
5. Open ticket in UI — data still visible.

**Pass criteria:** Same as AC-02.

### 9.4 What not to use for PERSIST-01

- In-memory H2 `jdbc:h2:mem:` — data lost on disconnect; fine for default integration tests, **not** for restart test.
- Single `@Transactional` integration test without restart — does not prove durability.

---

## 10. Frontend tests (basic)

**Runner:** Vitest + React Testing Library; MSW or vi.mock on API module.

**Tooling:** Vitest config lives in `frontend/vite.config.ts`; use `defineConfig` from `vitest/config` so `npm run build` (`tsc -b`) accepts the `test` block.

| ID | Component / flow | Assert |
|----|------------------|--------|
| FE-01 | Create form | Submit sends POST; `400` maps `errors.title` to inline text |
| FE-02 | Create form | Summary banner shows API `message` on validation failure |
| FE-03 | Ticket detail status section | When status `OPEN`, buttons for IN_PROGRESS and CANCELLED only |
| FE-04 | Ticket detail status section | When `CLOSED`, no transition buttons; terminal message |
| FE-05 | Ticket detail | `409` response shows alert text from `message` in status section (not on title field) |
| FE-06 | List | Empty state copy when `content.length === 0` |
| FE-07 | List | Loading indicator while fetch pending |
| FE-08 | Comment form | `400` on body shows inline error |

No snapshot-heavy full-page tests; focus on behaviour in [ui-flow.md](./ui-flow.md) §3.

---

## 11. Execution & gates

| Command | Suite |
|---------|--------|
| `cd backend && mvn test` | Unit + repository + integration (80 tests) |
| `cd frontend && npm test` | Vitest (hook/component tests; FE-01–08 partially covered) |
| `cd frontend && npm run build` | `tsc -b` + Vite production bundle (required CI gate alongside `npm test`) |

**Gate:** All tests green before merge; every bug fix adds a regression test ([testing.md](../.cursor/rules/testing.md)).

---

## 12. Traceability

| Spec | Test sections |
|------|----------------|
| [state-machine.md](./state-machine.md) | §6 |
| [api-contract.md](./api-contract.md) | §5, §7, §8 |
| [requirements.md](./requirements.md) AC-02 | §9 |
| AC-20–22 (status UI/API) | §5–6, FE-03–05 |
| [ui-flow.md](./ui-flow.md) | §10 |
