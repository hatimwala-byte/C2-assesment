# Support Ticket Management System — REST API Contract

**Document status:** Approved (synced with implementation)  
**Version:** 0.2  
**Last updated:** 2026-10-26  
**Base URL:** `/api/v1`  
**Related:** [.cursor/rules/api-standards.md](../.cursor/rules/api-standards.md), [data-model.md](./data-model.md), [state-machine.md](./state-machine.md)

## 1. Conventions

| Topic | Rule |
|-------|------|
| **Prefix** | All paths start with `/api/v1`. |
| **Format** | JSON request and response bodies; `Content-Type: application/json`. |
| **IDs** | `ticketId` and `commentId` are positive integers (`long`). |
| **Timestamps** | ISO-8601 UTC instants (e.g. `"2026-10-26T09:30:00.000Z"`). |
| **Enums** | Case-sensitive strings: `TicketStatus`, `TicketPriority` (see §2). |
| **Auth** | None in v1. |

### 1.1 Shared types

**`TicketStatus`:** `OPEN` \| `IN_PROGRESS` \| `RESOLVED` \| `CLOSED` \| `CANCELLED`

**`TicketPriority`:** `LOW` \| `MEDIUM` \| `HIGH`

**`TicketResponse`** (full ticket):

```json
{
  "id": 42,
  "title": "Cannot reset password",
  "description": "User reports expired link after SSO change.",
  "priority": "HIGH",
  "status": "OPEN",
  "assignee": "team-billing",
  "createdAt": "2026-10-26T09:00:00.000Z",
  "updatedAt": "2026-10-26T09:00:00.000Z"
}
```

`assignee` is `null` when unassigned.

**`TicketSummary`** (list rows): same fields as `TicketResponse` in v1 (no embedded comments). Per requirements A-18, including `description` on list rows is allowed for v1 simplicity.

**`CommentResponse`:**

```json
{
  "id": 7,
  "ticketId": 42,
  "body": "Reproduced in staging.",
  "createdAt": "2026-10-26T10:15:00.000Z"
}
```

**Paginated envelope** (tickets and comments):

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "last": true
}
```

### 1.2 Standard error body

All non-2xx responses use:

| Field | Type | Description |
|-------|------|-------------|
| `timestamp` | string | ISO-8601 instant |
| `status` | number | HTTP status |
| `error` | string | Machine code (e.g. `BAD_REQUEST`, `NOT_FOUND`, `INVALID_STATE_TRANSITION`) |
| `message` | string | Human-readable summary |
| `errors` | array | Field errors for validation; empty or omitted otherwise |

Field error object: `field`, `rejectedValue` (optional), `message`.

---

## 2. Field validation rules

Applied on create and update (when a field is present in the body). Strings are trimmed before validation.

| Field | Create (`POST /tickets`) | Update (`PATCH /tickets/{id}`) | Status (`PATCH /tickets/{id}/status`) | Comment (`POST .../comments`) |
|-------|--------------------------|--------------------------------|----------------------------------------|-------------------------------|
| `title` | Required; `@NotBlank`; max **200** | Optional; if present: `@NotBlank`; max **200** | — | — |
| `description` | Required; `@NotBlank`; max **5000** | Optional; if present: `@NotBlank`; max **5000** | — | — |
| `priority` | Optional; default **`MEDIUM`**; must be `LOW` \| `MEDIUM` \| `HIGH` | Optional; if present: valid enum | — | — |
| `assignee` | Optional; max **100**; blank → `null` | Optional; max **100**; blank → `null` | — | — |
| `status` | **Must not** appear; if sent → **400** | **Must not** appear; if sent → **400** | Required; valid `TicketStatus`; transition rules in [state-machine.md](./state-machine.md) | — |
| `body` | — | — | — | Required; `@NotBlank`; max **2000** |

**Query parameters (list tickets):**

| Parameter | Validation |
|-----------|------------|
| `page` | Integer ≥ 0; default `0` |
| `size` | Integer 1–**100**; default `20` |
| `sort` | `createdAt,asc` or `createdAt,desc` (default **`createdAt,desc`**); unknown fields → **400** |
| `query` | Optional; empty/whitespace ignored (no text filter) |
| `status` | Optional; comma-separated `TicketStatus` values; empty ignored; any invalid token → **400** |
| `priority` | Optional; comma-separated `TicketPriority`; empty ignored; invalid → **400** |
| `assignee` | Optional; exact match on stored assignee string (case-sensitive); empty ignored |

**Search semantics:** If `query` is non-empty after trim, tickets match when **case-insensitive** substring matches **title** or **description** (not comment bodies). Combined with `status` / `priority` / `assignee` using logical **AND**.

**Request body rules (all endpoints):**

- Unknown JSON properties in request bodies → **400 Bad Request** (do not silently ignore).
- `PATCH /tickets/{id}` with `{}` or no recognized fields → **400** (“At least one field must be provided”).

**Path IDs:** Non-numeric or non-positive `ticketId` / `commentId` → **400** (malformed id) or **404** if the implementation treats unknown ids uniformly as not found; pick one behaviour in implementation and test consistently (recommended: **404** for unknown positive ids, **400** for malformed path segments).

---

## 3. Endpoints

### 3.1 Create ticket

| | |
|--|--|
| **Method / path** | `POST /api/v1/tickets` |
| **Description** | Creates a ticket with `status` = `OPEN`. |

**Request body:**

```json
{
  "title": "Cannot reset password",
  "description": "User reports expired link after SSO change.",
  "priority": "HIGH",
  "assignee": "Ada Lovelace"
}
```

| Field | Required |
|-------|----------|
| `title` | Yes |
| `description` | Yes |
| `priority` | No (defaults to `MEDIUM`) |
| `assignee` | No |

**Response:** `201 Created`

**Headers:** `Location: /api/v1/tickets/{id}`

**Body:** `TicketResponse` (status always `OPEN`).

**Status codes:**

| Code | When |
|------|------|
| `201` | Created |
| `400` | Validation failure; unknown JSON properties; `status` in body → field error |
| `500` | Unexpected server error |

---

### 3.2 List tickets (pagination, search, filter)

| | |
|--|--|
| **Method / path** | `GET /api/v1/tickets` |
| **Description** | Paginated ticket list with optional keyword search and filters. |

**Query parameters:** `page`, `size`, `sort`, `query`, `status`, `priority`, `assignee` (see §2).

**Example:**  
`GET /api/v1/tickets?query=password&status=OPEN,IN_PROGRESS&page=0&size=20&sort=createdAt,desc`

**Response:** `200 OK`

```json
{
  "content": [
    {
      "id": 42,
      "title": "Cannot reset password",
      "description": "User reports expired link after SSO change.",
      "priority": "HIGH",
      "status": "OPEN",
      "assignee": "team-billing",
      "createdAt": "2026-10-26T09:00:00.000Z",
      "updatedAt": "2026-10-26T09:00:00.000Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

**Status codes:**

| Code | When |
|------|------|
| `200` | Success (possibly empty `content`) |
| `400` | Invalid `page`, `size`, `sort`, `status`, or `priority` |
| `500` | Unexpected server error |

---

### 3.3 Get ticket by ID

| | |
|--|--|
| **Method / path** | `GET /api/v1/tickets/{ticketId}` |
| **Description** | Returns one ticket. Does not include comments (use §3.7). |

**Response:** `200 OK` — `TicketResponse`

**Status codes:**

| Code | When |
|------|------|
| `200` | Found |
| `404` | `error`: `NOT_FOUND`, message e.g. `"Ticket not found."` |
| `500` | Unexpected server error |

---

### 3.4 Update ticket fields

| | |
|--|--|
| **Method / path** | `PATCH /api/v1/tickets/{ticketId}` |
| **Description** | Partial update of `title`, `description`, `priority`, `assignee` only. |

**Request body** (all keys optional; at least one must be present):

```json
{
  "title": "Cannot reset password (updated)",
  "description": "Updated steps to reproduce.",
  "priority": "MEDIUM",
  "assignee": null
}
```

Omitted fields remain unchanged. `assignee: null` clears assignee.

**Response:** `200 OK` — `TicketResponse`

**Status codes:**

| Code | When |
|------|------|
| `200` | Updated |
| `400` | Validation failure; empty body; **`status` present** (use status endpoint) |
| `404` | Ticket not found |
| `500` | Unexpected server error |

**Example `400` when `status` is sent on this endpoint:**

```json
{
  "timestamp": "2026-10-26T10:00:00.000Z",
  "status": 400,
  "error": "BAD_REQUEST",
  "message": "Status cannot be updated via this endpoint.",
  "errors": [
    {
      "field": "status",
      "rejectedValue": "IN_PROGRESS",
      "message": "Use PATCH /api/v1/tickets/{ticketId}/status to change status."
    }
  ]
}
```

---

### 3.5 Change ticket status

| | |
|--|--|
| **Method / path** | `PATCH /api/v1/tickets/{ticketId}/status` |
| **Description** | Applies [state-machine.md](./state-machine.md). Only way to change `status`. |

**Request body:**

```json
{
  "status": "IN_PROGRESS"
}
```

**Response:** `200 OK` — `TicketResponse` with new `status` and updated `updatedAt`.

**Status codes:**

| Code | When |
|------|------|
| `200` | Transition applied, or **idempotent** request (already in target `status`; body unchanged, `updatedAt` not bumped) |
| `400` | Missing/invalid `status` enum |
| `404` | Ticket not found |
| `409` | Illegal transition — `error`: **`INVALID_STATE_TRANSITION`** |
| `500` | Unexpected server error |

**Example `409` (invalid transition):**

```json
{
  "timestamp": "2026-10-26T10:05:00.000Z",
  "status": 409,
  "error": "INVALID_STATE_TRANSITION",
  "message": "Cannot transition ticket from CLOSED to OPEN.",
  "errors": []
}
```

---

### 3.6 Add comment

| | |
|--|--|
| **Method / path** | `POST /api/v1/tickets/{ticketId}/comments` |
| **Description** | Appends a comment to the ticket. Does not change ticket `status`. |

**Request body:**

```json
{
  "body": "Reproduced in staging with Chrome 141."
}
```

**Response:** `201 Created`

**Headers:** `Location: /api/v1/tickets/{ticketId}/comments/{commentId}`

**Body:** `CommentResponse`

**Status codes:**

| Code | When |
|------|------|
| `201` | Created |
| `400` | Validation failure (e.g. blank `body`) |
| `404` | Ticket not found |
| `500` | Unexpected server error |

---

### 3.7 List comments

| | |
|--|--|
| **Method / path** | `GET /api/v1/tickets/{ticketId}/comments` |
| **Description** | Paginated comments for a ticket, oldest first by default. |

**Query parameters:**

| Parameter | Default | Notes |
|-----------|---------|--------|
| `page` | `0` | ≥ 0 |
| `size` | `20` | 1–100 |
| `sort` | `createdAt,asc` | Only `createdAt,asc` or `createdAt,desc` supported |

**Example:** `GET /api/v1/tickets/42/comments?page=0&size=20`

**Response:** `200 OK`

```json
{
  "content": [
    {
      "id": 7,
      "ticketId": 42,
      "body": "Reproduced in staging.",
      "createdAt": "2026-10-26T10:15:00.000Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

**Status codes:**

| Code | When |
|------|------|
| `200` | Success (empty `content` if no comments) |
| `400` | Invalid pagination/sort |
| `404` | Ticket not found |
| `500` | Unexpected server error |

---

## 4. Example error responses

### 4.1 Validation failure (`400 Bad Request`)

`POST /api/v1/tickets` with empty title:

```json
{
  "timestamp": "2026-10-26T09:12:00.000Z",
  "status": 400,
  "error": "BAD_REQUEST",
  "message": "Validation failed for request payload.",
  "errors": [
    {
      "field": "title",
      "rejectedValue": "",
      "message": "Title must not be blank"
    },
    {
      "field": "description",
      "rejectedValue": "   ",
      "message": "Description must not be blank"
    }
  ]
}
```

### 4.2 Invalid state transition (`409 Conflict`)

`PATCH /api/v1/tickets/42/status` when ticket is `CLOSED` and body requests `OPEN`:

See §3.5 example (`INVALID_STATE_TRANSITION`).

### 4.3 Not found (`404 Not Found`)

```json
{
  "timestamp": "2026-10-26T09:20:00.000Z",
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Ticket not found.",
  "errors": []
}
```

---

## 5. Endpoint summary

| Method | Path | Success |
|--------|------|---------|
| `POST` | `/api/v1/tickets` | `201` + `TicketResponse` |
| `GET` | `/api/v1/tickets` | `200` + paginated `TicketSummary` |
| `GET` | `/api/v1/tickets/{ticketId}` | `200` + `TicketResponse` |
| `PATCH` | `/api/v1/tickets/{ticketId}` | `200` + `TicketResponse` |
| `PATCH` | `/api/v1/tickets/{ticketId}/status` | `200` + `TicketResponse` |
| `POST` | `/api/v1/tickets/{ticketId}/comments` | `201` + `CommentResponse` |
| `GET` | `/api/v1/tickets/{ticketId}/comments` | `200` + paginated `CommentResponse` |

---

## 6. Traceability

| Capability | Endpoint(s) |
|------------|-------------|
| FR-01–03 | `POST /tickets` |
| FR-04–05 | `GET /tickets` |
| FR-06–07 | `GET /tickets/{id}` |
| FR-08–11 | `PATCH /tickets/{id}` |
| FR-25–27 | `PATCH /tickets/{id}/status` + [state-machine.md](./state-machine.md) |
| FR-12–16 | `POST` / `GET .../comments` |
| FR-17–19 | `GET /tickets?query=` |
| FR-20–22 | `GET /tickets?status=` |

Implementation and tests must match this contract; OpenAPI in `spec/` may be generated from this document in a later task.

### 6.1 Implementation notes (PATCH tickets)

- `PATCH /tickets/{id}` parses JSON via `UpdateTicketRequestFactory`: allowed keys `title`, `description`, `priority`, `assignee` only; **unknown properties → 400** (`UnknownRequestPropertyException`).
- Present text fields are trimmed; blank after trim → **400** with field error (`FieldValidationException`).
- `status` in body → **400** with field error directing clients to `PATCH .../status` (not silently ignored).
- Typed create/comment bodies use Jackson `fail-on-unknown-properties: true`; PATCH uses the factory rules above for parity with §2.
