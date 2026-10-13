# Audit Log Service — Requirements Analysis

## 1. Objective

Build an audit log service that records an append-only history of system events and detects unauthorized modifications to stored records.

## 2. Scenario A — Core Audit Log Service

- Accept and validate audit events containing event type, actor ID, resource type, resource ID, and a structured payload.
- Assign the event timestamp on the server.
- Support filtering by actor, resource type, resource ID, event type, and time bounds, with pagination.
- Maintain a SHA-256 hash chain using each event's hash and the previous event's hash.
- Provide a verification endpoint that reports whether the chain is intact and identifies the first detected inconsistency.
- Do not expose general update or delete operations for events.

## 3. Scenario B — Retention and Redaction

- Configure a retention period and archive expired events without removing them from the chain.
- Redact selected top-level fields from an event payload while preserving the original payload hash and event hash.
- Export events by actor ID or resource ID with hash and canonicalization metadata.
- Explain verification limits for redacted payloads and filtered exports.

## 4. Scenario C — Compliance Reporting

### Original requirement

“Regulators need to be able to audit access to client account data.”

### Ambiguities identified

- Which account-related actions count as access?
- Which actor and resource identifiers must be searchable?
- Is a date range mandatory, and what pagination behavior is needed?
- Must reports distinguish successful and failed access, or only list recorded events?
- What report format, authorization controls, and regulatory rules apply?

### Working assumptions

- Existing audit events are the source of truth for recorded activity.
- Actor ID and resource ID can narrow the report.
- The prototype accepts optional `from` and `to` time bounds and supports pagination.
- The report includes a total matching-event count and the verification status of the stored chain.
- The prototype does not infer outcomes or authorization decisions that are absent from events.

### Clarified requirement

Provide a compliance-report endpoint that retrieves recorded audit events, optionally bounded by `from` and `to`, with optional actor and resource filters, pagination, a total matching-event count, and the verification status of the stored audit chain.

### Implemented scope

- Reuse the event-query functionality.
- Validate that `from` is not later than `to` when both are provided.
- Return report metadata, applied filters, total match count, a page of events, and whole-chain verification status.
- Return HTTP 400 for an invalid date range.

### Scope boundaries

- The report does not independently determine whether access was authorized.
- It does not distinguish successful from failed access unless that information is recorded in the event.
- It does not implement regulator-specific authentication, authorization, or report formats.
- Chain verification covers the stored chain as a whole, not an independent proof for only the filtered report events.
- The endpoint currently allows either or both time bounds to be omitted.

## 5. Engineering Principles

- Validate input data and define deterministic serialization for hash calculations.
- Test normal operations, invalid inputs, and tampering scenarios.
- Review AI-generated suggestions and document assumptions, risks, and trade-offs.
- Keep responsibility for correctness, maintainability, and production readiness with the engineer.

## 6. Validation Evidence

Automated tests cover report results, empty results, chain-status inclusion, and invalid date ranges. The API was also exercised end to end; the compliance-report route returned HTTP 200 with matching events and an intact-chain status.
