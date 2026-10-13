# Assessment Attestation

**Full Name:** Anish Achanti

**Email Address:** anishachanti@gmail.com

**Assignment Title:** Build an AI-Assisted Software Engineering System — Audit Log Service

**Start Date:** 9 October 2026

**Submission Date:** 13 October 2026

## Repository and Artifact Binding

**Repository URL:** https://github.com/anishachanti/audit-log-service

**Branch:** main

**Release source commit SHA:** 9147037fdff4c4845177f6ebf9892226a99133ad

**Submission artifact filename:** audit-log-service-submission.zip

**Submission artifact SHA-256:** B42B045BF9C8BA3F0AA9075D208A3A0720AA2B529AC012670C7C7FB503319A44

**CI verification:** GitHub Actions run 2 completed successfully for the stated commit. It ran Maven `clean verify` and uploaded the JaCoCo report as `jacoco-report-2` (available for 30 days): [GitHub Actions run 2](https://github.com/anishachanti/audit-log-service/actions/runs/38430299704)

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


