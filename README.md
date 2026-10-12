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

- Generate reports for a requested time range.

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

curl "http://localhost:8080/audit/events?page=0\&size=10"

```



Verify the audit chain:



```bash

curl "http://localhost:8080/audit/verify"

```



Generate a compliance report:



```bash

curl "http://localhost:8080/audit/events/compliance/report?from=2026-10-01T00:00:00Z\&to=2026-10-12T23:59:59Z\&page=0\&size=10"

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


Use the actual event IDs and request fields appropriate to your data when testing create, archive, and redact operations.



## Configuration



The retention period is configurable through the application properties:



```properties

audit.retention.days=365

```



Check `src/main/resources/application.properties` for the active database configuration and other settings.



## Documentation



- Documentation

- Requirements analysis

- Architecture

- Testing and limitations

- AI usage log

- Final engineering summary

- Attestation


Additional architecture, testing, AI-usage, and engineering-summary documentation will be added as those documents are completed.



## Testing



Run the automated test suite:



```bash

./mvnw clean test

```



Review the test output to confirm the current result. The tests cover core event operations, integrity verification, archival, export, and compliance reporting.



## Known Limitations



- A hash chain provides tamper evidence but is not tamper-proof if an attacker can modify records and recalculate hashes.

- Redacted payloads cannot be compared with the original payload after the original content is removed.

- Filtered exports may not contain the full chain history, and hash metadata alone does not authenticate the exporter.

- The compliance report verifies the stored chain as a whole and does not independently establish whether access was authorized.

- H2 and the current archival implementation are intended for this prototype, not a complete production retention architecture.



See the assessment documentation for design decisions, validation details, trade-offs, and limitations.
