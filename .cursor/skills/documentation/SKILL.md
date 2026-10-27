---
name: documentation
description: Guidelines and checklists for authoring, maintaining, and synchronizing project documentation, ADRs, specifications, and prompt history.
---

# Project Documentation Skill

Use this skill when creating, updating, or reviewing documentation across the repository to ensure all artifacts stay synchronized with code changes.

## 1. When & How to Update the README
Update `README.md` whenever:
- Project setup, prerequisites, or run instructions change (e.g., ports, environment variables, startup commands).
- New core features or user capabilities are added.
- High-level architecture or technology choices change.

Keep instructions practical, concise, and tested. Always provide clear, copy-pasteable commands for both frontend and backend development.

## 2. Short ADR-Style Decision Notes
When making significant technical or architectural choices (e.g., choosing an in-memory state strategy, profile separation, library selection), record a lightweight Architecture Decision Record (ADR) in `docs/adr/` (e.g., `0001-record-ticket-state-machine.md`):
- **Title**: Number and descriptive title.
- **Status**: Proposed | Accepted | Superseded.
- **Context**: The problem or requirement prompting the decision.
- **Decision**: The chosen approach.
- **Consequences**: Trade-offs, benefits, and drawbacks.

Keep ADRs short (under 1 page) and focused on rationale rather than repeating code.

## 3. Documenting API Endpoints
Document API endpoints in `docs/api.md` or OpenAPI specs in `spec/`:
- **Method & Path**: e.g., `POST /api/v1/tickets`
- **Description**: What action the endpoint performs and authorization rules.
- **Headers & Parameters**: Path parameters, query filters, and pagination arguments.
- **Request Body**: JSON example showing required and optional fields.
- **Responses**: Standard status codes with sample responses (success `200`/`201` with `Location` header, and error shapes for `400`/`404`/`409`).

## 4. Keeping Spec Files in Sync
Specifications in `spec/` represent the source of truth for the system:
- **Spec First**: If requirements or behaviour change, update the specification *before* or *alongside* the code.
- **Traceability**: Reference specific requirement and task IDs in commit messages and documentation.
- Never let code drift away from the specifications in `spec/`. If an implementation limitation is discovered, document the spec change explicitly.

## 5. Post-Feature Completion Checklist
After finishing any feature, bug fix, or significant task, perform this verification checklist:
1. **Spec Sync**: Are all status updates, edge cases, and field additions reflected in `spec/`?
2. **API & Arch Docs**: Are new endpoints, status codes, or database schema changes documented in `docs/`?
3. **README Check**: Did any run/build commands or configurations change?
4. **Prompt & Transcript History**: Confirm the chat transcript / prompt log is preserved in `.specstory/history/` for auditing and assessment submission.
5. **Clean Verification**: Ensure no untracked secrets, unformatted files, or temporary files remain.
