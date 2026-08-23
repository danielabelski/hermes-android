# AI Use in Hermes-Android

Hermes-Android uses AI-assisted development as part of day-to-day engineering.
AI tools can speed up exploration, drafting, refactoring, and documentation,
but they do not replace human technical ownership.

## What AI Is Used For

AI assistance may be used for:

- exploring implementation options and tracing existing code paths
- drafting routine code changes and refactors
- writing and extending tests
- improving diagnostics and troubleshooting workflows
- helping maintain documentation and release automation

The amount of assistance varies by task. Some changes are mostly human-authored,
while others may begin with AI-generated drafts.

## Human Ownership and Review

Project maintainers remain responsible for:

- defining requirements and constraints
- architecture and security decisions
- code review and acceptance decisions
- release readiness and publication

AI output is treated as proposed work, not as proof of correctness.

## Validation Standards

Changes are validated according to risk and scope using the same engineering
bar whether AI was involved or not. Validation may include:

- unit tests and build verification
- targeted manual testing on Android devices or emulators
- regression checks for WebView behavior, security boundaries, and release flow

Evidence from tests and runtime behavior determines acceptance.

## Accountability

Final accountability for Hermes-Android remains with human maintainers.
AI tooling assists implementation, but maintainers own the final code,
decisions, and release outcomes.