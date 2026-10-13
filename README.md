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



Before starting, configure the redaction proof key and the admin/reader credentials described under [Configuration](#configuration). The application will not start if required secrets are missing.

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



### Authentication and authorization

The API uses HTTP Basic authentication. Configure two accounts using environment variables: an administrator (`AUDIT_API_USERNAME` and `AUDIT_API_PASSWORD`) and a read-only user (`AUDIT_READER_USERNAME` and `AUDIT_READER_PASSWORD`). Users are stored in memory and recreated from these settings each time the application starts.

Both roles can query, export, generate compliance reports, and verify the chain. Only administrators can create events, redact payloads, archive events, or run retention archival. Requests without valid credentials are rejected. The API is configured as stateless and disables CSRF because it uses HTTP Basic authentication for API clients. State-changing requests require administrator credentials but do not require a CSRF token. Use HTTPS when deploying beyond a local development environment because HTTP Basic credentials must be protected in transit.

### Example requests

The examples below require the reader or administrator credentials. With curl, add `-u username` to be prompted for the password, for example `curl -u "$AUDIT_READER_USERNAME" ...`.

Query events:



```bash

curl -u "$AUDIT_READER_USERNAME" "http://localhost:8080/audit/events?page=0&size=10"

```



Verify the audit chain:



```bash

curl -u "$AUDIT_READER_USERNAME" "http://localhost:8080/audit/verify"

```



Generate a compliance report:



```bash

curl -u "$AUDIT_READER_USERNAME" "http://localhost:8080/audit/events/compliance/report?from=2026-10-01T00:00:00Z&to=2026-10-12T23:59:59Z&page=0&size=10"

```



Export events for an actor:



```bash

curl -u "$AUDIT_READER_USERNAME" "http://localhost:8080/audit/events/export?actorId=user-101"

```

Redact selected fields from an existing event (replace `3` with the event's sequence number):

```bash
curl -u "$AUDIT_API_USERNAME" -X PATCH http://localhost:8080/audit/events/3/redact \
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



To generate the JaCoCo report and enforce the coverage thresholds, run `./mvnw clean verify` (or `.\mvnw.cmd clean verify` in PowerShell). The HTML report is written to `target/site/jacoco/index.html`. The build checks at least 50% overall line coverage, 25% overall branch coverage, and 50% line coverage for `AuditEventService`, `RedactionSignatureService`, and `SecurityConfig`. The 13 October 2026 `clean verify` run passed with 81.7% overall line coverage and 71.2% branch coverage; the three critical classes achieved 88.5%, 80.6%, and 100% line coverage, respectively.



The suite covers core event operations, integrity verification, archival, export, compliance reporting, field-level redaction validation, redaction-proof tampering cases, role-based API access, and a concurrent append check.

GitHub Actions runs `clean verify` on pushes and pull requests targeting `main`. Each run uploads the generated JaCoCo HTML report as the `jacoco-report-<run number>` artifact for 30 days. Open the workflow run page and use its Artifacts section to download it. A report may be absent if the build fails before JaCoCo creates it. Each run uploads the generated JaCoCo HTML report as the jacoco-report-<run number> artifact for 30 days. Open the workflow run's **Artifacts** section to download it. The report is available only when JaCoCo generated it; a failed or interrupted build may have no report. The full suite passed on 13 October 2026 with 31 tests, 0 failures, 0 errors, and 0 skipped tests using `.\mvnw.cmd clean test` from PowerShell.



## Known Limitations



- A hash chain provides tamper evidence but is not tamper-proof if an attacker can modify records and recalculate hashes.

- For redacted events, `/audit/verify` checks the HMAC-SHA256 signature on the redaction record, confirms the record is tied to the event payload hash, and checks the current redacted payload against its signed hash. It reports `PARTIAL_REDACTED` because the removed original payload cannot be reconstructed or rehashed. Verification depends on keeping the HMAC key secret and stable; anyone with the key can create valid signatures, and HMAC proofs are not independently verifiable by the public.

- Filtered exports may not contain the full chain history, and hash metadata alone does not authenticate the exporter.

- The compliance report verifies the stored chain as a whole and does not independently establish whether access was authorized.

- User accounts are held in memory and supplied through environment variables. There is no user lifecycle, external identity provider, or persistent account store. Protect credentials and use HTTPS outside local development.

- The HMAC key must remain stable to verify existing redaction records. Key rotation and key-version tracking are not implemented; anyone who obtains the key can create signatures that verify.

- A concurrency test starts 8 event writes together and verifies contiguous sequence numbers, hash links, the final chain head, and chain verification in H2. Higher-volume stress and performance testing, including validation against a production database, has not been completed.

- H2 and the current archival implementation are intended for this prototype, not a complete production retention architecture.



See the assessment documentation for design decisions, validation details, trade-offs, and limitations.



