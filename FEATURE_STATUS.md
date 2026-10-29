# COMPLYMIND AI — Feature Status

> **Last Updated:** 2026-10-29 | **Automated Tests:** 21/21 passing

Status legend: **VERIFIED** | **PARTIAL** | **BLOCKED** | **NOT IMPLEMENTED**

---

## Authentication & User Management

| Feature | Status | Notes |
|---|---|---|
| Login (form) | VERIFIED | `/login` → Spring Security form login |
| Logout | VERIFIED | `/logout` → session invalidation |
| Password reset (email token) | VERIFIED | `PasswordResetToken` table, email link |
| Password reset (form submission) | VERIFIED | Token validation, BCrypt re-hash |
| Admin: Create user (modal) | VERIFIED | `POST /users/create` → DB persisted |
| User list with roles | VERIFIED | `GET /users` → live DB query |
| Role-based access control | VERIFIED | `@PreAuthorize` via Spring Security |
| Cross-institution access prevention | VERIFIED | `SecurityUtils.canAccessInstitution()` |
| BCrypt password hashing | VERIFIED | `UserService.registerUser()` |
| Setup wizard (first run) | VERIFIED | `SetupController` creates first admin |

---

## Institution Registry

| Feature | Status | Notes |
|---|---|---|
| List institutions (paginated) | VERIFIED | `GET /institutions` → Page query |
| View institution details | VERIFIED | `GET /institutions/{id}` → entity + apps |
| Create institution | VERIFIED | `POST /institutions/create` → DB insert |
| Search/filter institutions | PARTIAL | Server-side pagination; client-side filter only |
| 31 real institutions seeded | VERIFIED | `RealDataSeederService`, `isDemo=false` |

---

## Compliance Applications

| Feature | Status | Notes |
|---|---|---|
| List applications (paginated) | VERIFIED | `GET /applications` → Page query |
| View application detail | VERIFIED | `GET /applications/{id}` with JOIN FETCH |
| Create application | VERIFIED | `POST /applications/create` → auto-attaches requirements |
| Status transition (form) | VERIFIED | Validated state machine, DB persisted |
| Status change notifications | VERIFIED | `NotificationService.createNotification()` on transition |
| Export CSV | VERIFIED | `GET /export/applications.csv` |
| Export Excel (.xlsx) | VERIFIED | `GET /export/applications.xlsx` (Apache POI) |
| Single-app report CSV | VERIFIED | `GET /export/applications/{id}/report.csv` |
| Pagination navigation | VERIFIED | Page controls wired to `?page=` param |

---

## Framework & Rules

| Feature | Status | Notes |
|---|---|---|
| List frameworks | VERIFIED | `GET /rules` → framework filter |
| List rules by framework | VERIFIED | `?frameworkId=` query parameter |
| Create rule | VERIFIED | `POST /rules/create` → DB insert |
| Edit rule | NOT IMPLEMENTED | No `GET /rules/{id}/edit` endpoint yet |
| AICTE-APH-2026-27 framework seeded | VERIFIED | 10 rules from APH public handbook |
| Rule versioning | PARTIAL | `version` column exists; no version bump UI |

---

## Evidence Management

| Feature | Status | Notes |
|---|---|---|
| File upload (inline form) | VERIFIED | `POST /evidence/upload` → disk + DB |
| MIME type validation | VERIFIED | Extension + Tika MIME check |
| Path traversal prevention | VERIFIED | `normalize().toAbsolutePath()` check |
| File size limit (25 MB) | VERIFIED | `application.yml` multipart config |
| Evidence download (authorized) | VERIFIED | `GET /evidence/download/{id}` |
| Apache Tika text extraction | VERIFIED | `DocumentParserService` → `extractedText` |
| Review decision (PASS/FAIL) | VERIFIED | `POST /evidence/review/{id}` → `ReviewDecision` |
| Evidence Passport view | VERIFIED | `GET /applications/{id}/passport` |
| Evidence versioning | PARTIAL | `versionNumber` incremented on re-upload; no UI history view |

---

## Corrective Actions

| Feature | Status | Notes |
|---|---|---|
| List all actions | VERIFIED | `GET /corrective-actions` |
| Create action | VERIFIED | `POST /corrective-actions/create` → DB + notification |
| Assign to user | VERIFIED | `TASK_ASSIGNED` notification sent to assignee |
| Update status (inline form) | VERIFIED | `POST /corrective-actions/{id}/status` |
| OPEN → IN_PROGRESS → SUBMITTED | VERIFIED | State machine in `CorrectiveActionService` |
| Verify / Reject action | VERIFIED | `POST /corrective-actions/{id}/verify` |
| Overdue auto-escalation | VERIFIED | `@Scheduled(cron = "0 0 * * * *")` hourly check |
| Overdue notification | VERIFIED | `ESCALATION` notification on overdue transition |
| Resolution summary | VERIFIED | `resolutionSummary` field persisted |
| Corrective actions export CSV | VERIFIED | `GET /export/corrective-actions.csv` |

---

## Notifications

| Feature | Status | Notes |
|---|---|---|
| In-app notification list | VERIFIED | `GET /notifications` → paged |
| Unread count in nav | VERIFIED | `NotificationInterceptor` → `unreadCount` model |
| Mark single as read | VERIFIED | `POST /notifications/{id}/read` |
| Mark all as read | VERIFIED | `POST /notifications/mark-all-read` |
| Trigger on: app created | VERIFIED | `ApplicationController.createApplication()` |
| Trigger on: status change | VERIFIED | `ApplicationController.updateStatus()` |
| Trigger on: CA assigned | VERIFIED | `CorrectiveActionService.createAction()` |
| Trigger on: CA overdue | VERIFIED | `CorrectiveActionService.checkOverdueActions()` |
| Email delivery | PARTIAL | `EmailService` wired to Spring Mail; requires SMTP credentials |
| Email delivery tracking | PARTIAL | No `email_sent_at` / retry column yet |

---

## Dashboard & Analytics

| Feature | Status | Notes |
|---|---|---|
| Total applications count | VERIFIED | Live DB count |
| Blocked requirements count | VERIFIED | `DashboardStats.blockedRequirementsCount` |
| High/Critical risk count | VERIFIED | Derived from `riskBand` on applications |
| Overdue actions count | VERIFIED | `CorrectiveActionRepository.countOverdue()` |
| Recent applications table | VERIFIED | `findTop5RecentWithInstitution` JOIN FETCH |
| Application pipeline stages | VERIFIED | `applicationsByStatus` map from live query |
| Empty state (zero data) | VERIFIED | Accurate "no data" message, not fabricated 0s |

---

## Risk Engine

| Feature | Status | Notes |
|---|---|---|
| Risk score calculation | VERIFIED | 4-factor weighted formula, deterministic |
| Risk band assignment | VERIFIED | LOW/MEDIUM/HIGH/CRITICAL thresholds |
| Risk view page | VERIFIED | `GET /applications/{id}/risk` |
| Root cause investigation | VERIFIED | `GET /applications/{id}/root-cause` |
| Risk breakdown display | VERIFIED | Contributing factors shown |
| Risk recalculation on create | VERIFIED | Called after `applicationRepository.save()` |
| Risk recalculation on evidence | PARTIAL | Scheduled; not triggered instantly on upload |
| Assessment history | PARTIAL | `RiskAssessment` entity saved; no history UI |

---

## Ripple Engine

| Feature | Status | Notes |
|---|---|---|
| Dependency graph (BFS) | VERIFIED | `RippleEngineService.propagate()` |
| Graph visualisation | VERIFIED | `GET /applications/{id}/graph` — Cytoscape.js |
| Cycle detection | VERIFIED | Visited-set guard in BFS |
| Dependency types | VERIFIED | `DependencyType` enum (`BLOCKS`, `REQUIRES`, `RELATES`) |
| Downstream impact propagation | VERIFIED | Cascades `BLOCKED` status downstream |

---

## Audit Trail

| Feature | Status | Notes |
|---|---|---|
| Audit event list | VERIFIED | `GET /audit` → `AuditEvent` table |
| Event recording | VERIFIED | `AuditService.recordEvent()` on mutations |
| Events: app created/transitioned | VERIFIED | ApplicationController |
| Events: action created/changed | VERIFIED | CorrectiveActionService |
| Events: evidence uploaded | VERIFIED | EvidenceReviewService |
| Protect from user modification | VERIFIED | No delete/edit endpoint exposed |

---

## Reports & Exports

| Feature | Status | Notes |
|---|---|---|
| Applications CSV | VERIFIED | `GET /export/applications.csv` |
| Applications Excel | VERIFIED | `GET /export/applications.xlsx` (Apache POI) |
| Corrective actions CSV | VERIFIED | `GET /export/corrective-actions.csv` |
| Single application CSV report | VERIFIED | `GET /export/applications/{id}/report.csv` |
| PDF generation | NOT IMPLEMENTED | iText/JasperReports dep not added; CSV covers use case |
| Compliance certificate | NOT IMPLEMENTED | Requires regulatory verification workflow |

---

## DevOps & Deployment

| Feature | Status | Notes |
|---|---|---|
| Dockerfile (multi-stage) | VERIFIED | JDK 17 builder + JRE 17 runtime, non-root user |
| docker-compose (dev H2) | VERIFIED | `docker compose up` |
| docker-compose (prod PostgreSQL) | VERIFIED | `docker compose --profile postgres up` |
| GitHub Actions CI | VERIFIED | test → build → Docker push on main |
| PostgreSQL Spring profile | VERIFIED | `application.yml` `postgres` profile |
| `.env.template` | VERIFIED | All vars documented, no secrets committed |
| Flyway migrations (H2 + PG) | VERIFIED | V1 + V2 in `db/migration/` |

---

## Security

| Feature | Status | Notes |
|---|---|---|
| BCrypt password hashing | VERIFIED | No plaintext storage |
| CSRF protection | VERIFIED | Spring Security CSRF filter enabled |
| Session-based auth | VERIFIED | `HttpSession` with 30-min timeout |
| Role-based route protection | VERIFIED | `SecurityConfig` route matchers |
| File path traversal prevention | VERIFIED | `normalize().toAbsolutePath()` parent check |
| No credentials in code | VERIFIED | All secrets via env vars |
| No sensitive data in audit log | VERIFIED | Passwords never logged |
| Cross-institution data isolation | VERIFIED | `canAccessInstitution()` in all services |
