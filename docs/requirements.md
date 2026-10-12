# Audit Log Service — Requirements Analysis

## 1\. Objective

Build an AI-assisted audit log service that maintains an append-only history of system events and detects unauthorized modifications to previously stored records.

## 2\. Scenario A — Core Audit Log Service

* Accept and validate audit events.
* Store event type, actor ID, resource type, resource ID, payload and timestamp.
* Support event queries using filters and pagination.
* Maintain a hash chain using each event's content hash and the previous record's hash.
* Provide a verification endpoint that reports whether the chain is intact.
* Detect modifications made directly to stored records.

## 3\. Scenario B — Retention and Redaction

* Implement a configurable retention policy.
* Support structured redaction of sensitive payload fields.
* Provide bulk export for a specified resource ID or actor ID.
* Preserve the ability to verify records despite legitimate archival or redaction.

## 4\. Scenario C — Compliance Reporting

Clarify the requirement: "Regulators need to be able to audit access to client account data."

* Identify ambiguities and questions requiring clarification.
* Document assumptions.
* Define the scope of the prototype.
* Translate the clarified requirement into a technical design and implementation.



## 5\. Engineering Principles

* Do not expose update or delete operations for audit events through the API.
* Validate input data.
* Define deterministic serialization for hash calculations.
* Test normal operations, invalid inputs and tampering scenarios.
* Review and validate AI-generated code.
* Document assumptions, risks, trade-offs and limitations.



#### **## 6. Scenario C — Requirement Clarification**



\### **Original requirement**



"Regulators need to be able to audit access to client account data."



\### **Ambiguities identified**



\- Which account-related actions count as access?

\- Which actor and resource identifiers must be searchable?

\- What reporting period and pagination behavior are required?

\- Does the regulator require evidence of successful and failed access, or only recorded events?

\- What report format, authorization controls, and regulatory rules must be supported?



\### **Working assumptions**



\- Existing audit events are the source of truth for recorded activity.

\- Actor ID and resource ID can be used to narrow the report.

\- A report should support a date range and pagination.

\- The report should include the total matching event count and the audit-chain verification result.

\- The prototype should not infer success, failure, or authorization outcomes that are not explicitly recorded.



\### **Clarified requirement**



Provide a compliance-report endpoint that retrieves recorded audit events for a requested time range, with optional actor and resource filters, pagination, a total matching event count, and the verification status of the stored audit chain.



\### **Implemented scope**



\- Reuse the existing event-query functionality.

\- Validate the requested date range.

\- Return report-generation metadata, filters, total event count, paginated events, and chain-verification status.

\- Return a client error for an invalid date range.



\### **Scope boundaries**



\- The report does not independently determine whether access was authorized.

\- It does not distinguish successful from failed access unless that information exists in the recorded events.

\- It does not implement regulator-specific authentication, authorization, or report formats.

\- Chain verification covers the stored chain as a whole, not an independent proof for only the filtered report events.



\### **Validation evidence**



The compliance-report tests cover a matching resource report, a report with no matching events, and inclusion of chain-verification status. The application was also manually tested with an invalid date range, which returned HTTP 400.



The most recent recorded full test run passed 16 tests. Re-run the tests before submission to confirm the current repository state.
