# Support Ticket Management System — Requirements

**Document status:** Approved (synced with implementation)  
**Version:** 0.3  
**Last updated:** 2026-10-26

## 1. Purpose

Define what the Support Ticket Management System must do so that implementation and testing can be traced to explicit, testable requirements. This document reflects only the capabilities described by the product owner unless marked as an **Assumption** pending approval.

## 2. Scope summary

The system allows users to manage support tickets through a user interface backed by a REST API and a persistent database. Users can create tickets, browse and inspect them (with pagination), update selected fields, change ticket **status** only via the defined state machine, add comments, search by keyword, and filter by status. The backend validates input; the UI surfaces validation, illegal status transitions, and other operational errors in a way users can understand.

---

## 3. Functional requirements

### 3.1 Ticket creation

| ID | Requirement |
|----|-------------|
| FR-01 | The system shall allow a user to create a new support ticket. |
| FR-02 | A newly created ticket shall be stored in the database and remain available after application or database process restarts. |
| FR-03 | The system shall reject ticket creation when required input fails validation and shall return a clear indication of which fields failed and why. |

**Fields at creation (see Assumptions):** title, description, priority; optional assignee unless otherwise agreed.

### 3.2 Ticket listing

| ID | Requirement |
|----|-------------|
| FR-04 | The system shall provide a **paginated** way to list support tickets (see [api-contract.md](./api-contract.md)). |
| FR-05 | The ticket list shall include enough information to identify and open a ticket (at minimum: identifier and title; see Assumptions for additional list columns). |

### 3.3 Ticket detail

| ID | Requirement |
|----|-------------|
| FR-06 | The system shall allow a user to open a single ticket and view its full stored details (title, description, priority, assignee, status, and timestamps per Assumptions). |
| FR-07 | The system shall return a clear error when the requested ticket does not exist. |

### 3.4 Ticket update

| ID | Requirement |
|----|-------------|
| FR-08 | The system shall allow a user to update an existing ticket’s **title**, **description**, **priority**, and **assignee**. |
| FR-09 | Updates shall be persisted in the database. |
| FR-10 | The system shall reject invalid updates with field-level validation feedback. |
| FR-11 | The system shall return a clear error when updating a ticket that does not exist. |

**Note:** **Status** is changed only through the state machine ([state-machine.md](./state-machine.md)), not via the general update fields in FR-08.

### 3.5 Comments

| ID | Requirement |
|----|-------------|
| FR-12 | The system shall allow a user to add a comment to an existing ticket. |
| FR-13 | Comments shall be persisted and associated with the ticket. |
| FR-14 | Ticket detail (or an equivalent view) shall show comments for that ticket in a sensible order (see Assumptions). |
| FR-15 | The system shall reject invalid comments (e.g. empty body) with clear validation feedback. |
| FR-16 | The system shall return a clear error when adding a comment to a ticket that does not exist. |

### 3.6 Search

| ID | Requirement |
|----|-------------|
| FR-17 | The system shall allow a user to search tickets by **keyword**. |
| FR-18 | Search results shall include only tickets whose searchable fields match the keyword per the agreed search rules (see Assumptions). |
| FR-19 | An empty or whitespace-only keyword shall be handled predictably (see Assumptions). |

### 3.7 Filter by status

| ID | Requirement |
|----|-------------|
| FR-20 | The system shall allow a user to filter the ticket list (or search results) by **status**. |
| FR-21 | Filtering shall return only tickets whose status is in the selected set (one or more statuses; see Assumptions). |
| FR-22 | Invalid status filter values shall be **rejected** with a clear API error (HTTP 400); see [api-contract.md](./api-contract.md). |

### 3.8 Ticket status transitions

| ID | Requirement |
|----|-------------|
| FR-25 | The system shall allow a user to change a ticket’s **status** only through the transitions defined in [state-machine.md](./state-machine.md). |
| FR-26 | The system shall **reject** illegal status transitions with a clear, user-visible error (HTTP 409 per API contract). |
| FR-27 | The UI shall offer only **valid next statuses** for the current ticket state (no free-form status picker). |

### 3.9 Error presentation (UI)

| ID | Requirement |
|----|-------------|
| FR-23 | When the API returns validation errors, the UI shall display messages that identify the problem in plain language (not raw stack traces or opaque codes alone). |
| FR-24 | When the API returns not-found or other client/server errors relevant to the user’s action, the UI shall show an appropriate message and avoid presenting a broken or empty state without explanation. |

---

## 4. Non-functional requirements

| ID | Requirement |
|----|-------------|
| NFR-01 | **Persistence:** All tickets and comments shall be stored in a relational database; data shall survive normal application restarts. |
| NFR-02 | **API:** Business operations shall be exposed via a REST API consumed by the frontend (HTTP semantics: appropriate status codes and JSON bodies). |
| NFR-03 | **Validation:** Input validation shall occur on the server; client-side validation may supplement but shall not replace server validation. |
| NFR-04 | **Consistency:** After a successful create or update, subsequent reads shall reflect the persisted state. |
| NFR-05 | **Local operability:** The application shall be runnable in a local development setup; the default **`h2`** Spring profile uses **file-backed H2** (`jdbc:h2:file:./data/tickets`) so restart persistence (AC-02) is verifiable when the API is started from `backend/` (see README). |
| NFR-06 | **Deployability:** The application shall support a non-development database profile suitable for PostgreSQL (see Assumptions). |

---

## 5. Assumptions (pending your approval)

Items below are **not** stated in the problem statement; they are proposed defaults. Reject or amend any item before implementation treats it as fixed.

| ID | Assumption |
|----|------------|
| A-01 | **Stack (from repository charter):** Backend Java 21 + Spring Boot; frontend React (Vite); REST API between them. |
| A-02 | **Database profiles:** H2 for local development; PostgreSQL for a production-like profile, with schema managed by the application (e.g. migrations or JPA ddl strategy—decided in a later spec/plan). |
| A-03 | **Single application user / no authentication:** Anyone with access to the UI can perform all operations; there is no login, roles, or per-user ownership. |
| A-04 | **Ticket identifier:** Each ticket has a stable unique **`BIGINT`** surrogate key assigned by the system at creation (see [data-model.md](./data-model.md)). |
| A-05 | **Required fields on create:** `title` and `description` are required; `priority` is required with a default if omitted; `assignee` is optional and may be null/empty. |
| A-06 | **Priority values:** Fixed set: `LOW`, `MEDIUM`, `HIGH`; JSON literals are **case-sensitive**. |
| A-07 | **Status values:** Fixed set: `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `CANCELLED`. New tickets default to `OPEN`. Status changes follow [state-machine.md](./state-machine.md) via a dedicated API endpoint. |
| A-08 | **Assignee:** Free-text name or handle (not a separate “users” directory or foreign key to a user table). |
| A-09 | **Timestamps:** System records `createdAt` and `updatedAt` on tickets; `createdAt` on comments. Display in UI is desirable but format/timezone is implementation detail. |
| A-10 | **List columns:** Besides ID and title, list view shows status, priority, and assignee (if any) for quick scanning. |
| A-11 | **Keyword search scope:** Case-insensitive match against ticket **title** and **description** only (not comment bodies) unless you want comments included. |
| A-12 | **Empty search:** If the user submits an empty/whitespace keyword, the system shows the full list (subject to active status filter) rather than returning an error. |
| A-13 | **Search + filter together:** Status filter and keyword search can be applied together (logical AND). |
| A-14 | **Comments:** Body is required text; no edit or delete of comments in v1; comments ordered by `createdAt` ascending (oldest first). |
| A-15 | **Pagination:** List and comment list APIs use `page` / `size` with the standard envelope in [api-contract.md](./api-contract.md) (defaults: page `0`, size `20`, max size `100`). UI provides next/previous or equivalent. |
| A-16 | **Field length limits:** Enforced server-side per [api-contract.md](./api-contract.md) (title 200, description 5000, comment 2000, assignee 100). |
| A-17 | **Optional list API filters:** `priority` and `assignee` query parameters are supported on `GET /tickets`; the **UI v1** implements **status** and **keyword** only (not priority/assignee filters). |
| A-18 | **List row payload:** List responses may include full ticket fields (including `description`) for simplicity in v1; minimum display per FR-05 remains id + title. |
| A-19 | **Terminal tickets:** Tickets in `CLOSED` or `CANCELLED` may still have title, description, priority, and assignee updated unless product rules change later. |

---

## 6. Out of scope

The following are explicitly **not** required unless added in a later change request:

| Item | Rationale |
|------|-----------|
| User authentication, authorization, roles, audit of “who did what” | Not requested; A-03 assumes open access. |
| Arbitrary **status** changes (bypassing the state machine) | Status is not part of the general ticket update; see [state-machine.md](./state-machine.md). |
| Deleting tickets or comments | Not requested. |
| Editing or deleting comments | Not requested. |
| Attachments, tags, categories, SLA, due dates, watchers | Not requested. |
| Email or push notifications | Not requested. |
| Ticket history / activity log beyond stored comments | Not requested. |
| Bulk operations, import/export | Not requested. |
| Real-time updates (WebSockets) | Not requested. |
| Internationalization (multiple languages) | Not requested. |
| Mobile-native apps | Not requested; web UI only. |
| Advanced query builders, ad-hoc sort UI beyond API `sort` | Not required; API default sort is `createdAt,desc` per contract. |

---

## 7. Acceptance criteria (testable)

Each criterion is written so it can be verified manually or via automated tests. IDs map to functional requirements above.

### Ticket lifecycle

- **AC-01 (FR-01, FR-02):** Given the application is running with an empty database, when a user submits valid create-ticket data, then the API responds with success, the response includes a ticket ID, and a GET for that ID returns the same title, description, priority, and assignee as submitted.
- **AC-02 (FR-02, NFR-01):** Given at least one ticket exists, when the backend process is stopped and started again (and the database volume is unchanged), then listing or fetching that ticket still returns the same data.
- **AC-03 (FR-03, FR-23):** Given create is attempted with missing or invalid required fields, when the user submits the form, then the API returns HTTP 4xx with a structured validation error, and the UI shows which fields failed and a readable message.

### Listing and detail

- **AC-04 (FR-04, FR-05, A-15):** Given more tickets exist than one page holds, when the user opens the ticket list and navigates through pages, then every persisted ticket appears on some page and each row allows navigation to detail.
- **AC-05 (FR-06):** Given a ticket exists, when the user opens its detail view, then title, description, priority, assignee, and status match the database.
- **AC-06 (FR-07, FR-24):** Given no ticket with ID `X` exists, when the user or client requests ticket `X`, then the API returns not-found and the UI shows a clear not-found message (not a generic crash).

### Update

- **AC-07 (FR-08, FR-09):** Given a ticket exists, when the user changes title, description, priority, and assignee to new valid values and saves, then a subsequent detail fetch shows all four fields updated.
- **AC-08 (FR-10):** Given an update with invalid priority or empty title (per validation rules), when the user saves, then the API rejects the request and the UI shows field-level errors; previously stored values remain unchanged.
- **AC-09 (FR-11):** Given a non-existent ticket ID, when an update is attempted, then the API returns not-found and the UI informs the user.

### Comments

- **AC-10 (FR-12, FR-13, FR-14):** Given a ticket exists, when the user adds a valid comment, then the API succeeds, the comment appears on that ticket’s detail view, and after restart the comment is still present.
- **AC-11 (FR-15):** Given an empty comment body, when the user submits, then the API rejects with validation error and the UI displays the error.
- **AC-12 (FR-16):** Given a non-existent ticket ID, when adding a comment, then the API returns not-found and the UI informs the user.

### Search

- **AC-13 (FR-17, FR-18, A-11):** Given tickets with distinct titles/descriptions, when the user searches for a keyword that appears in one ticket’s title or description, then only matching tickets (or that ticket alone) appear in results.
- **AC-14 (A-12):** Given tickets exist, when the user clears the keyword and applies search (or loads list with no keyword), then all tickets matching the current status filter are shown.
- **AC-15 (A-11):** Given a keyword that appears only in a comment body but not in title/description, when the user searches, then that ticket is **not** returned (unless A-11 is changed to include comments).

### Filter by status

- **AC-16 (FR-20, FR-21):** Given tickets in at least two different statuses, when the user filters by one status, then only tickets with that status are shown.
- **AC-17 (A-13):** Given a keyword and a status filter, when both are applied, then only tickets matching **both** conditions appear.

### API and UI error behavior

- **AC-18 (NFR-03, FR-23):** For any form that triggers server validation failure, the UI never treats the operation as successful and displays at least one human-readable message tied to the failure.
- **AC-19 (FR-24):** Given the backend is unreachable or returns 5xx for a user action, when the user retries an operation, then the UI shows a generic failure message without exposing sensitive internals.

### Status transitions

- **AC-20 (FR-25, FR-27):** Given a ticket in `OPEN`, when the user chooses a valid next action (e.g. start progress), then the API returns success and the detail view shows the new status; only valid actions are shown for each state.
- **AC-21 (FR-26, FR-23):** Given an illegal status transition (e.g. after a concurrent change or API call), when the backend returns HTTP 409 with `INVALID_STATE_TRANSITION`, then the UI shows the API `message` in the status area (not on unrelated fields) and the displayed status matches the server after refresh.
- **AC-22 (FR-25):** Given a ticket, when the user completes the path OPEN → IN_PROGRESS → RESOLVED → CLOSED using only allowed transitions, then each step persists and the ticket ends in `CLOSED` with no further status actions offered.

---

## 8. Resolved decisions (formerly open questions)

| Topic | Decision |
|-------|----------|
| Status updates | Yes, via [state-machine.md](./state-machine.md) and `PATCH .../status`. |
| Search includes comments? | **No** (A-11). |
| Assignee | **Free text** (A-08). |
| Enumerations | As in A-06 and A-07. |
| Authentication | **Not required** for v1 (A-03). |

---

## 9. Traceability

Future artifacts should reference requirement IDs (`FR-xx`, `NFR-xx`, `AC-xx`, `A-xx`). API and domain specs in `spec/` should not contradict this document; changes here require explicit approval.
