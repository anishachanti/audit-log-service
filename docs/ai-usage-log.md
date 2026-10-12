\# Audit Log Service — AI Usage Log



\## 1. Purpose



This document records how AI assistance was used during development of the Audit Log Service. AI was used as an engineering aid for understanding requirements, implementation planning, debugging, testing, code review, and documentation.



The engineer remained responsible for executing the changes, reviewing the results, validating behavior, and deciding what was appropriate for the assessment.



This log summarizes the work performed; representative task descriptions are not intended to be a verbatim transcript of every prompt.



\## 2. AI-Assisted Development Activities



\### 2.1 Requirement Analysis and Task Decomposition



\*\*Task:\*\* Understand the assessment and break it into manageable implementation steps.



\*\*AI assistance:\*\* Helped interpret the three scenarios, explain the acceptance criteria, identify ambiguities, and sequence the implementation work.



\*\*Engineer review:\*\* The proposed steps were applied incrementally, with implementation and validation results checked before proceeding.



\### 2.2 Core Audit Log Implementation



\*\*Task:\*\* Implement and understand the REST API, event persistence, query filtering, pagination, and hash-chain verification.



\*\*AI assistance:\*\* Provided explanations of Spring Boot components, hashing, canonicalization, and possible implementation approaches.



\*\*Engineer validation:\*\* The application was built and run locally. API requests and automated tests were used to check implemented behavior.



\### 2.3 Retention and Redaction



\*\*Task:\*\* Extend the service to archive expired events and redact payloads without changing the stored chain hashes.



\*\*AI assistance:\*\* Helped reason about preserving the payload hash separately from the readable payload and about the verification implications of redaction.



\*\*Engineer review:\*\* The implementation was exercised through API calls and tests. The resulting design and its limitations were documented.



\*\*Design trade-off:\*\* The implemented redaction replaces the entire payload with a redaction marker. This is simpler than field-level redaction but does not meet the more granular privacy behavior that could be required in a production system.



\### 2.4 Bulk Export



\*\*Task:\*\* Export events by actor ID or resource ID with verification metadata.



\*\*AI assistance:\*\* Helped design the export bundle, reason about deterministic hash inputs, identify validation cases, and formulate tests for exported hashes.



\*\*Engineer validation:\*\* Export requests were exercised with valid and invalid filter combinations, and automated tests were added for metadata and hash integrity.



\*\*Limitation identified:\*\* Filtered exports may omit predecessor events, and hash values alone do not authenticate the exporter.



\### 2.5 Compliance Reporting



\*\*Task:\*\* Turn the ambiguous requirement about auditing access to client account data into a concrete report endpoint.



\*\*AI assistance:\*\* Helped identify ambiguities, formulate assumptions, design the response fields, and document scope boundaries.



\*\*Engineer validation:\*\* The endpoint was manually exercised, including a reversed date range that returned HTTP 400. Automated tests cover matching reports, empty results, and chain-verification status.



\### 2.6 Debugging and Testing



\*\*Task:\*\* Resolve implementation and build issues and verify the service incrementally.



\*\*AI assistance:\*\* Helped interpret compiler errors, HTTP responses, test results, and configuration details.



\*\*Engineer validation:\*\* Changes were applied locally and checked using Maven builds, automated tests, and API requests.



The latest recorded full test run passed 16 tests. The final test result must be reconfirmed before submission.



\### 2.7 Documentation and Review Preparation



\*\*Task:\*\* Prepare requirements, architecture, testing and limitations, AI-use, and final engineering documentation.



\*\*AI assistance:\*\* Helped organize technical explanations, identify documentation gaps, and prepare clear descriptions of implementation choices and limitations.



\*\*Engineer review:\*\* Documentation was written and saved in the repository, with further consistency checks required before submission.



\## 3. How AI Suggestions Were Evaluated



AI-generated suggestions were treated as proposals rather than automatically trusted output.



The review approach included:



\- Checking suggested changes against the assessment requirements.

\- Building the application after relevant code changes.

\- Running automated tests and manually exercising endpoints.

\- Reviewing hash-chain and redaction trade-offs.

\- Documenting limitations rather than claiming stronger guarantees than the implementation provides.

\- Keeping implementation decisions and final validation under engineer control.



\## 4. Accepted, Adapted, and Deferred Approaches



\### Accepted and implemented



\- Separating controller, service, persistence, and response-model responsibilities.

\- Using SHA-256 and previous-hash links for tamper-evident records.

\- Reusing existing query functionality for compliance reporting.

\- Including explicit verification metadata and limitations in export bundles.



\### Adapted to the assessment scope



\- Using whole-payload redaction rather than field-level redaction.

\- Using H2 file-based persistence for a locally runnable prototype.

\- Implementing a basic compliance report instead of a complete regulator-specific reporting platform.



\### Deferred improvements



\- Digital signatures or independently protected integrity checkpoints.

\- A stronger proof mechanism for filtered exports.

\- Field-level redaction.

\- Production authentication, authorization, monitoring, and load testing.



These are scope decisions and identified future improvements, not claims that the deferred features have been implemented.



\## 5. Security and Confidentiality



Only information permitted for use in the development environment should be supplied to AI tools. Secrets, credentials, customer information, and confidential production data must not be exposed.



The final repository should not contain API keys, passwords, confidential customer records, or generated database files. AI assistance does not transfer responsibility for security, correctness, or maintainability away from the engineer.



\## 6. Ownership Statement



The engineer is responsible for understanding, reviewing, testing, and defending the submitted implementation. The purpose of AI assistance was to accelerate engineering work and improve reasoning, not to replace engineering judgment.



This log should be updated if additional implementation, testing, or review work is performed before submission.
