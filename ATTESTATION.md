# Assessment Attestation

**Full Name:** Anish Achanti

**Email Address:** anishachanti@gmail.com

**Assignment Title:** Build an AI-Assisted Software Engineering System — Audit Log Service

**Start Date:** 9 October 2026

**Submission Date:** 13 October 2026

## Repository and Artifact Binding

**Repository URL:** https://github.com/anishachanti/audit-log-service

**Branch:** main

**Release source commit SHA:** 5ece43dfaa071358923d10ac1f3d7b06183edf6c

**Submission artifact filename:** audit-log-service-5ece43dfaa071358923d10ac1f3d7b06183edf6c.zip

**Submission artifact SHA-256:** 154EDCA07195BE97C877592636936F9EDF7739933C08370BA281283943178CEA

**CI verification:** **CI verification:** GitHub Actions Java CI run #4 completed successfully for release source commit `5ece43dfaa071358923d10ac1f3d7b06183edf6c`. The Tests and coverage job passed, and the workflow uploaded the JaCoCo report as `jacoco-report-4` (272 KB, available for the retention period configured by GitHub Actions). [GitHub Actions run #4](https://github.com/anishachanti/audit-log-service/actions/runs/38540402866) · [JaCoCo report artifact](https://github.com/anishachanti/audit-log-service/actions/runs/38540402866/artifacts/11882280983)

## Scope, Exclusions, and Third-Party Components

**Scope:** Application source, tests, documentation, and Maven build configuration at the stated commit. The submission ZIP contains those tracked project files and excludes Git metadata, this ATTESTATION.md sidecar, generated target/ output, local data/ files, and .env files. This attestation is supplied separately because it records the ZIP checksum.

**Generated/local files:** The submission ZIP was checked to exclude `target/`, `data/`, `.env` files, and Git metadata.

**Third-party components:** Dependencies are declared in `pom.xml` and resolved through Maven. Their source code is not claimed as my original work.

## Claims and Evidence

| Claim | Code evidence | Test evidence |
|---|---|---|
| Events are chained and tampering is detected | `AuditEventService.java`, `HashService.java`, `AuditChainHeadRepository.java` | `shouldCreateAuditEvent`, `shouldVerifyAnUntamperedChain`, `shouldDetectTamperedEvent` |
| Redactions retain signed evidence and report verification limits | `AuditEventService.java`, `RedactionSignatureService.java`, `RedactionRecordEntity.java` | `shouldRedactSelectedPayloadFieldsAndPreserveHashChain`, `shouldReportRedactionLimitationDuringChainVerification`, `shouldDetectTamperingWithRedactedPayload` |
| Readers are restricted to assigned resources in queries, exports, and reports | `SecurityConfig.java`, `AuditEventService.java`, `AuditEventSpecifications.java` | `readerCannotQueryUnassignedResource`, `readerCannotExportUnassignedResourceEvents`, `readerComplianceReportIncludesOnlyAssignedResources` |
| Authentication and reader/admin permissions are checked | `SecurityConfig.java` | `rejectsMissingAndInvalidCredentials`, `readerCanViewEventsButCannotCreateThem`, `adminCanCreateEvent` |

## Attestation Statement

I, Anish Achanti, attest that this submission is my own individual work, completed on my own machine and accounts, and that it honestly reflects my development process and use of AI.


