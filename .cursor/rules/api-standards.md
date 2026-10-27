# REST API Standards & Conventions

## URI Structure & Versioning
- **Base Prefix**: All endpoints must be prefixed with `/api/v1`.
- **Resource Naming**: Plural nouns in kebab-case (e.g., `/api/v1/tickets`, `/api/v1/ticket-comments`).
- **Sub-resources**: Natural hierarchical paths for nested relationships (e.g., `/api/v1/tickets/{ticketId}/comments`).
- **Actions / State Transitions**: Prefer standard HTTP verbs (`PATCH /api/v1/tickets/{id}/status` or `POST /api/v1/tickets/{id}/assign`).

## HTTP Status Codes
- **`200 OK`**: Successful `GET`, `PUT`, or `PATCH` returning a body.
- **`201 Created`**: Successful `POST` creating a resource. **Must** include a `Location` header pointing to the created resource URI (e.g., `Location: /api/v1/tickets/{id}`).
- **`204 No Content`**: Successful request returning no body (e.g., `DELETE`, or empty state action).
- **`400 Bad Request`**: Malformed request body, unparseable JSON, or failed Bean Validation.
- **`404 Not Found`**: Target resource identifier does not exist.
- **`409 Conflict` / `422 Unprocessable Entity`**: Business rule violation or illegal state transition (e.g., transitioning a ticket from `CLOSED` to `RESOLVED`).
- **`500 Internal Server Error`**: Unexpected server error (log internally, return clean generic message to client).

## Standard Error Response Shape
All non-2xx responses must return a uniform JSON body:

```json
{
  "timestamp": "2026-10-26T14:40:00.000Z",
  "status": 400,
  "error": "BAD_REQUEST",
  "message": "Validation failed for request payload.",
  "errors": [
    {
      "field": "title",
      "rejectedValue": "",
      "message": "Title must not be blank"
    }
  ]
}
```

- **User-Facing Quality**: The `message` and nested field `message` attributes must be human-readable and clean enough to display directly in UI notifications or form error badges.
- **Field Errors**: The `errors` array contains field-specific validation failures. For non-validation errors (e.g., 404, 409), `errors` can be an empty array or omitted.

## Pagination & Sorting
- Use consistent zero-based query parameters:
  - `page`: Page index (default: `0`).
  - `size`: Items per page (default: `20`, maximum enforced: `100`).
  - `sort`: Field and direction (e.g., `sort=createdAt,desc`).
- Standard paginated response envelope:
  ```json
  {
    "content": [ ... ],
    "page": 0,
    "size": 20,
    "totalElements": 85,
    "totalPages": 5,
    "last": false
  }
  ```

## Search & Filtering Query Parameters
- Use camelCase parameter names matching domain attributes:
  - Filtering by status: `?status=OPEN` or `?status=OPEN,IN_PROGRESS`
  - Filtering by priority: `?priority=HIGH`
  - Filtering by assignee (free-text): `?assignee=team-billing` (exact match; see `spec/api-contract.md`)
  - Text search: `?query=login+issue`
- Empty filter values should be ignored by backend queries.
