# Audit Log Service — Testing and Limitations



## 1. Testing Approach



The service is tested using automated tests and manual API checks. Automated tests cover core event operations, integrity verification, retention, export, and compliance reporting.



Run the test suite from the repository root:



```bash

./mvnw clean test

```



Review the Maven output to confirm the result for the current source code.



## 2. Automated Test Coverage



The current test suite includes tests for:



- Application context startup.

- Audit event creation.

- Verification of an intact audit chain.

- Querying events by actor.

- Detection of a tampered event.

- Archiving expired events.

- Exporting events by actor ID and resource ID.

- Rejecting export requests with invalid filter combinations.

- Including export verification metadata.

- Recalculating exported event hashes and payload hashes where applicable.

- Generating compliance reports for matching events.

- Returning an empty report when no events match.

- Including chain-verification status in compliance reports.

- Verifying signed redaction records and detecting tampered redacted payloads or missing proof records.



The latest full automated test run passed. The suite covers core event operations, integrity verification, retention, export, compliance reporting, field-level redaction validation, and redaction-proof tampering cases.



## 3. Manual Validation



Manual checks performed during development included:



- Calling the health endpoint.

- Creating audit events through the REST API.

- Querying events with pagination and filters.

- Verifying the audit chain.

- Testing archival and redaction while preserving stored hash relationships.

- Redacted selected top-level payload fields through the REST API.

- Confirmed unrelated payload fields remained unchanged.

- Confirmed the original payload hash and event hash were preserved.

- Confirmed `/audit/verify` returned `intact: true` and `payloadVerificationStatus: PARTIAL_REDACTED` after valid redaction, with the redacted event sequence number listed.

- Calling export endpoints with valid and invalid filters.

- Generating a compliance report.

- Supplying a reversed date range and confirming that the API returned HTTP 400 with a clear validation message.



Automated testing confirmed that modifying a stored event's actor ID causes hash verification to fail with CONTENT_HASH_MISMATCH.



## 4. Integrity and Redaction Limitations



The service uses SHA-256 hashes and a previous-hash chain to make modifications detectable.



A hash chain does not independently prevent a privileged attacker from changing records and recalculating the hashes. Stronger guarantees would require trusted integrity checkpoints, protected signing keys, or equivalent external evidence.



After a payload is redacted, its original contents are unavailable for reconstruction or rehashing. Verification checks the signed redaction record and current redacted payload against the preserved hashes, but this does not independently prove the erased original contents. Verification depends on the HMAC key remaining secret and stable; anyone with the key can create valid signatures, and HMAC proofs are not publicly verifiable.



## 5. Retention and Export Limitations



The current retention implementation marks events as archived rather than moving them into separate archival storage or permanently deleting them.



A filtered export may omit predecessor events needed to reconstruct the complete chain. The bundle includes verification metadata, but hash values alone do not authenticate the exporter or establish trust in the source.



## 6. Compliance Reporting Limitations



The compliance report provides recorded events, filtering, pagination, total matching event count, and the status of verification of the stored chain.



It does not independently establish whether access was authorized or whether an operation succeeded unless those outcomes are explicitly represented in the stored events. It also does not implement a complete regulator-specific authorization or reporting workflow.



## 7. Production Readiness Gaps



The current implementation is an assessment prototype. Before production use, further work would include:



- Authentication and role-based authorization.

- Production database configuration, backups, and recovery testing.

- Protected integrity checkpoints or digital signatures.

- Review and harden field-level redaction policies, including authorization, nested-field support, and privacy requirements.

- Concurrency, performance, and load testing.

- Security testing and structured operational monitoring.

- A documented and enforced archival and retention policy.



## 8. Quality Gates and Ownership



AI assistance was used during development, but generated suggestions were reviewed and validated through code inspection, API execution, and automated tests. The engineer remains responsible for correctness, security, maintainability, and the final design decisions.



See the AI usage log for a record of assistance, modifications, validation, and accepted or rejected suggestions.
