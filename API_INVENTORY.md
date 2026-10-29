# COMPLYMIND AI — API Inventory

> **Version:** 1.0 | **Date:** 2026-10-29
> Swagger UI: `http://localhost:8080/swagger-ui.html`
> OpenAPI JSON: `http://localhost:8080/api-docs`

---

## Auth: `SecurityConfig`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/login` | Public | Login form |
| POST | `/login` | Public | Spring Security form login |
| POST | `/logout` | Authenticated | Session invalidation |
| GET | `/forgot-password` | Public | Password reset request form |
| POST | `/forgot-password` | Public | Send reset email |
| GET | `/reset-password` | Public (token) | Reset password form |
| POST | `/reset-password` | Public (token) | Submit new password |
| GET | `/setup` | Unauthenticated only | First-run admin setup |
| POST | `/setup` | Unauthenticated only | Create initial admin |

---

## Dashboard

| Method | Path | Auth | Template | DB Entities |
|---|---|---|---|---|
| GET | `/dashboard` | Any auth | `dashboard.html` | `ComplianceApplication`, `CorrectiveAction`, `ApplicationRequirement` |

**Response model:** `DashboardStats`
```json
{
  "totalApplications": 31,
  "blockedRequirementsCount": 5,
  "highRiskCount": 3,
  "criticalRiskCount": 1,
  "overdueActionsCount": 2,
  "recentApplications": [...],
  "applicationsByStatus": { "DRAFT": 10, "IN_PROGRESS": 8, ... }
}
```

---

## Institutions: `InstitutionController` + `InstitutionRestController`

| Method | Path | Auth | Template / Response | DB |
|---|---|---|---|---|
| GET | `/institutions` | Any | `institutions/list.html` (paged) | `Institution` |
| GET | `/institutions/new` | ADMIN | `institutions/create.html` | — |
| POST | `/institutions/create` | ADMIN | redirect `/institutions` | INSERT `institutions` |
| GET | `/institutions/{id}` | Any | `institutions/view.html` | `Institution` + apps |
| GET | `/api/institutions` | Any | JSON `ApiResponse<List<Institution>>` | `Institution` |
| GET | `/api/institutions/{id}` | Any | JSON `ApiResponse<Institution>` | `Institution` |

**Institution fields:** `id, code, name, type, aictePermamentId, state, city, address, contactEmail, contactPhone, isDemo`

---

## Compliance Applications: `ApplicationController` + `ApplicationRestController` + `ExportController`

| Method | Path | Auth | Template / Response | DB |
|---|---|---|---|---|
| GET | `/applications` | Any | `applications/list.html` (paged) | `ComplianceApplication` |
| GET | `/applications/new` | Any | `applications/create.html` | — |
| POST | `/applications/create` | Any | redirect `/applications/{id}` | INSERT + requirements + risk calc |
| GET | `/applications/{id}` | Any | `applications/view.html` | JOIN FETCH institution+framework |
| POST | `/applications/{id}/status` | Any | redirect `/applications/{id}` | UPDATE status + notify |
| GET | `/export/applications.csv` | Any | CSV download | SELECT all applications |
| GET | `/export/applications.xlsx` | Any | XLSX download | SELECT all applications |
| GET | `/export/applications/{id}/report.csv` | Any | CSV download | Single application |
| GET | `/api/applications` | Any | JSON | `ComplianceApplication` |
| GET | `/api/applications/{id}` | Any | JSON | `ComplianceApplication` |

**Status transition rules:**
```
DRAFT → IN_PROGRESS
IN_PROGRESS → READY_FOR_REVIEW | DRAFT
READY_FOR_REVIEW → UNDER_REVIEW | ACTION_REQUIRED
UNDER_REVIEW → ACTION_REQUIRED | COMPLETED
ACTION_REQUIRED → IN_PROGRESS | READY_FOR_REVIEW
COMPLETED → (final)
```

---

## Evidence: `EvidenceController` + `EvidencePassportController`

| Method | Path | Auth | Template / Response | DB |
|---|---|---|---|---|
| POST | `/evidence/upload` | Any | redirect `/applications/{id}` | INSERT `evidence_documents`, disk write |
| GET | `/evidence/review/{id}` | Any | `evidence/review.html` | `EvidenceDocument` |
| POST | `/evidence/review/{id}` | Any | redirect `/applications/{id}` | INSERT `review_decisions` |
| GET | `/evidence/download/{id}` | Any | File stream | `EvidenceDocument.storedFilename` |
| GET | `/applications/{id}/passport` | Any | `evidence/passport.html` | JOIN all evidence + requirements |

**Upload request:** `multipart/form-data` — fields: `applicationId`, `appReqId`, `file`
**Allowed types:** `.pdf`, `.docx`, `.xlsx`, `.doc`, `.xls`, `.txt`, `.csv`
**Max size:** 25 MB

---

## Requirements & Rules: `RuleController`

| Method | Path | Auth | Template | DB |
|---|---|---|---|---|
| GET | `/rules` | Any | `rules/list.html` | `ComplianceFramework`, `ComplianceRule` |
| GET | `/rules/new` | ADMIN | `rules/create.html` | — |
| POST | `/rules/create` | ADMIN | redirect `/rules` | INSERT `compliance_rules` |

---

## Corrective Actions: `CorrectiveActionController` + `ExportController`

| Method | Path | Auth | Template | DB |
|---|---|---|---|---|
| GET | `/corrective-actions` | Any | `correctiveactions/list.html` | `CorrectiveAction` |
| GET | `/corrective-actions/new` | Any | `correctiveactions/create.html` | — |
| POST | `/corrective-actions/create` | Any | redirect `/corrective-actions` | INSERT + notify |
| POST | `/corrective-actions/{id}/status` | Any | redirect `/corrective-actions` | UPDATE status |
| POST | `/corrective-actions/{id}/verify` | Any | redirect `/corrective-actions` | UPDATE status + verifier |
| GET | `/export/corrective-actions.csv` | Any | CSV download | SELECT all actions |

**State machine:**
```
OPEN → IN_PROGRESS → SUBMITTED_FOR_VERIFICATION → VERIFIED
                                               ↘ REJECTED
Any non-VERIFIED → OVERDUE (automated @Scheduled hourly)
```

---

## Notifications: `NotificationController`

| Method | Path | Auth | Template / Response | DB |
|---|---|---|---|---|
| GET | `/notifications` | Any | `notifications/list.html` (paged) | `Notification` |
| POST | `/notifications/{id}/read` | Any | redirect `/notifications` | UPDATE is_read |
| POST | `/notifications/mark-all-read` | Any | redirect `/notifications` | UPDATE all |
| GET | `/notifications/unread-count` | Any | JSON `ApiResponse<Long>` | COUNT unread |

---

## Risk Engine: `RiskController`

| Method | Path | Auth | Template | DB |
|---|---|---|---|---|
| GET | `/applications/{id}/risk` | Any | `risk/view.html` | `RiskAssessment` |
| GET | `/applications/{id}/root-cause` | Any | `risk/rootcause.html` | `ApplicationRequirement` |

**Risk factors:**
- `deadlineUrgency` × 0.35 — days until closest deadline
- `blockedDependency` × 0.30 — blocked requirement count
- `evidenceDeficiency` × 0.20 — missing/failed evidence ratio
- `unresolvedPendency` × 0.15 — open corrective action count

---

## Ripple Engine: `RippleGraphController`

| Method | Path | Auth | Template | DB |
|---|---|---|---|---|
| GET | `/applications/{id}/graph` | Any | `ripple/graph.html` | `RequirementDependency`, `ApplicationRequirement` |

**Graph format (JSON to Cytoscape.js):**
```json
{
  "nodes": [{"data": {"id": "REQ-01", "label": "Faculty Ratio", "status": "BLOCKED"}}],
  "edges": [{"data": {"source": "REQ-01", "target": "REQ-02", "type": "BLOCKS"}}]
}
```

---

## Audit: `AuditService`

| Method | Path | Auth | Template | DB |
|---|---|---|---|---|
| GET | `/audit` | ADMIN | `audit/list.html` | `AuditEvent` |

**Recorded event types:** `APPLICATION_CREATED`, `APPLICATION_STATUS_TRANSITION`, `ACTION_CREATED`, `ACTION_STATUS_CHANGE`, `ACTION_VERIFIED`, `EVIDENCE_UPLOADED`, `EVIDENCE_REVIEWED`

---

## Users: `UserController`

| Method | Path | Auth | Template | DB |
|---|---|---|---|---|
| GET | `/users` | ADMIN | `users/list.html` | `User` |
| POST | `/users/create` | ADMIN | redirect `/users` | INSERT `users` |

---

## Demo Dataset: `DemoController`

| Method | Path | Auth | Action |
|---|---|---|---|
| POST | `/demo/load` | ADMIN | Seed synthetic demo data (`isDemo=true`) |
| POST | `/demo/clear` | ADMIN | Delete all `isDemo=true` records |

> **Note:** Real institution data (`isDemo=false`) is never deleted by `/demo/clear`

---

## Database Tables (14 total)

| Table | Primary Entity | Key Relations |
|---|---|---|
| `institutions` | `Institution` | ← `users`, `compliance_applications` |
| `users` | `User` | → `institutions` |
| `compliance_frameworks` | `ComplianceFramework` | ← `compliance_rules`, `requirements` |
| `compliance_rules` | `ComplianceRule` | → `compliance_frameworks` |
| `requirements` | `Requirement` | → `compliance_frameworks` |
| `requirement_dependencies` | `RequirementDependency` | → `requirements` (self-join) |
| `compliance_applications` | `ComplianceApplication` | → `institutions`, `compliance_frameworks` |
| `application_requirements` | `ApplicationRequirement` | → `compliance_applications`, `requirements` |
| `evidence_documents` | `EvidenceDocument` | → `application_requirements`, `compliance_applications` |
| `review_decisions` | `ReviewDecision` | → `evidence_documents`, `users` |
| `risk_assessments` | `RiskAssessment` | → `compliance_applications` |
| `corrective_actions` | `CorrectiveAction` | → `compliance_applications`, `users` |
| `notifications` | `Notification` | → `users`, `institutions` |
| `audit_events` | `AuditEvent` | standalone (entity type + ID stored as strings) |
| `password_reset_tokens` | `PasswordResetToken` | → `users` |
