# Support Ticket Management System — UI Flows & Screens

**Document status:** Approved (synced with implementation)  
**Version:** 0.2  
**Last updated:** 2026-10-26  
**Related:** [api-contract.md](./api-contract.md), [state-machine.md](./state-machine.md), [requirements.md](./requirements.md)

## 1. Purpose

Describe frontend screens, navigation, and user-visible behaviour—including loading, empty, and error states—and how API errors map to the UI. Implementation is React (Vite); routing is client-side (e.g. React Router).

---

## 2. Routes & navigation

| Route | Screen | Entry |
|-------|--------|--------|
| `/tickets` | Ticket list | App home / nav link “Tickets” |
| `/tickets/new` | Create ticket | “New ticket” on list |
| `/tickets/:ticketId` | Ticket detail | Row click / link from list |

**Back navigation:** Detail → “Back to tickets” → `/tickets` (preserve list filters in URL query when possible, e.g. `/tickets?query=foo&status=OPEN`).

**No authentication** in v1; no login screen.

---

## 3. Global error presentation rules

All messages shown to users must come from API `message` and `errors[].message` when available—never raw stack traces or status codes alone.

### 3.1 Validation errors (`400`, `error`: `BAD_REQUEST`)

| Source | UI behaviour |
|--------|----------------|
| `errors[]` with `field` | **Inline:** show `message` adjacent to the matching form control (`title`, `description`, `priority`, `assignee`, `body`). Highlight control border/outline as invalid. |
| Top-level `message` | **Summary banner** at top of the form (or form section): e.g. “Validation failed for request payload.” |
| Unknown `field` | Show in summary banner as bullet list. |
| Multiple errors on one field | Show all messages for that field (stacked or combined). |

Client-side checks (optional) may mirror required/max length before submit; **server response always wins** on conflict.

### 3.2 State transition errors (`409`, `error`: `INVALID_STATE_TRANSITION`)

| Rule | UI behaviour |
|------|----------------|
| No field mapping | Do **not** attach to title/description fields. |
| Display | **Alert in the Status section** only: show API `message` (e.g. “Cannot transition ticket from CLOSED to OPEN.”). |
| Persistence | Leave ticket fields and comments unchanged; refresh displayed `status` from last successful fetch if needed. |
| After race | If UI offered a transition that backend rejects (stale state), show the 409 message and **re-fetch** ticket to sync `status`. |

### 3.3 Not found (`404`, `error`: `NOT_FOUND`)

| Context | UI behaviour |
|---------|----------------|
| Ticket detail / comments | **Full-page or in-content message:** API `message` or “Ticket not found.” + link to list. |
| List fetch N/A | — |

### 3.4 Network / server errors (`500`, network offline)

| UI behaviour |
|--------------|
| **Dismissible banner** at top of current screen: generic text e.g. “Something went wrong. Please try again.” Use API `message` if present and safe. |
| Retry: primary action on list (“Retry”), or user re-submits form. |

### 3.5 List query errors (`400` on `GET /tickets`)

Invalid `status` / `sort` / pagination: banner on list + reset filter to last valid or defaults; do not leave a silent empty table.

---

## 4. Status labels & allowed next states (UI)

Display status as human-readable labels; send API enum values on transition.

| API value | Label (suggested) |
|-----------|-------------------|
| `OPEN` | Open |
| `IN_PROGRESS` | In progress |
| `RESOLVED` | Resolved |
| `CLOSED` | Closed |
| `CANCELLED` | Cancelled |

**Valid next states** (only these actions are shown; see [state-machine.md](./state-machine.md)):

| Current status | Actions offered to user |
|----------------|-------------------------|
| `OPEN` | “Start progress” → `IN_PROGRESS`; “Cancel ticket” → `CANCELLED` |
| `IN_PROGRESS` | “Mark resolved” → `RESOLVED`; “Cancel ticket” → `CANCELLED` |
| `RESOLVED` | “Close ticket” → `CLOSED` |
| `CLOSED` | None (read-only note: “This ticket is closed.”) |
| `CANCELLED` | None (read-only note: “This ticket is cancelled.”) |

**Control pattern:** Primary buttons or a compact dropdown “Change status…” listing only valid targets—not a free enum select of all five states.

**Current state:** Show badge + label; do not offer transitioning to the same state (optional idempotent call is unnecessary if UI only lists real transitions).

---

## 5. Screen: Ticket list (`/tickets`)

**API:** `GET /api/v1/tickets?query=&status=&page=&size=&sort=createdAt,desc`

### 5.1 Purpose

Browse tickets, search by keyword, filter by status, open detail, create new ticket.

### 5.2 Wireframe

```
+------------------------------------------------------------------+
|  Support Tickets                          [ + New ticket ]        |
+------------------------------------------------------------------+
|  [ Search........................ ]  [ Status: All v ]  [ Search ]|
+------------------------------------------------------------------+
|  (!) Banner area (errors / retry)                                 |
+------------------------------------------------------------------+
|  ID    Title              Status      Priority  Assignee   Created |
|  ----  ------------------  ---------  --------  ---------  ------- |
|  42    Cannot reset...     Open       High      team-bill  ...    |
|  41    Login loop          In prog.   Medium    (none)     ...    |
+------------------------------------------------------------------+
|  < Prev   Page 1 of 3   Next >                                    |
+------------------------------------------------------------------+
```

- **Search box:** maps to `query`; **implemented** with debounced URL sync (~350ms); optional Search button not shown in v1 UI.
- **Status filter (v1):** **Single-select** dropdown: “All” omits `status`; otherwise one enum value sent as `?status=OPEN` (API also supports comma-separated multi-status; UI need not expose multi-select in v1 per requirements A-17).
- **No priority/assignee filters** on list UI in v1 (API supports them; A-17).
- **Table rows:** clickable → `/tickets/{id}`.
- **Pagination:** `page`, `size` (default 20).
- **List column:** **Created** (`createdAt`) is shown in the implemented UI (wireframe above previously showed Updated).

### 5.3 States

| State | User sees |
|-------|-----------|
| **Loading** | Skeleton rows or centered “Loading tickets…”; disable Search/New ticket optional. |
| **Success (data)** | Table populated; pagination from `totalPages` / `last`. |
| **Empty (no tickets)** | No filters: “No tickets yet.” + prominent “Create your first ticket”. With filters/search: “No tickets match your search.” + “Clear filters”. |
| **Error (fetch)** | Banner per §3.4 or §3.5; empty table; “Retry” reloads same query. |

### 5.4 Validation / transition errors

Not applicable on list (read-only). Filter mistakes → §3.5.

---

## 6. Screen: Create ticket (`/tickets/new`)

**API:** `POST /api/v1/tickets`

### 6.1 Purpose

Create ticket with title, description, priority, optional assignee.

### 6.2 Wireframe

```
+------------------------------------------------------------------+
|  < Back to tickets          New ticket                            |
+------------------------------------------------------------------+
|  (!) Summary banner (validation / server errors)                  |
+------------------------------------------------------------------+
|  Title *                                                          |
|  [________________________________________]  <- inline error      |
|                                                                   |
|  Description *                                                    |
|  [                                                                    ]|
|  [                                                                    ]|
|                                              <- inline error        |
|  Priority                                                         |
|  ( ) Low   (•) Medium   ( ) High            <- inline error       |
|                                                                   |
|  Assignee (optional)                                              |
|  [________________________________________]  <- inline error      |
|                                                                   |
|              [ Cancel ]    [ Create ticket ]                      |
+------------------------------------------------------------------+
```

### 6.3 States

| State | User sees |
|-------|-----------|
| **Loading (submit)** | Disable form; button “Creating…” |
| **Success** | Redirect to `/tickets/{id}` (toast optional: “Ticket created”) |
| **Error (400)** | §3.1: inline on fields + summary banner; form values preserved |
| **Error (500 / network)** | §3.4 banner; form preserved |

### 6.4 Field validation display

| Field | Inline message source |
|-------|------------------------|
| `title` | `errors` where `field === "title"` |
| `description` | `field === "description"` |
| `priority` | `field === "priority"` |
| `assignee` | `field === "assignee"` |

Do not send `status` from this form.

---

## 7. Screen: Ticket detail (`/tickets/:ticketId`)

**APIs:**

- `GET /api/v1/tickets/{ticketId}` — ticket
- `PATCH /api/v1/tickets/{ticketId}` — field updates (edit mode)
- `PATCH /api/v1/tickets/{ticketId}/status` — status
- `GET /api/v1/tickets/{ticketId}/comments` — comments
- `POST /api/v1/tickets/{ticketId}/comments` — add comment

Detail combines **view mode**, **edit mode**, **status control**, and **comments** on one page (sections below).

### 7.1 View mode wireframe

```
+------------------------------------------------------------------+
|  < Back to tickets     Ticket #42              [ Edit ]             |
+------------------------------------------------------------------+
|  (!) Page-level banner (404 / network)                            |
+------------------------------------------------------------------+
|  Title: Cannot reset password                                     |
|  Status: [ Open ]     Priority: High     Assignee: team-billing |
|  Created: ...   Updated: ...                                      |
+------------------------------------------------------------------+
|  Status actions                                                   |
|  [ Start progress ]  [ Cancel ticket ]     <- only valid next     |
|  (!) Status error alert (409) here only                           |
+------------------------------------------------------------------+
|  Description                                                      |
|  User reports expired link after SSO change.                      |
+------------------------------------------------------------------+
|  Comments                                                         |
|  +--------------------------------------------------------------+|
|  | 10:15  Reproduced in staging.                                 ||
|  +--------------------------------------------------------------+|
|  [ Add a comment................................ ] [ Post ]       |
|       ^ inline error on body                                      |
|  (!) Comment submit banner (400/500)                              |
+------------------------------------------------------------------+
```

### 7.2 Edit mode wireframe

Entered via **Edit**; same URL.

```
+------------------------------------------------------------------+
|  < Back        Ticket #42          [ Cancel ]  [ Save changes ]   |
+------------------------------------------------------------------+
|  (!) Summary banner (validation on save)                          |
+------------------------------------------------------------------+
|  Title *                                                          |
|  [ Cannot reset password___________________ ]  inline errors      |
|  Priority   [ High v ]                                            |
|  Assignee   [ team-billing________________ ]                      |
|  Description *                                                    |
|  [ multiline________________________________ ]                    |
+------------------------------------------------------------------+
|  Status actions (unchanged from view — still separate from Save)  |
|  [ Start progress ]  [ Cancel ticket ]                            |
+------------------------------------------------------------------+
|  Comments (same as view mode)                                     |
+------------------------------------------------------------------+
```

**Rules:**

- **Save changes** → `PATCH` ticket fields only; success → exit edit mode, refresh view, show updated `updatedAt`.
- **Cancel** → discard local edits, exit edit mode.
- Status buttons work in either mode; they call status endpoint, not bundled into Save.

### 7.3 Loading states

| Section | Loading behaviour |
|---------|-------------------|
| Initial page | Full-page or main panel “Loading ticket…” until `GET` ticket succeeds. |
| Comments | “Loading comments…” in comments block; ticket header may show first if loaded sequentially. |
| Save fields | Disable Save/Cancel; “Saving…” |
| Status change | Disable status buttons; spinner on clicked action or section-level “Updating status…” |
| Post comment | Disable Post; “Posting…” |

Prefer parallel fetch: ticket + comments page 0 when entering detail.

### 7.4 Empty states

| Section | Empty behaviour |
|---------|-----------------|
| Comments | “No comments yet.” + visible compose area |
| N/A | Ticket always has title/description once loaded |

### 7.5 Error states

| Scenario | Display |
|----------|---------|
| Ticket `404` | Replace main content with message + link to list (§3.3). |
| Comments fetch fails | Comments section alert + “Retry”; ticket header still visible if ticket loaded. |
| Field update `400` | §3.1 on edit form; stay in edit mode. |
| Status `409` | §3.2 alert in **Status actions** section; re-fetch ticket. |
| Status `400` | Rare if UI only valid targets; treat as banner in status section + re-fetch. |
| Comment `400` | Inline on comment `body` + small banner above compose. |
| Comment ticket missing | Same as 404 if ticket deleted (out of scope for delete). |

### 7.6 Status change UX (step-by-step)

1. User clicks e.g. “Start progress”.
2. Optional confirm for destructive actions (`CANCELLED`) — **Assumption:** confirm dialog: “Cancel this ticket?” [Cancel] [Yes, cancel ticket].
3. `PATCH .../status` with `{ "status": "IN_PROGRESS" }`.
4. **Success:** update badge and action buttons per new state; clear status alert; optional success toast.
5. **409:** show `message` in status alert (§3.2); re-fetch ticket.

### 7.7 Comments UX

- List: chronological per API (`createdAt,asc`); show time + body.
- Post: trim body; empty submit blocked client-side with “Comment cannot be empty” **or** rely on server 400—if client blocks, no API call.
- On `201`: append comment to list or re-fetch first page; clear textarea.
- Pagination: if `totalPages > 1`, “Load more” or pager at bottom of comments section.

---

## 8. End-to-end flows

### 8.1 Create → work → close

```
List → New ticket → Create → Detail (OPEN)
  → Start progress → IN_PROGRESS
  → Mark resolved → RESOLVED
  → Close ticket → CLOSED (no further status actions)
```

### 8.2 Search and filter

```
List → enter "password" + status "Open" → Search
  → GET with query + status
  → empty → message §5.3
  → row click → Detail
```

### 8.3 Edit fields while in progress

```
Detail → Edit → change title → Save → PATCH success → view mode
(Status unchanged unless user uses status actions.)
```

### 8.4 Cancel from open

```
Detail (OPEN) → Cancel ticket → confirm → PATCH CANCELLED
  → terminal UI note; field edit still allowed unless product later forbids
```

---

## 9. UI ↔ API quick reference

| User action | API |
|-------------|-----|
| Load list | `GET /tickets?...` |
| Create | `POST /tickets` |
| Open detail | `GET /tickets/{id}` |
| Save edits | `PATCH /tickets/{id}` |
| Status button | `PATCH /tickets/{id}/status` |
| Load comments | `GET /tickets/{id}/comments` |
| Post comment | `POST /tickets/{id}/comments` |

---

## 10. Assumptions (UI)

| ID | Assumption |
|----|------------|
| UI-A-01 | Single-page app; no mobile-specific layout required. |
| UI-A-02 | Confirm dialog only for `CANCELLED` transition. |
| UI-A-03 | List status filter: **single-select** (“All” or one status); sync with URL query `status` when preserving list state. |
| UI-A-04 | Terminal tickets (`CLOSED`, `CANCELLED`) remain editable per requirements A-19; UI still shows **Edit**. |
| UI-A-05 | No **priority** or **assignee** filters on list UI in v1 (requirements A-17). |
| UI-A-06 | **Back to tickets** from detail uses `/tickets` without restoring list query string yet; preserving `query`/`status`/`page` remains a follow-up (§2). |

---

## 11. Traceability

| Requirement | UI |
|-------------|-----|
| FR-04–05, FR-17–20 | List + search + status filter |
| FR-01–03 | Create form |
| FR-06–08 | Detail + edit |
| FR-12–14 | Comments section |
| FR-23–24, AC-18–19 | §3 error mapping |
| FR-25–27, AC-20–22 | §4 status actions + §7.6 |
