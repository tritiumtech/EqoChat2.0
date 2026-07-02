# Sprint 12G Implementation Notes: Credit Audit Actor Fields

Sprint 12G makes Credit audit participants subject-aware without changing credit score calculation or the public credit profile surface.

## Scope

- Added `CreditRecord.operatorType` mapped to `credit_record.operator_type`.
- Added `ViolationRecord.reporterType` mapped to `violation_record.reporter_type`.
- Added `ViolationRecord.reviewerType` mapped to `violation_record.reviewer_type`.
- Kept legacy id column names (`operator_id`, `reporter_id`, `reviewer_id`) to avoid a risky rename in the same sprint.
- Updated `CreditRecordMapper` with subject-aware operator queries:
  - `findByOperator(Long operatorId, String operatorType)`
  - `findByOperator(SubjectRef operator)`
  - legacy `findByOperatorId(Long)` now delegates to `HUMAN`.
- Updated `ViolationRecordMapper` with subject-aware reporter/reviewer queries:
  - `findByReporter(Long reporterId, String reporterType)`
  - `findByReporter(SubjectRef reporter)`
  - `findByReviewer(Long reviewerId, String reviewerType)`
  - `findByReviewer(SubjectRef reviewer)`
  - legacy `findByReporterId(Long)` and `findByReviewerId(Long)` now delegate to `HUMAN`.
- Added Flyway `V25__credit_audit_actor_identity.sql`:
  - backfills historical audit actors to `HUMAN`,
  - adds subject-aware indexes for operator, reporter, and reviewer.
- Extended `CreditProfileServiceImplActorContractTest` to lock mapper SQL on `*_id + *_type`.
- Added a static guard rule blocking Credit mapper SQL from regressing to direct human-only audit actor queries.

## Boundary Decision

Credit subject identity was already canonical. This sprint only closes the audit actor side of the model. A credit record can now say both "the affected subject" and "the operator subject" without assuming the operator is a human.

Existing human-only methods are compatibility wrappers. New write/query code should pass `SubjectRef` or explicit `(id, type)`.

## Verification

- PASS: `mvn -pl eqochat-business/eqochat-credit-parent/eqochat-credit -am test`
- PASS: `bash scripts/tests/actor-static-guard-test.sh`
- PASS: `bash scripts/actor-static-guard.sh backend frontend`

## Remaining Work

- Physical Contact relationship/tag storage still uses `user_friend` and `user_contact_tag` table names.
- Legacy `/api/v1/users` public compatibility surface still needs an explicit retire/deprecate sprint.
- Actor registry source-table fallback remains a controlled migration bridge.
