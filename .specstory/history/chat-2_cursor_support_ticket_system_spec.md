# Support ticket system spec
_Exported on 10/26/2026 at 15:05:29 GMT+5:30 from Cursor (3.17.19)_

---

**User**

Time to start the spec phase. Here is the problem in my own words: we need a Support Ticket Management System where a user can create a ticket, list tickets, view ticket details, update the title, description, priority and assignee, add comments, search tickets by keyword, and filter tickets by status. Data must persist in a database and survive restarts. The backend must validate input and the UI must show meaningful errors. Please write spec/requirements.md with functional requirements, non-functional requirements, assumptions, out-of-scope items, and the acceptance criteria written in a testable way. Please don't invent features I didn't ask for, and mark clearly anything you are assuming so I can approve or reject it.

---

**Cursor**

Reviewing project documentation conventions and any existing spec structure before drafting `spec/requirements.md`.


Drafting `spec/requirements.md` from your stated capabilities only, with assumptions clearly labeled for your approval.


Created **`spec/requirements.md`** as the first spec-driven artifact. Here is what it contains and what needs your sign-off.

### Contents

- **Functional requirements (FR-01–FR-24)** — Mapped to your capabilities: create, list, detail, update (title, description, priority, assignee only), comments, keyword search, status filter, persistence, server validation, and readable UI errors.
- **Non-functional requirements (NFR-01–NFR-06)** — Persistence across restarts, REST API, server-side validation, read-after-write consistency, and H2/PostgreSQL profiles (aligned with your repo charter).
- **Assumptions (A-01–A-16)** — Each labeled **pending approval**, including:
  - No auth (open UI)
  - Priority/status enums and **default `OPEN` status with no status updates in v1** (you asked to filter by status but not to change it — called out in §6 and open question #1)
  - Free-text assignee
  - Search on title/description only (not comments)
  - Search + status filter combined with AND
  - No pagination, no comment edit/delete
- **Out of scope** — Auth, status updates, delete, attachments, notifications, etc., with short rationale so nothing is implied.
- **Acceptance criteria (AC-01–AC-19)** — Testable scenarios tied to FR/NFR IDs, including restart persistence and a negative case for search excluding comment-only matches (per A-11).
- **Open questions (§8)** — Five items for you to resolve before API/domain spec.

### What I did not add

No workflows, SLAs, email, roles, ticket deletion, or status editing unless you approve changing assumptions or scope.

Please review **§5 Assumptions** and **§8 Open questions** — especially **A-07** (status read-only vs updatable) and **A-11** (search fields). Tell me what to change and we can move to the next spec artifact (domain model or API contract).

---

**User**

Good. Now write spec/architecture.md. Describe the overall architecture: the Spring Boot backend with its layers, the React frontend, how they communicate, the H2 versus PostgreSQL profile strategy, how configuration and secrets are handled through environment variables, and how CORS is handled in development. Include a simple component diagram in Mermaid. Keep it proportionate, since this is a small system, so please avoid over-engineering such as microservices, message queues or caching.

---

**Cursor**

Reviewing requirements and repo layout so the architecture doc stays aligned.


Created **`spec/architecture.md`**. Summary:

### What it covers

- **Monolith:** One Spring Boot process + React SPA in `backend/` and `frontend/`; no microservices, queues, or caching.
- **Backend layers:** Controller → Service → Repository → DB, with DTOs, domain entities, config, and `@RestControllerAdvice` — aligned with `.cursor/rules/java-springboot.md` and `/api/v1`.
- **Frontend:** Pages, components, API client, types; server-side validation drives error display.
- **Communication:** JSON REST, sequence diagram for create-ticket flow, standard error shape referenced.
- **Profiles:** `dev` → H2 (file-backed noted for restart persistence); `prod` → PostgreSQL via env; same JAR and JPA model.
- **Config & secrets:** `SPRING_*` / datasource env vars, `VITE_API_BASE_URL`, `.env.example` vs gitignored `.env`, no secrets in frontend.
- **CORS:** Dev-only, origin from `CORS_ALLOWED_ORIGINS` (default `localhost:5173`); production prefers same host or explicit origins — not `*` with credentials.

### Diagrams

1. **Component diagram** — Browser → React → Controllers → Services → Repositories → DB.  
2. **Profile diagram** — `dev` / `prod` → H2 vs PostgreSQL.  
3. **Sequence diagram** — Create ticket end-to-end.

### Proportionate extras

Brief note on optional Vite `/api` proxy as dev-only; canonical path is backend CORS when the SPA calls the API directly.

If you want the next spec step, we can add **`spec/openapi.yaml`** or **`docs/api.md`** next to lock the REST contract.

---

**User**

Next is spec/data-model.md. Design the entities for Ticket and Comment, including field names, types, constraints (not null, max lengths), relationships, timestamps (createdAt, updatedAt), the status enum, the priority enum, indexes that would help search and status filtering, and how the assignee should be represented. Because there is no user management in scope, tell me whether you recommend a simple text field for the assignee, and explain the tradeoff. Include an ER diagram in Mermaid.

---

**Cursor**

Drafting the data model spec aligned with `requirements.md` assumptions and the API rules.


Created **`spec/data-model.md`**. Highlights:

### Entities

- **`ticket`** — `id` (BIGINT identity), `title` (200, required), `description` (5000, required), `priority` / `status` (string enums), nullable `assignee` (100), `created_at` / `updated_at` (UTC).
- **`comment`** — `id`, `ticket_id` (FK, `ON DELETE RESTRICT`), `body` (2000, required), `created_at` only (no `updated_at` in v1).

### Enums

- **Status:** `OPEN` (default), `IN_PROGRESS`, `RESOLVED`, `CLOSED` — not user-editable in v1.
- **Priority:** `LOW`, `MEDIUM` (default), `HIGH`.

### Indexes

- `idx_ticket_status` — status filter  
- `idx_ticket_created_at` — list ordering  
- `idx_comment_ticket_id` + `(ticket_id, created_at)` — comment thread  
- Search: case-insensitive `LIKE` on title/description; optional PostgreSQL FTS/trgm noted for later; no assignee index unless you add assignee filtering.

### Assignee

**Recommendation:** nullable **`VARCHAR(100)`** free text (trim; blank → `NULL`). §6 compares that to a `User` FK: fits no user management and simple updates; tradeoffs are typos, inconsistent names, and no integrity vs a proper user directory when you add auth later.

### Diagram

Mermaid **ER diagram** with both entities, FK, and column summary.

Also includes illustrative DDL, JPA-oriented notes (lazy comments on detail), and traceability to requirement IDs. If you want `tickets` / `ticket_comment` as table names instead of `ticket` / `comment`, say so and we can align the spec before migrations.

---

**User**

Now the most important spec: spec/state-machine.md. The backend must enforce these transitions: OPEN to IN_PROGRESS, IN_PROGRESS to RESOLVED, RESOLVED to CLOSED, OPEN to CANCELLED, and IN_PROGRESS to CANCELLED. Everything else must be rejected, for example CLOSED to OPEN, RESOLVED to OPEN and CANCELLED to OPEN. Please document the states, a transition table, a Mermaid state diagram, which HTTP status and error code is returned for an invalid transition, what happens when someone tries to transition to the same state they're already in, and how a status change is handled when it arrives through the general update endpoint versus a dedicated transition endpoint. Give me your recommendation on that last point and explain why.

---

**Cursor**

Checking existing specs and API standards for consistency with the new CANCELLED state and error conventions.


Adding `state-machine.md` and updating `data-model.md` to include `CANCELLED` and align status-change behavior with the state machine.


Created **`spec/state-machine.md`** and aligned **`spec/data-model.md`** and **`spec/requirements.md`** with `CANCELLED` and the new status rules.

### States (5)

`OPEN` (initial) → workflow states → terminals **`CLOSED`** and **`CANCELLED`** (no outbound transitions).

### Allowed transitions (only these 5)

- `OPEN` → `IN_PROGRESS` | `CANCELLED`
- `IN_PROGRESS` → `RESOLVED` | `CANCELLED`
- `RESOLVED` → `CLOSED`

Everything else is rejected (including `CLOSED`→`OPEN`, `RESOLVED`→`OPEN`, `CANCELLED`→`OPEN`, skips like `OPEN`→`CLOSED`, and any move from a terminal state).

### Errors

| Situation | HTTP | `error` code |
|-----------|------|----------------|
| Forbidden transition | **409 Conflict** | **`INVALID_STATE_TRANSITION`** |
| Ticket missing | 404 | `NOT_FOUND` |
| Bad enum / malformed body | 400 | `BAD_REQUEST` |

### Same target as current status

**Recommended:** **`200 OK`** idempotent — return the ticket unchanged; do not bump `updatedAt`. (Strict `409` is documented as an alternative if you want every call to imply a change.)

### General update vs dedicated endpoint

**Recommendation (normative in the spec):**

- **`PATCH /api/v1/tickets/{id}`** — only `title`, `description`, `priority`, `assignee`. If `status` is present → **`400`** with a message to use the status endpoint (no silent ignore).
- **`PATCH /api/v1/tickets/{id}/status`** — only place that runs the state machine.

**Why:** one enforcement path, clear contract, easier table-driven tests, matches your API rules (`PATCH .../status`), and room later for transition comments/audit without bloating generic PATCH.

The spec includes a **transition matrix**, **Mermaid `stateDiagram-v2`**, example **409** JSON, a **test matrix**, and notes that editing title on `CLOSED` tickets is still allowed unless you change that later.

**Sync:** `data-model.md` now includes `CANCELLED` in the enum and DDL check; `requirements.md` A-07 and the status out-of-scope note point at the state machine. Next logical step: **`spec/openapi.yaml`** with both PATCH shapes and `INVALID_STATE_TRANSITION`.

---

**User**

Write spec/api-contract.md following our api-standards rule. Define every endpoint with method, path, request body, response body, status codes and example JSON: create ticket, list tickets with pagination, get ticket by ID, update ticket fields, change ticket status, add a comment, list comments, and search and filter through query parameters. Include the validation rules per field and a couple of example error responses, one for validation failure and one for an invalid state transition.

---

**Cursor**

Drafting the API contract to align with api-standards, the data model, and the state machine.


Created **`spec/api-contract.md`** aligned with `.cursor/rules/api-standards.md` and your other specs.

### Endpoints (7)

| Method | Path | Success |
|--------|------|---------|
| `POST` | `/api/v1/tickets` | `201` + `Location` |
| `GET` | `/api/v1/tickets` | Paginated list + `query`, `status`, `priority`, `assignee`, `page`, `size`, `sort` |
| `GET` | `/api/v1/tickets/{ticketId}` | Full ticket (no embedded comments) |
| `PATCH` | `/api/v1/tickets/{ticketId}` | Title, description, priority, assignee only; `status` → `400` |
| `PATCH` | `/api/v1/tickets/{ticketId}/status` | `{ "status": "..." }` per state machine |
| `POST` | `/api/v1/tickets/{ticketId}/comments` | `201` + `Location` |
| `GET` | `/api/v1/tickets/{ticketId}/comments` | Paginated, default `createdAt,asc` |

### Also documented

- Shared DTOs (`TicketResponse`, `CommentResponse`, pagination envelope)
- **§2** validation table (lengths, enums, create/update/status/comment/query rules)
- Search: case-insensitive substring on title/description; AND with filters
- Status codes per endpoint (`404` `NOT_FOUND`, `409` `INVALID_STATE_TRANSITION`, etc.)
- Example JSON for success bodies and **two required errors**: validation `400` and transition `409`
- Bonus: `404` example and `400` when `status` is sent on general PATCH

**Note:** Filter uses **`assignee`** (string match) to match the free-text data model; api-standards’ `assignedTo=123` example is not used here.

Next step in the workflow is usually **`spec/plan.md`** or implementation tasks—say which you want.

---

**User**

Now spec/ui-flow.md. Describe the screens and user flows for the frontend: a ticket list page with search box and status filter, a create ticket form, a ticket detail page with an edit mode, a status change control that only offers valid next states, and a comments section. For each screen explain loading, empty and error states, and exactly how backend validation errors and state-transition errors should be displayed to the user. A rough ASCII wireframe for each screen is enough.

---

**Cursor**

Created **`spec/ui-flow.md`**. Summary:

### Structure
- **Routes:** `/tickets`, `/tickets/new`, `/tickets/:ticketId`
- **§3 Global rules:** How to show **400** (inline per `errors[].field` + summary banner), **409** (`INVALID_STATE_TRANSITION` only in the **Status** section, then re-fetch), **404**, and **500**/network (dismissible banner + retry)

### Screens (ASCII wireframes each)
1. **List** — search, status filter, table, pagination; loading skeleton; empty vs “no matches”; list `400` handling  
2. **Create** — form fields; submit loading; redirect to detail on success; validation on fields  
3. **Detail** — view + **edit mode** wireframes; status actions; description; comments + compose  

### Status control (§4)
- Labels for all five states  
- **Only valid next actions** (e.g. Open → “Start progress” / “Cancel ticket”; Closed/Cancelled → message, no buttons)  
- Not a full enum dropdown  

### Detail behaviour
- Status changes are **separate** from **Save changes** (PATCH vs PATCH `/status`)  
- Comments: loading/empty/post errors; optional pagination  
- Optional **confirm** for cancel → `CANCELLED` (UI-A-02)  

### Flows
- Create → progress → resolve → close  
- Search/filter → detail  
- Edit while in progress  

**Assumption flagged:** priority filter on list is API-only for now (UI-A-03). Say if you want that on the list wireframe in the spec.

---

**User**

Create spec/test-strategy.md. Based on our testing rules and the other specs, define what gets tested at which level: unit, repository, API integration, and some basic frontend tests. Specifically list the state machine integration test matrix covering every possible from-state and to-state pair, so I can see the full set of valid and invalid combinations. Also list the validation test cases, the search and filter cases, and a persistence-across-restart check and how we'd verify it.

---

**Cursor**

Created **`spec/test-strategy.md`**, aligned with `.cursor/rules/testing.md` and your other specs.

### Levels
- **Unit** — transition policy, services (Mockito), optional exception mappers  
- **Repository** — `@DataJpaTest`: persistence, FK, search/filter queries  
- **API integration** — `@SpringBootTest` + MockMvc + H2: HTTP, JSON errors, full flows  
- **Frontend** — Vitest + RTL: validation UI, status buttons, 409 in status section (FE-01–08)

### State machine matrix (25 pairs)
Full **5×5** grid in **§6.1** plus numbered cases **SM-01 … SM-25**:
- **5** allowed distinct transitions → **200** + status changes  
- **5** same-state → **200** idempotent  
- **15** forbidden → **409** + `INVALID_STATE_TRANSITION`  

Includes setup paths to reach each `from` state using only legal transitions.

### Other suites
- **§7** — Validation: create, patch, status, comment, list query (VAL-C/U/S/M/Q IDs)  
- **§8** — Search/filter: keyword, case, comments excluded, status, priority, assignee, AND with query, pagination, sort (SF-01–13)  
- **§9** — **PERSIST-01**: file-backed H2, two JVM/context lifecycles or manual demo steps; explains why `mem:` and `@Transactional` alone are not enough  

### Gates
All backend/frontend tests green before merge; bug fixes need regression tests per your testing rules.

---

**User**

Please run our review-spec command against everything in the spec folder. Check for inconsistencies between the files, anything in the assessment's acceptance criteria that isn't covered, and any assumptions that sneaked in without being flagged. Just report findings and don't fix anything yet. I want to read them first.

---

**Cursor**

# Spec review report

Per `.cursor/commands/review-spec.md` — **no spec files were modified.**

---

## 1. Summary of findings

The `spec/` set is **largely coherent** on core behaviour: REST shape, validation limits, search/filter semantics, dedicated `/status` endpoint, state machine, UI error mapping, and test matrices align well. The main problems are:

1. **Pagination** — required in API/UI/tests but **out of scope / not required** in `requirements.md`.
2. **Status workflow** — fully specified in `state-machine.md`, `api-contract.md`, and `ui-flow.md`, but **no FR or AC** in `requirements.md` for changing status or handling `409` in the UI.
3. **Several API filters and behaviours** appear without a matching requirement or flagged assumption (`priority` / `assignee` query params, list payload shape).
4. **Stale or ambiguous lines** in a few places (data-model `updatedAt`, state-machine create `status`, architecture pointer to “later” API doc).
5. **`.cursor/rules/api-standards.md`** still disagrees with `api-contract.md` on assignee filtering (`assignedTo` vs `assignee`).

**Verdict:** Good enough to implement with discipline, but **`requirements.md` should be reconciled** with pagination, status workflow, and optional filters before treating requirements as the single acceptance baseline for the assessment.

---

## 2. Contradictions & inconsistencies

| # | Issue | Documents |
|---|--------|-----------|
| C-01 | **Pagination required vs not required.** `requirements.md` A-15 says pagination not required; §6 lists “Formal pagination, sorting controls” as **out of scope**. `api-contract.md` §3.2, `ui-flow.md` §5, `test-strategy.md` SF-12, and `.cursor/rules/api-standards.md` require `page`/`size`/envelope and UI pagination. | `requirements.md` vs `api-contract.md`, `ui-flow.md`, `test-strategy.md`, `api-standards.md` |
| C-02 | **Invalid status filter: reject vs ignore.** FR-22 allows “reject **or** ignore”; `api-contract.md` §2 / §3.2 mandates **400** on invalid `status` tokens. `ui-flow.md` §3.5 assumes banner + reset. Not wrong if “documented,” but FR-22 and A-12 never record the chosen behaviour. | `requirements.md` FR-22 vs `api-contract.md` |
| C-03 | **Multi-value status filter.** FR-21 / AC-16 read like a **single** selected status; `api-contract.md` allows `status=OPEN,IN_PROGRESS`. UI wireframe shows one “Status: All” control without defining multi-select. | `requirements.md` vs `api-contract.md` vs `ui-flow.md` |
| C-04 | **`updatedAt` on status change.** `data-model.md` §4.2 still says `updatedAt` changes on assignee/title updates “and on any **future** status change **if added to scope**.” Status changes are in scope via `state-machine.md` §9 (`updatedAt` on successful transition). | `data-model.md` vs `state-machine.md` |
| C-05 | **Create with `status` in body.** `state-machine.md` §9: “reject … **or ignore** with 400 if sent” (mixed wording). `api-contract.md` §2 / §3.1: **400** with field error. | `state-machine.md` vs `api-contract.md` |
| C-06 | **Assignee filter naming.** `api-standards.md`: `?assignedTo=123` (numeric). `api-contract.md` §2: `assignee` string exact match. `data-model.md` §7 says not to index assignee unless filter is a requirement—but API contract already exposes the filter. | `api-standards.md` vs `api-contract.md` / `data-model.md` |
| C-07 | **Architecture API doc pointer.** `architecture.md` §3.2 still says endpoints will be detailed in “later `spec/openapi` or `docs/api.md`”; **`spec/api-contract.md` exists** and is normative. | `architecture.md` vs `api-contract.md` |
| C-08 | **State machine transition grid (§4).** Columns are only non-`OPEN` targets; transitions **to `OPEN`** are forbidden but not visible in the 4-column grid (test-strategy §6.1 5×5 matrix is the complete picture). Risk of someone using §4 only and missing “→ OPEN” cases. | `state-machine.md` §4 vs `test-strategy.md` §6 |

---

## 3. Missing edge cases (not or barely specified)

| # | Scenario | Gap |
|---|----------|-----|
| E-01 | **Concurrent status transitions** (two tabs, stale UI) | `ui-flow.md` §3.2 mentions re-fetch after 409; no API idempotency/versioning (acceptable for v1, but not written in requirements). |
| E-02 | **Invalid `ticketId` format** (non-numeric, negative) | No spec for 400 vs 404 on `GET/PATCH /tickets/{id}`. |
| E-03 | **`PATCH` with only unknown JSON properties** | `api-contract.md` §3.1: unknown fields “ignored or rejected per implementation”—not pinned. |
| E-04 | **`PATCH` empty `{}`** | `api-contract.md` §3.4 requires at least one field → 400; `test-strategy.md` VAL-U-07 allows “or documented behaviour”—duplicate ambiguity. |
| E-05 | **List `GET` with invalid `assignee`** | No 400 rules; empty ignored only—fine, but no test case in §8 (unlike `status`/`priority`). |
| E-06 | **Comments `GET` when ticket exists but page out of range** | Standard Spring behaviour unspecified (empty page vs 400). |
| E-07 | **Editing ticket in terminal states** | Allowed in `state-machine.md` §10 and `ui-flow.md` UI-A-04; not in original problem statement—only flagged in UI assumptions, not in `requirements.md`. |
| E-08 | **H2 in-memory dev** | NFR-05 allows in-memory H2; AC-02 / `test-strategy.md` §9 require **file** (or durable) store for restart proof—dev profile choice affects whether AC-02 is passable without config discipline. |

---

## 4. Ambiguities & clarifications needed

| # | Topic | Notes |
|---|--------|------|
| A-01 | **Open questions §8** | Q2 (search comments), Q3 (assignee), Q4 (enums), Q5 (auth) still open while A-08, A-11, A-07 largely decide defaults—readers may not know what is “approved.” |
| A-02 | **`TicketSummary` = full `TicketResponse`** including long `description` on every list row | Performance/display not required by FR-05 (min id + title); acceptable but unstated as assumption. |
| A-03 | **List status UI** | Single select vs multi vs “All”; mapping to comma-separated `status` param undefined. |
| A-04 | **Priority enum case** | A-06 defers case handling to API spec; `api-contract.md` says case-sensitive JSON only. |
| A-05 | **CORS vs Vite proxy** | `architecture.md` names CORS as canonical; proxy optional—implementers must pick one path for local dev. |
| A-06 | **`error` code for 404** | Contract uses `NOT_FOUND`; api-standards example uses `BAD_REQUEST` only—409/404 codes not fully enumerated in rules. |
| A-07 | **Table name `ticket` vs resource `tickets`** | `data-model.md` allows renaming; no final lock-in for migrations. |

---

## 5. Gaps against acceptance criteria (`requirements.md` §7)

| AC | Coverage in other specs | Gap |
|----|-------------------------|-----|
| AC-01–03 | API + UI + tests | OK |
| AC-02 | `test-strategy.md` §9 (PERSIST-01) | OK if file H2 used; not tied to NFR-05 wording |
| AC-04 | `ui-flow.md` list | **Weak with pagination:** “every persisted ticket appears” conflicts with paged list unless interpreted as “all tickets reachable via pages” |
| AC-05–06 | Detail + 404 UI | OK |
| AC-07–09 | PATCH + validation UI | OK |
| AC-10–12 | Comments | OK; AC-10 restart covered in test strategy |
| AC-13–17 | Search/filter | OK in API + tests; UI list must call `query` + `status` (ui-flow) |
| AC-18–19 | Global error UI | `ui-flow.md` §3 + `test-strategy.md` FE-* | OK |
| **(none)** | **Status transitions** | **No AC** for: user changes status, valid paths, **409** surfaced in UI, `CANCELLED` in filter. Covered only in `state-machine.md`, `api-contract.md` §3.5, `ui-flow.md` §4/7.6, `test-strategy.md` §6—not in assessment AC list. |
| **(none)** | **Dedicated `/status` endpoint** | Implied by FR-08 note + A-07; not a testable AC |
| **(none)** | **List pagination UX** | Required by downstream specs; **not** in AC despite C-01 |

**Original assessment capabilities (create, list, detail, update fields, comments, search, filter status, persistence, validation, UI errors):** all have at least one AC **except** that **status change** was added after the AC set was written and is **not** reflected in §7.

---

## 6. Assumptions that sneaked in (not in problem statement / not clearly in `requirements.md` §5)

| Item | Where introduced | Flagged? |
|------|------------------|----------|
| **`CANCELLED` state** | User state-machine request; now A-07 | In A-07, not in original §2 scope summary |
| **Mandatory pagination** | `api-contract`, `ui-flow`, api-standards | **Conflicts** with A-15 / §6 out of scope |
| **`priority` query filter** | `api-contract.md` §2–3.2 | **Not** in FR; not in assumptions |
| **`assignee` query filter** | `api-contract.md` | **Not** in FR; differs from api-standards |
| **Default sort `createdAt,desc`** | `api-contract.md` | Not in requirements |
| **Full ticket body on list** | `api-contract.md` `TicketSummary` | Not assumed in A-10 (columns only) |
| **URL-preserved list filters** | `ui-flow.md` §2 | UI-A partial; not in requirements |
| **Cancel confirmation dialog** | `ui-flow.md` UI-A-02 | Flagged in ui-flow only |
| **Edit closed/cancelled tickets** | `state-machine.md` §10, `ui-flow.md` UI-A-04 | Flagged in ui-flow / state-machine, not requirements |
| **TypeScript frontend** | `architecture.md` | A-01 says React/Vite, not TS explicitly |
| **BIGINT vs UUID** | `data-model.md` §4.1 | A-04 still says “e.g. numeric or UUID” |
| **Comment list pagination** | `api-contract.md` §3.7 | FR-14 doesn’t require paginated comments on UI |
| **Idempotent same-state `200`** | `state-machine.md` §7.5 | Not in requirements; tests assume it (SM-01, etc.) |

---

## 7. Recommended revisions (for when you approve edits)

1. **`requirements.md`:** Resolve C-01—either adopt pagination + sort as in-scope (new FR/AC) or simplify API/UI specs to match A-15.  
2. **`requirements.md`:** Add FR + AC for **status transitions** (happy paths, 409, UI status section) and mention `CANCELLED` in §2 scope summary.  
3. **`requirements.md`:** Close or update §8 open questions; align FR-21/FR-22 with api-contract (single vs multi status, reject vs ignore).  
4. **`requirements.md`:** Clarify AC-04 for paginated lists.  
5. **`data-model.md` §4.2:** State that `updatedAt` updates on successful **status** transition (remove “future / if added”).  
6. **`state-machine.md` §9:** Create body — **reject only** with 400 (drop “ignore”).  
7. **`architecture.md` §3.2:** Point to `spec/api-contract.md`.  
8. **`.cursor/rules/api-standards.md`:** Align assignee filter with `assignee` string (or document exception).  
9. **`api-contract.md`:** Pin unknown JSON fields and empty PATCH behaviour; optional note on list `TicketSummary` vs summary DTO.  
10. **`ui-flow.md`:** Define status filter control vs multi-value API; optional priority filter or explicitly out of UI scope.  
11. **`state-machine.md` §4:** Add a footnote or column for transitions **to `OPEN`** (all forbidden) or defer explicitly to test-strategy 5×5 matrix.

---

I have **not** changed any files. Tell me which of the recommended revisions you want applied (all, a subset, or your own decisions on pagination and status ACs), and I can update the specs accordingly.

---

**User**

Thanks for the review. I agree with most findings, except for the ones I'll mention here: [write which findings you reject and why]. Please update the specs accordingly. After that, create spec/plan.md with a numbered task list for implementation, in small vertical steps, each with a clear definition of done and the tests that go with it. Order them so we can run and verify something after every task. Don't start implementing.

---

**Cursor**

No specific rejections were provided (placeholder left empty). Applying the review's recommended spec revisions and creating `spec/plan.md`. Updating spec files and aligning `.cursor/rules/api-standards.md` with the API contract.


---

**User**

Thanks for the review. I agree with most findings, except for the ones I'll mention here: [write which findings you reject and why]. Please update the specs accordingly. After that, create spec/plan.md with a numbered task list for implementation, in small vertical steps, each with a clear definition of done and the tests that go with it. Order them so we can run and verify something after every task. Don't start implementing.

---

**Cursor**

Checking spec folder state and finishing remaining updates plus `spec/plan.md`.


You left the placeholder **`[write which findings you reject and why]`** empty, so I treated that as **no rejected findings** and applied the full spec-review revision set. If you meant to exclude something, say which item (e.g. keep pagination optional, drop API `priority` filter) and we can adjust.

### Spec updates (review alignment)

| File | Changes |
|------|---------|
| **`requirements.md`** (v0.2) | Pagination in scope (FR-04, A-15); FR-25–27 + AC-20–22 for status; FR-21/22 aligned with API; NFR-05 file H2 dev; A-17–19; AC-04 pagination wording; §8 resolved decisions; removed pagination from out-of-scope |
| **`data-model.md`** | `updatedAt` includes status transitions; idempotent same-state note |
| **`state-machine.md`** | Footnote for →`OPEN` + link to 5×5 matrix; create `status` → 400 only |
| **`architecture.md`** | Points to `api-contract.md` |
| **`api-contract.md`** | A-18 note; unknown JSON + empty PATCH → 400; path ID note; traceability FR-25–27 |
| **`ui-flow.md`** | Single-select status filter; A-17/A-19; traceability AC-20–22 |
| **`test-strategy.md`** | VAL-U-07 pinned; AC-20–22 trace |
| **`.cursor/rules/api-standards.md`** | `assignee` query param (not `assignedTo`) |

### New: **`spec/plan.md`**

**21 ordered tasks** — backend skeleton → schema → errors → state machine unit → create/get/PATCH/status → list → search/filter → comments → dev H2/CORS → restart test → frontend → list → create → detail → edit → status UI → comments → polish.

Each task has **goal**, **work**, **DoD**, **tests** (mapped to `test-strategy.md` IDs), and **how to verify**. Includes a dependency diagram and AC→task matrix. **No implementation started.**

Next step when you’re ready: **Task 1** (backend skeleton). If any review finding should *not* have been applied, list it and we’ll revert or adjust before coding.

