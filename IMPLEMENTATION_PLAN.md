# COMPLYMIND AI — Implementation Plan

> **Version:** 1.0 | **Date:** 2026-10-29 | **Status:** Active

## Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.3.4 |
| Security | Spring Security 6 + BCrypt |
| ORM | Spring Data JPA / Hibernate 6.5 |
| DB (Dev) | H2 file (PostgreSQL compat mode) |
| DB (Prod) | PostgreSQL 16 |
| Migrations | Flyway V1 (schema) + V2 (password reset) |
| Templates | Thymeleaf 3.1 + Bootstrap 5.3 |
| Doc Parser | Apache Tika 2.9.2 + Apache POI 5.2.5 |
| API Docs | SpringDoc OpenAPI 2.5 (Swagger UI) |
| CI/CD | GitHub Actions + Docker (multi-stage) |

---

## Phase Overview

| Phase | Description | Status |
|---|---|---|
| 0 | Full repository audit & deliverable creation | ✅ COMPLETE |
| 1 | Frontend–Backend integration | ✅ COMPLETE |
| 2 | Dynamic data & database integrity | ✅ COMPLETE |
| 3 | Core modules (auth, institutions, applications, evidence, CA) | ✅ COMPLETE |
| 4 | Compliance & risk intelligence | ✅ COMPLETE |
| 5 | Reporting & exports | ✅ COMPLETE |
| 6 | DevOps: Docker + CI/CD | ✅ COMPLETE |
| 7 | AI/ML integration | PARTIAL (document parser implemented; Gemini API awaits credentials) |

---

## Phase 0 — Audit Deliverables

- [x] `IMPLEMENTATION_PLAN.md` (this file)
- [x] `FEATURE_STATUS.md`
- [x] `API_INVENTORY.md`
- [x] `.env.template`
- [x] Baseline test run: **14/14 tests passing**

## Phase 1 — Integration

### Critical Bug Fixes Applied
1. `${application.id}` → `${currApp.id}` in `applications/view.html` (evidence upload form)
2. All controllers now use `model.addAttribute("app", app)` in addition to `"application"` to avoid Thymeleaf ServletContext collision
3. Null-safe guards on all templates for lazy-loaded associations

### Features Wired
- Evidence upload form → `POST /evidence/upload` → `EvidenceStorageService` → disk + DB
- Evidence download → `GET /evidence/download/{id}` → authorized file serve
- Evidence review decision → `POST /evidence/review/{id}` → `EvidenceReviewService`
- Status transition form → `POST /applications/{id}/status` → validated state machine
- Corrective action status form → `POST /corrective-actions/{id}/status`
- Corrective action verify form → `POST /corrective-actions/{id}/verify`
- User create modal → `POST /users/create` → `UserService.registerUser()`

## Phase 2 — Dynamic Data

### Real Data Seeded (isDemo=false, never wiped)
- 31 real Indian institutions (IITs, NITs, IIMs, private colleges)
- 1 AICTE-APH-2026-27 compliance framework
- 10 AICTE-sourced rules (APH-1.1 to APH-5.2)
- 31 linked compliance applications with auto-attached requirements

### Data Integrity Rules
- All dashboard totals computed from live DB queries
- Risk scores persisted via `RiskAssessment` entity, recalculated on evidence events
- Ripple propagation via BFS through `RequirementDependency` graph
- Empty states display accurate "0" rather than fabricated numbers

## Phase 3 — Core Modules

### Authentication
- BCrypt password hashing
- Spring Security form login / logout
- Password reset via email token (`PasswordResetToken` table)
- Admin-only user creation via modal

### Corrective Actions State Machine
```
OPEN → IN_PROGRESS → SUBMITTED_FOR_VERIFICATION → VERIFIED
                                               ↘ REJECTED → IN_PROGRESS
Any non-terminal → OVERDUE (automated hourly check @Scheduled)
```

### Evidence Pipeline
1. Upload → MIME + extension validation → UUID-named file storage
2. Apache Tika text extraction → `EvidenceDocument.extractedText`
3. Review decision → `ReviewDecision` entity → audit event

## Phase 4 — Risk & Compliance Engine

### Risk Score Formula
```
score = (deadline_urgency × 0.35) + (blocked_deps × 0.30) + (evidence_deficiency × 0.20) + (unresolved_actions × 0.15)
```
- Normalised to 0–100
- Bands: LOW (0–29), MEDIUM (30–59), HIGH (60–79), CRITICAL (80–100)
- Configurable weights in `application.yml`

### Ripple Engine
- Dependency graph loaded from `requirement_dependencies` table
- BFS traversal to find all downstream blocked requirements
- Cycle detection guards against infinite loops
- Results stored in `ApplicationRequirement.status`

## Phase 5 — Reports & Exports

- `GET /export/applications.csv` — all applications as CSV
- `GET /export/applications.xlsx` — all applications as Excel (Apache POI)
- `GET /export/corrective-actions.csv` — all corrective actions as CSV
- `GET /export/applications/{id}/report.csv` — single-application report

## Phase 6 — DevOps

- `Dockerfile` — multi-stage build (JDK 17 builder → JRE 17 runtime, non-root user)
- `docker-compose.yml` — dev (H2) and prod (PostgreSQL) profiles
- `.github/workflows/ci.yml` — test → build → Docker push pipeline
- `.env.template` — all configuration variables documented

## Phase 7 — AI Integration

- **Document Parser**: Apache Tika extracts text from PDF/DOCX/XLSX — functional
- **Compliance Scoring**: Deterministic rule engine — functional
- **Gemini API Integration**: Skeleton ready; requires `GEMINI_API_KEY` environment variable

---

## Risks & Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| H2 → PostgreSQL migration | High | `postgres` Spring profile ready; Flyway migrations are PostgreSQL-compatible |
| Email delivery failures | Medium | Notifications persisted before email attempt; failure logged, not silent |
| Large file uploads | Medium | 25 MB limit enforced in `application.yml` + MIME validation |
| Thymeleaf model collision | High | All controllers use `"app"` key; `th:with="currApp"` alias in templates |
| Circular ripple dependencies | Medium | BFS cycle detection in `RippleEngineService` |

---

## Acceptance Criteria

Every feature is considered VERIFIED only when:
1. The UI action triggers a real backend endpoint (not a JS stub)
2. The endpoint performs a real database read or write
3. The database change persists across application restart
4. The UI refreshes with the new state after the operation
5. All 14 existing tests continue to pass
