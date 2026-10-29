# COMPLYMIND AI — AICTE Approval Workflow Automation & Compliance Intelligence

> **Autonomous Compliance Monitoring, DAG Ripple Propagation & Explainable Risk Intelligence for Technical Institutions**

COMPLYMIND AI is an enterprise-grade full-stack compliance intelligence platform built for technical education institutions to automate, trace, evaluate, and navigate AICTE (All India Council for Technical Education) Approval Process Handbook (APH) regulatory compliance.

---

## 🌟 Key Capabilities

1. **Compliance Application Workflow**:
   - Streamlined institutional application registration attaching statutory framework requirements.
   - Formal multi-stage workflow pipeline: `DRAFT` &rarr; `IN_PROGRESS` &rarr; `READY_FOR_REVIEW` &rarr; `UNDER_REVIEW` &rarr; `ACTION_REQUIRED` &rarr; `COMPLETED`.
   - Real-time in-app notification triggers and audit trail recording.

2. **Compliance Ripple Engine (Directed Acyclic Graph)**:
   - Full dependency DAG modeling source-to-dependent statutory requirements using Breadth-First Search (BFS).
   - Traversal automatically detects downstream blocking paths when prerequisites fail or expire.
   - Cycle detection algorithm protects graph integrity against circular dependencies.
   - Interactive Cytoscape.js visual graph canvas with real-time node state inspection.

3. **Deterministic Explainable Risk Engine**:
   - Multi-factor mathematical scoring model:
     $$\text{Risk} = (w_1 \cdot \text{Deadline}) + (w_2 \cdot \text{Blocked}) + (w_3 \cdot \text{Evidence}) + (w_4 \cdot \text{Pendency})$$
   - Factors evaluated: deadline proximity, downstream blocked dependencies, evidence deficiency, and unresolved corrective actions.
   - Explains every score with contributing factors, risk bands (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`), and historical assessments.

4. **Root Cause Investigator**:
   - Isolates the originating blocker from clusters of cascading downstream failures.
   - Traces dependency chains and recommends tailored corrective actions.

5. **Evidence Lifecycle Management**:
   - Document upload with file signature / magic byte inspection (`%PDF`, `PK\x03\x04`).
   - Automated text and metadata extraction powered by Apache Tika and Apache POI.
   - In-browser inline PDF preview (`GET /evidence/preview/{id}`).
   - SHA-256 cryptographic integrity checksums and document version history.
   - Formal reviewer decision audit (`APPROVED`, `REJECTED`, `PENDING`).

6. **Public & Open-Source Data Ingestion Pipeline**:
   - Ingestion workflow for public datasets from AICTE, Data.gov.in, and official gazettes.
   - Provenance classification: `OFFICIAL_SOURCE_VERIFIED`, `PUBLIC_SOURCE_UNVERIFIED`, `SYNTHETIC_TEST_DATA`, `USER_PROVIDED`.
   - Automated duplicate detection and upsert by institutional code and AICTE permanent ID.

7. **Data Export & Reporting**:
   - Instant export of live operational records to CSV and formatted Microsoft Excel (`.xlsx`).
   - Single-application compliance dossier report download.

8. **Enterprise Design System with VFX**:
   - High-tech interactive card spotlight cursor tracking, holographic button light sweeps, and pulsing radar beacons for critical blockers.

---

## 🛠️ Technology Stack

| Layer | Component |
|---|---|
| **Backend** | Java 17, Spring Boot 3.3.4, Spring Security 6, Spring Data JPA, Hibernate 6.5 |
| **Frontend** | Thymeleaf 3.1, Bootstrap 5.3, Bootstrap Icons, Cytoscape.js 3.28 |
| **Database** | H2 Database (Dev / Test) & PostgreSQL 16 (Production) |
| **Database Migration** | Flyway 10 |
| **Document Processing**| Apache Tika 2.9.2, Apache POI 5.2.5 |
| **API Documentation** | SpringDoc OpenAPI 2.5 (Swagger UI) |
| **DevOps** | Multi-stage Dockerfile, Docker Compose, GitHub Actions CI/CD |

---

## 🚀 Getting Started

### Prerequisites
* Java Development Kit (JDK) 17+
* Git

### Local Execution (Dev Profile — H2 File DB)
```bash
# Clone the repository
git clone <YOUR_REPO_URL>
cd "compliment ai"

# Run all automated tests (21 tests)
.\tools\apache-maven-3.9.6\bin\mvn.cmd test

# Run the application
java -jar target\complymind-ai-1.0.0-SNAPSHOT.jar
# or build and run with Maven:
.\tools\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run
```
Open **[http://localhost:8080](http://localhost:8080)** in your browser.

### Default Credentials
* **Username**: `admin`
* **Password**: `Admin@123`

### REST API Documentation
* **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
* **OpenAPI 3.0 Spec**: [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

### Docker Deployment
```bash
# Standalone dev container (H2)
docker compose up -d complymind-dev

# Production container with PostgreSQL 16
docker compose --profile postgres up -d
```

---

## 🧪 Automated Test Suite

Run the full regression test suite:
```bash
.\tools\apache-maven-3.9.6\bin\mvn.cmd test
```
**Results: 21 tests, 0 failures, 0 errors.**
* `ComplyMindApplicationTests` (5 tests) — Schema integrity, demo dataset lifecycle, data isolation.
* `DocumentParserTests` (6 tests) — Text extraction, sha256 checksums, metadata parsing.
* `FullWorkflowIntegrationTests` (7 tests) — End-to-end registration, evidence validation, ripple DAG traversal, root cause investigation, risk scoring, corrective actions, and data ingestion.
* `PasswordResetTests` (1 test) — Secure tokenized password reset flow.
* `RippleEngineTests` (2 tests) — BFS downstream impact propagation & cycle detection.
* `RiskEngineTests` (3 tests) — Deterministic multi-factor weighted risk formulas & band thresholds.

---

## 📄 License
This project is licensed under the Apache License 2.0.
