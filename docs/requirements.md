# Audit Log Service — Requirements Analysis

## 1. Objective

Build an AI-assisted audit log service that maintains an append-only history of system events and detects unauthorized modifications to previously stored records.

## 2. Scenario A — Core Audit Log Service

- Accept and validate audit events.
- Store event type, actor ID, resource type, resource ID, payload and timestamp.
- Support event queries using filters and pagination.
- Maintain a hash chain using each event's content hash and the previous record's hash.
- Provide a verification endpoint that reports whether the chain is intact.
- Detect modifications made directly to stored records.

## 3. Scenario B — Retention and Redaction

- Implement a configurable retention policy.
- Support structured redaction of sensitive payload fields.
- Provide bulk export for a specified resource ID or actor ID.
- Preserve the ability to verify records despite legitimate archival or redaction.

## 4. Scenario C — Compliance Reporting

Clarify the requirement: "Regulators need to be able to audit access to client account data."

- Identify ambiguities and questions requiring clarification.
- Document assumptions.
- Define the scope of the prototype.
- Translate the clarified requirement into a technical design and implementation.


## 5. Engineering Principles

- Do not expose update or delete operations for audit events through the API.
- Validate input data.
- Define deterministic serialization for hash calculations.
- Test normal operations, invalid inputs and tampering scenarios.
- Review and validate AI-generated code.
- Document assumptions, risks, trade-offs and limitations.