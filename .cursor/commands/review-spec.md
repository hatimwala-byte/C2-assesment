# Command: Review Spec

Audit the specification documents in the `spec/` folder against the project goals, domain rules, and assessment acceptance criteria.

## Review Process
1. Read all active specifications in `spec/` (e.g., requirements, domain models, API specifications, user stories).
2. Cross-examine the specifications for:
   - **Contradictions**: Conflicting rules, overlapping states, or inconsistent field types across documents.
   - **Missing Edge Cases**: Unhandled error paths, boundary limits (e.g., max string lengths, empty search results), concurrency scenarios, or unauthorized state transitions.
   - **Ambiguous Requirements**: Vaguely worded criteria (e.g., "fast response", "handle gracefully") lacking concrete acceptance conditions or validation constraints.
   - **Gaps against Acceptance Criteria**: Missing features, unmapped transitions, or unspecified CRUD/filter operations required for a complete Support Ticket Management System.

## Output Format
Present a structured report containing:
1. **Summary of Findings**: High-level verdict on specification completeness.
2. **Contradictions & Inconsistencies**: Specific conflicts identified with document and section references.
3. **Missing Edge Cases**: Concrete scenarios that need explicit handling rules defined.
4. **Ambiguities & Clarifications Needed**: Items requiring precise definitions.
5. **Recommended Revisions**: Concrete text or schema updates proposed for the specification files.

## Action Policy
**Do not modify or edit any specification files automatically.** Present the analysis and recommendations, then explicitly ask the user for confirmation before making any changes to files.
