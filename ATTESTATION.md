# Assessment Attestation

**Full Name:** Anish Achanti

**Email Address:** anishachanti@gmail.com

**Assignment Title:** Build an AI-Assisted Software Engineering System — Audit Log Service

**Start Date:** 9 October 2026

**Submission Date:** 13 October 2026

## Repository and Artifact Binding

**Repository URL:** https://github.com/anishachanti/audit-log-service

**Branch:** main

**Commit SHA:** Pending final commit

**Submission artifact filename:** Pending

**Submission artifact SHA-256:** Pending

## Scope, Exclusions, and Third-Party Components

**Scope:** All files tracked at the stated repository commit, including application source, tests, documentation, and Maven build configuration.

**Generated/local files:** `target/` build output, `data/` local H2 database files, and `.env` files are ignored by Git. Their absence from the final submission archive must be confirmed separately.

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