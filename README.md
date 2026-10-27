# Support Ticket Management System

A small full-stack application for creating and managing support tickets: REST API (Spring Boot), React UI (Vite), file-backed H2 for local development, and PostgreSQL for production-like deployments.

**Behavioural source of truth:** [`spec/`](spec/) (requirements, API contract, UI flows, state machine, test strategy).

---

## Overview

| Layer | Stack | Location |
|-------|--------|----------|
| API | Java 21, Spring Boot 3.4, JPA, Flyway, Bean Validation | `backend/` |
| UI | React 19, TypeScript, React Router, Vite 7 | `frontend/` |
| Database | H2 (file) or PostgreSQL (profile) | See [Run backend](#run-the-backend) |

**Capabilities:** Create and list tickets (search, status/priority/assignee filters, pagination), view and edit ticket fields, change status via a enforced state machine, add and list comments. Server-side validation with a uniform JSON error shape for the UI.

**Default ports:** API `http://localhost:8080`, Vite dev server `http://localhost:5173`.

---

## Prerequisites

| Tool | Version (tested) | Notes |
|------|------------------|--------|
| **JDK** | 21 | `java -version` |
| **Maven** | 3.9+ | Backend build and tests |
| **Node.js** | 20+ recommended | Frontend; npm included |
| **PostgreSQL** | 14+ | Only when using the `postgres` profile |

Copy environment templates (placeholders only—never commit real secrets):

```bash
# Optional: backend/.env or shell exports (see backend/.env.example)
# Frontend: frontend/.env.local (see frontend/.env.example)
cp .env.example .env.local          # repo root reference
cp backend/.env.example backend/.env
cp frontend/.env.example frontend/.env.local
```

---

## Run the backend

All commands assume `cd backend` first. The default Spring profile is **`h2`** (file-backed database).

### H2 (local development, default)

Data is stored under `backend/data/` (gitignored). **Run from the `backend/` directory** so the relative JDBC path `./data/tickets` is stable across restarts.

```bash
cd backend
mvn spring-boot:run
```

Or explicitly:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=h2"
```

API base: `http://localhost:8080/api/v1`

**Persistence check:** Create a ticket, stop the app (`Ctrl+C`), start again, `GET` the same ticket—or run `PersistenceAcrossRestartIntegrationTest` (see [Tests](#run-tests)).

### PostgreSQL

Set credentials via environment variables (required for this profile):

```bash
cd backend
set SPRING_PROFILES_ACTIVE=postgres
set SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/tickets
set SPRING_DATASOURCE_USERNAME=your_db_username
set SPRING_DATASOURCE_PASSWORD=your_db_password
mvn spring-boot:run
```

On PowerShell use `$env:SPRING_PROFILES_ACTIVE = "postgres"` (and the same for the datasource variables). Ensure the database exists and Flyway can migrate on startup.

---

## Run the frontend

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`. Routes: `/tickets`, `/tickets/new`, `/tickets/:ticketId`.

### API connectivity (dev)

| Setup | `VITE_API_BASE_URL` | How it works |
|--------|---------------------|--------------|
| **Recommended** | Empty (default in `.env.development`) | Browser calls relative `/api/v1/...`; Vite proxies `/api` → `http://localhost:8080` |
| Direct to API | `http://localhost:8080` | Requires backend CORS for the Vite origin (see [spec/architecture.md](spec/architecture.md) §8; optional `CorsConfig` not yet added—use proxy for local dev) |

---

## Run tests

### Backend (80 tests)

```bash
cd backend
mvn test
```

Includes unit, repository, state-machine integration, PATCH validation, list filters, and persistence-across-restart tests.

### Frontend

```bash
cd frontend
npm test              # Vitest
npm run build         # tsc -b + production bundle (recommended CI gate)
```

---

## API summary

Base path: **`/api/v1`**. JSON bodies. Standard error envelope: `timestamp`, `status`, `error`, `message`, `errors[]` (field-level on validation).

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/tickets` | Create ticket (`201` + `Location`) |
| `GET` | `/tickets` | List: `query`, `status`, `priority`, `assignee`, `page`, `size`, `sort` |
| `GET` | `/tickets/{id}` | Get one ticket |
| `PATCH` | `/tickets/{id}` | Update title, description, priority, assignee only (not `status`) |
| `PATCH` | `/tickets/{id}/status` | Status transition (`409` if illegal) |
| `POST` | `/tickets/{id}/comments` | Add comment (`201` + `Location`) |
| `GET` | `/tickets/{id}/comments` | Paginated comments (`sort=createdAt,asc` default) |

**Status transitions:** Only via `PATCH .../status`; rules in [spec/state-machine.md](spec/state-machine.md).

Full request/response examples: [spec/api-contract.md](spec/api-contract.md). Short reference: [docs/api.md](docs/api.md).

---

## Spec-driven workflow

Development followed **spec-first, then plan, then code**:

1. **Requirements & specs** — `spec/requirements.md`, `data-model.md`, `state-machine.md`, `api-contract.md`, `ui-flow.md`, `architecture.md`, `test-strategy.md`.
2. **Implementation plan** — `spec/plan.md` (ordered tasks and DoD).
3. **Steering** — `.cursor/rules/` (Java/Spring, API standards, testing) and `.cursor/commands/` (e.g. review-code).
4. **Implementation** — `backend/` and `frontend/` aligned to the contract; gaps closed during review (PATCH validation, list filters, persistence test, frontend build gate).
5. **Verification** — `mvn test`, `npm test`, `npm run build`; regression tests for critical fixes.
6. **Documentation** — This README, `docs/`, and spec updates when behaviour diverged or clarified.

**Prompt and chat history:** Full Cursor chat transcripts were exported and stored under **[`.specstory/history/`](.specstory/history/)** for assessment audit (see also [docs/prompt-history.md](docs/prompt-history.md)).

---

## Repository layout

```
backend/          Spring Boot API
frontend/         React SPA
spec/             Requirements, API contract, UI flows (source of truth)
docs/             API quick reference, prompt history
.cursor/            Rules, commands, skills
.specstory/history/ Exported chat transcripts
```

---

## Further reading

- [spec/plan.md](spec/plan.md) — Task breakdown and implementation status notes  
- [spec/test-strategy.md](spec/test-strategy.md) — Test levels and case IDs  
- [.cursor/rules/api-standards.md](.cursor/rules/api-standards.md) — Error shape and HTTP conventions
