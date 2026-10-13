# Audit Log Service — Final Engineering Summary



## 1. Objective



Build a locally runnable, tamper-evident audit log service that records events, supports querying and verification, and extends the core functionality with retention, redaction, bulk export, and compliance reporting.



The implementation uses Java, Spring Boot, Spring Data JPA, an H2 file-based database, and SHA-256 hashing.



## 2. Implementation Plan and Rationale



The work was organized into three scenarios.



### Scenario A — Core Audit Log



Implemented event creation, filtering, pagination, persistence, and hash-chain verification.



Each event stores its own event hash and a reference to the preceding event's hash. Verification recalculates hashes and checks chain links to identify integrity inconsistencies.



### Scenario B — Retention, Redaction, and Export



Added configurable retention-based archival, payload redaction, and filtered export bundles.



The redaction design preserves the original payload hash and event hash when replacing the readable payload with a redaction marker. The export bundle includes hash algorithm and canonicalization metadata, along with verification scope and limitations.



### Scenario C — Compliance Reporting



Clarified the ambiguous reporting requirement and implemented a report endpoint using existing query and verification functionality.



The report supports date-range filtering, optional actor and resource filters, pagination, total matching event counts, report-generation metadata, and overall chain-verification status.



## 3. Key Artifacts



\- `src/main/java/com/anish/auditlogservice/AuditEventController.java` — REST endpoints.

\- `src/main/java/com/anish/auditlogservice/AuditEventService.java` — core business logic.

\- `src/main/java/com/anish/auditlogservice/AuditEventEntity.java` — persisted audit event model.

\- `src/main/java/com/anish/auditlogservice/AuditEventSpecifications.java` — dynamic query filters.

\- `src/main/java/com/anish/auditlogservice/AuditEventExportBundle.java` — export response model.

\- `src/main/java/com/anish/auditlogservice/ComplianceReport.java` — compliance report model.

\- `src/test/java/com/anish/auditlogservice/AuditLogServiceApplicationTests.java` — automated tests.

\- `docs/requirements.md` — requirements analysis and Scenario C decisions.

\- `docs/architecture.md` — architecture and design trade-offs.

\- `docs/testing-and-limitations.md` — validation approach and limitations.

\- `docs/ai-usage-log.md` — AI assistance and engineering review.

\- `ATTESTATION.md` — assessment attestation.



## 4. Validation



During development, the application was built and run locally. REST endpoints were exercised with valid requests and selected invalid inputs. Automated tests cover core operations, tampering detection, archival, export, and compliance reporting.



The latest recorded full test run passed 16 tests with no failures or errors. Rerun the full test suite before submission and record the final observed result.



## 5. Assumptions



\- Existing audit events are the source of truth for recorded activity.

\- Actor ID and resource ID provide useful report filters.

\- Compliance reports should support time-range filtering and pagination.

\- Integrity status should be included in the report.

\- The prototype should not infer access outcomes or authorization decisions that are not represented in stored event data.



## 6. Risks and Trade-offs



### Hash-chain integrity



SHA-256 and previous-hash links make many changes detectable, but they do not prevent a privileged attacker from modifying records and recalculating hashes.



### Redaction



Whole-payload redaction preserves the stored payload hash and event hash but removes the original content needed to recalculate the original payload hash. Field-level redaction remains a possible improvement.



### Export



A filtered bundle may not include all predecessor events needed to reconstruct the entire chain. Hash metadata alone does not authenticate the exporter.



### Retention



The current implementation marks records as archived. It does not provide a complete separate archival-storage system or permanent-deletion lifecycle.



### Compliance reporting



The report verifies the stored chain as a whole. It does not independently determine whether access was authorized or whether an operation succeeded unless the underlying event contains that information.



### Production readiness



The prototype still needs stronger integrity evidence, production-grade security and storage controls, backups and recovery testing, concurrency and load testing, and operational monitoring.



## 7. AI Assistance and Engineering Ownership



AI assistance was used for requirement analysis, implementation planning, debugging, testing ideas, and documentation. Suggestions were reviewed against the requirements and validated through local builds, tests, and API execution.



The engineer remains responsible for the correctness, security, maintainability, and defensibility of the submitted work. The AI usage log records the assistance used, the scope decisions made, and the limitations identified.



## 8. Final Assessment



The prototype implements the principal functionality across Scenarios A, B, and C and documents significant design trade-offs. The remaining work is to rerun validation, verify documentation consistency, confirm the repository contains no generated database files or secrets, and ensure the private repository is ready for panel access.



This summary describes the implemented prototype and identified limitations; it does not claim full production readiness.
