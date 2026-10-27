# API quick reference

Normative contract: [spec/api-contract.md](../spec/api-contract.md).

**Base URL:** `/api/v1`  
**Content-Type:** `application/json`

## Tickets

| Method | Path | Success | Notes |
|--------|------|---------|--------|
| POST | `/tickets` | 201 | Body: `title`, `description`, optional `priority`, `assignee`. No `status`. |
| GET | `/tickets` | 200 | Query: `query`, `status`, `priority`, `assignee`, `page`, `size`, `sort` (default `createdAt,desc`). |
| GET | `/tickets/{ticketId}` | 200 / 404 | |
| PATCH | `/tickets/{ticketId}` | 200 / 400 / 404 | Partial update; unknown properties → 400; `status` in body → 400 with field error. |
| PATCH | `/tickets/{ticketId}/status` | 200 / 409 | Body: `{ "status": "IN_PROGRESS" }`. Illegal transition → 409 `INVALID_STATE_TRANSITION`. |

## Comments

| Method | Path | Success | Notes |
|--------|------|---------|--------|
| POST | `/tickets/{ticketId}/comments` | 201 | Body: `body` (required, max 2000). |
| GET | `/tickets/{ticketId}/comments` | 200 / 404 | Paginated; default sort `createdAt,asc`. |

## Error envelope (non-2xx)

```json
{
  "timestamp": "2026-10-26T10:00:00.000Z",
  "status": 400,
  "error": "BAD_REQUEST",
  "message": "Validation failed for request payload.",
  "errors": [{ "field": "title", "rejectedValue": "", "message": "must not be blank" }]
}
```

Common `error` codes: `BAD_REQUEST`, `NOT_FOUND`, `INVALID_STATE_TRANSITION`, `INTERNAL_SERVER_ERROR`.
