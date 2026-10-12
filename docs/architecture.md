\# Audit Log Service — Architecture Overview



\## 1. Purpose



The Audit Log Service is a Java and Spring Boot REST API that records audit events, supports event queries, and detects changes to stored records using a SHA-256 hash chain.



The prototype implements three scenarios:

\- Scenario A: Core audit logging and integrity verification.

\- Scenario B: Retention-based archival, payload redaction, and bulk export.

\- Scenario C: Compliance reporting for recorded account-related activity.



\## 2. Technology Stack



\- Java

\- Spring Boot

\- Spring Data JPA

\- H2 file-based database

\- Maven

\- SHA-256 for payload and event hashes



\## 3. Architecture Components



\### Controller



`AuditEventController` exposes REST endpoints, receives request parameters, calls the service layer, and returns HTTP responses.



\### Service



`AuditEventService` implements event creation, query validation, hash calculation, chain verification, archival, redaction, export, and compliance-report generation.



\### Repository



The Spring Data JPA repository persists audit events and retrieves them using identifiers, filters, and ordering.



\### Entity and DTOs



\- `AuditEventEntity` represents persisted audit events and their integrity and lifecycle metadata.

\- `AuditEventExportBundle` represents exported events and associated verification metadata.

\- `ComplianceReport` represents report metadata, matching events, and chain-verification status.

\- `AuditEventSpecifications` composes optional query filters.



\## 4. Data Model



Audit events include event metadata such as event type, actor ID, resource type, resource ID, timestamp, and payload. Integrity fields include the previous-event hash, event hash, and payload hash. Lifecycle metadata tracks archival and redaction.



The first event uses a defined genesis value as its previous hash. Subsequent events reference the preceding event's hash.



\## 5. Hash Chain Design



The service uses SHA-256 to calculate payload and event hashes. The event hash input includes event metadata and the stored payload hash. Hash input data is canonicalized using sorted keys where applicable to make serialization deterministic.



During verification, the service checks previous-hash links and recalculates event hashes. For non-redacted events, it also recalculates the payload hash from the stored payload.



SHA-256 is used for integrity checking, not encryption. A hash chain provides tamper evidence but does not prevent an attacker with sufficient database access from rewriting records and recalculating hashes.



\## 6. API Design



The API includes:

\- Event creation and filtered, paginated queries.

\- Full-chain verification.

\- Explicit event archival and retention-based archival.

\- Payload redaction.

\- Bulk export filtered by actor ID or resource ID.

\- Compliance reporting with date filters, optional actor/resource filters, pagination, total matching events, and chain-verification status.



Invalid query parameters, including an invalid date range or invalid export-filter combination, are rejected with a client error.



\## 7. Key Design Decisions and Trade-offs



\### File-based H2 database



H2 provides a simple local persistence layer suitable for a self-contained assessment prototype. It is not intended to represent the final production storage architecture.



\### Retention through archival



Archival metadata allows records to be marked as archived without physically deleting them. This preserves stored event data and avoids removing records that participate in the hash chain.



\### Redaction using a preserved payload hash



The readable payload can be replaced with a redaction marker while retaining the original payload hash and event hash. This preserves the chain's stored hash relationships, but the original payload cannot be independently rehashed once removed.



\### Filtered export



The export includes hash and canonicalization metadata so a recipient can recalculate hashes for the supplied data. A filtered export may omit predecessor events, and the hashes alone do not authenticate the exporter.



\### Compliance report



The report reuses existing query and chain-verification functionality. Its integrity status describes the stored chain as a whole and does not establish whether a recorded access was authorized.



\## 8. Production Considerations



Potential future improvements include stronger protection of integrity checkpoints using digital signatures, authentication and authorization, production-grade database operations and backups, field-level redaction, more complete export proofs, and concurrency and performance testing.
