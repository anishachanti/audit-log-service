# Audit Log Service



A Java and Spring Boot REST API for recording audit events, detecting unauthorized changes through hash-chain verification, archiving old records, redacting payloads, exporting audit events, and generating compliance reports.



## Features



### Scenario A — Core Audit Log

- Create audit events with event type, actor, resource, payload, and timestamp.

- Query events using filters and pagination.

- Maintain a SHA-256 hash chain.

- Verify stored event hashes and chain links.



### Scenario B — Retention and Redaction

- Configure the retention window and archive expired events.

- Redact event payloads while preserving the stored payload hash and event hash.

- Export events by actor ID or resource ID with verification metadata.



### Scenario C — Compliance Reporting

- Generate reports with optional time-range bounds.

- Filter by actor ID or resource ID.

- Return total matching events, paginated results, and audit-chain verification status.



## Technology Stack

- Java

- Spring Boot

- Spring Data JPA

- H2 database

- Maven

- SHA-256 hashing



## Prerequisites

- A compatible JDK installed.

- Git, if cloning the repository.

- Internet access on the first Maven build to download dependencies.



Check the Java version:



```bash

java -version

```



## Build and Run



From the repository root, build and test the application:



```bash

./mvnw clean test

```



Start the application:



```bash

./mvnw spring-boot:run

```



The API is available at:



`http://localhost:8080`



On Windows Command Prompt, use `mvnw.cmd` instead of `./mvnw`.



## API Endpoints



| Method | Endpoint | Purpose |

|---|---|---|

| GET | `/health` | Check service health |
| POST | `/audit/events` | Create an audit event |

| GET | `/audit/events` | Query events with filters and pagination |

| GET | `/audit/verify` | Verify the audit chain |

| PATCH | `/audit/events/{sequenceNumber}/archive` | Archive a specific event |

| POST | `/audit/events/retention/archive-expired` | Archive events beyond the retention window |

| PATCH | `/audit/events/{sequenceNumber}/redact` | Redact an event payload |

| GET | `/audit/events/export?actorId={actorId}` | Export events for an actor |

| GET | `/audit/events/export?resourceId={resourceId}` | Export events for a resource |

| GET | `/audit/events/compliance/report` | Generate a compliance report |



### Example requests



Query events:



```bash

curl "http://localhost:8080/audit/events?page=0&size=10"

```



Verify the audit chain:



```bash

curl "http://localhost:8080/audit/verify"

```



Generate a compliance report:



```bash

curl "http://localhost:8080/audit/events/compliance/report?from=2026-10-01T00:00:00Z&to=2026-10-12T23:59:59Z&page=0&size=10"

```



Export events for an actor:



```bash

curl "http://localhost:8080/audit/events/export?actorId=user-101"

```

Redact selected fields from an existing event (replace `3` with the event's sequence number):

```bash
curl -X PATCH http://localhost:8080/audit/events/3/redact \
  -H "Content-Type: application/json" \
  -d '{"fields":["accountNumber","email"]}'
```


Use the returned event `sequenceNumber` and payload fields appropriate to your data when testing archive and redact operations.



## Configuration
### Redaction proof key

The application requires `AUDIT_REDACTION_HMAC_KEY_BASE64` to sign and verify redaction records. Set it to a Base64-encoded random key of at least 32 bytes. Keep the key outside the repository; the key in `src/test/resources/application.properties` is for tests only.

For a temporary key in PowerShell, run this in the project folder, then start the app in the same terminal:

```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$env:AUDIT_REDACTION_HMAC_KEY_BASE64 = [Convert]::ToBase64String($bytes)
$rng.Dispose()
Remove-Variable bytes, rng

.\mvnw.cmd spring-boot:run
```

The generated key is available only in that terminal session. Generate and securely store a stable key for environments where the same redaction records must remain verifiable across restarts.



The retention period is configurable through the application properties:



```properties

audit.retention.days=365

```



Check `src/main/resources/application.properties` for the active database configuration and other settings.



## Documentation



- [Requirements analysis](docs/requirements.md)
- [Architecture overview](docs/architecture.md)
- [Testing and limitations](docs/testing-and-limitations.md)
- [AI usage log](docs/ai-usage-log.md)
- [Final engineering summary](docs/final-engineering-summary.md)
- [Attestation](ATTESTATION.md)



## Testing



Run the automated test suite:



```bash

./mvnw clean test

```



The suite covers core event operations, integrity verification, archival, export, compliance reporting, field-level redaction validation, and redaction-proof tampering cases. The latest full test run passed.



## Known Limitations



- A hash chain provides tamper evidence but is not tamper-proof if an attacker can modify records and recalculate hashes.

- For redacted events, `/audit/verify` checks the HMAC-SHA256 signature on the redaction record, confirms the record is tied to the event payload hash, and checks the current redacted payload against its signed hash. It reports `PARTIAL_REDACTED` because the removed original payload cannot be reconstructed or rehashed. Verification depends on keeping the HMAC key secret and stable; anyone with the key can create valid signatures, and HMAC proofs are not independently verifiable by the public.

- Filtered exports may not contain the full chain history, and hash metadata alone does not authenticate the exporter.

- The compliance report verifies the stored chain as a whole and does not independently establish whether access was authorized.

- H2 and the current archival implementation are intended for this prototype, not a complete production retention architecture.



See the assessment documentation for design decisions, validation details, trade-offs, and limitations.
