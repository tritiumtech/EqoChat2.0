# Sprint 5A Implementation Notes: Project Subject Surface Cut

Date: 2026-06-30

## Scope

Sprint 5A cuts Project API/service/frontend positive paths over to canonical Actor Model subject identity.

- Project owner responses expose `ownerSubjectId/ownerSubjectType`.
- Project member responses expose `memberSubjectId/memberSubjectType`.
- Ownership transfer requests use `newOwnerSubjectId/newOwnerSubjectType`.
- Task creation requests use `assigneeSubjectId/assigneeSubjectType`.
- Sidebar task, payment, and file responses expose `assigneeSubjectId/assigneeSubjectType`, `recipientSubjectId/recipientSubjectType`, and `uploaderSubjectId/uploaderSubjectType`.
- Project business paths accept `HUMAN` and `AGENT`; `SYSTEM` is rejected for owner, transfer target, and task assignee.

## Backend Changes

- `ProjectServiceImpl` now resolves project owner/member/task/payment/file identities through `SubjectRef`.
- Owner/member display data comes from `SubjectDirectoryApi` instead of `UserProfileMapper`.
- Owner authority and agent ownership transfer go through `LiabilityPolicyApi`, keeping principal human, actor subject, and liable human separate.
- Wallet and payment-sidebar routing display go through `WalletPolicyApi`.
- Agent-owned projects preserve the agent as displayed owner and expose associated/liable human transparency metadata separately.
- Project list/detail/access checks account for human membership, agent-owned projects, and agent memberships by subject type/liable human rather than numeric id alone.
- Task creation now requires an explicit canonical assignee subject and verifies project membership with both id and type.
- Project tests cover agent owner creation, rejection of `SYSTEM`, rejection of `USER` through `SubjectType`, agent transfer liability, subject-aware member lookup, same numeric id collision protection, and canonical sidebar subject responses.

## Frontend Changes

- Project API types use canonical subject fields for owner, members, transfer, tasks, payments, and files.
- Project ownership checks use `ownerSubjectId/ownerSubjectType` plus liable/associated human transparency for agent-owned projects.
- Transfer selection keys are subject-aware (`TYPE:ID`) to avoid human/agent numeric collisions.
- Task creation modal now requires selecting a project member as explicit assignee subject.
- Project detail/sidebar UI reads display labels from canonical subject fields instead of legacy `isAgent` flags.

## Smoke Guard Update

- `scripts/smoke/actor-baseline-smoke.sh` now asserts Project list/detail/sidebar responses include canonical subject fields.

## Verification

- `cd backend && mvn -pl eqochat-business/eqochat-project-parent/eqochat-project -am test`
  - PASS, 9 Project actor contract tests.
- `cd frontend && npm run build:h5`
  - PASS.
- `cd backend && mvn -pl eqochat-server -am -DskipTests package`
  - PASS.
- `scripts/actor-static-guard.sh`
  - PASS, `P0=0`, `WARN=43`.
- `git diff --check`
  - PASS.
- Backend runtime:
  - started latest `backend/eqochat-server/target/eqochat-server-1.0.0.jar` with `--spring.profiles.active=local`;
  - Flyway validated 20 migrations;
  - schema current version remained v19.
- `scripts/smoke/actor-baseline-smoke.sh`
  - PASS with Project canonical subject assertions.

## Notes

- Internal Project entity and schema column names such as `owner_id`, `owner_type`, `member_id`, `member_type`, `master_id`, `assignee_id`, `recipient_id`, and `uploaded_by_id` remain because Sprint 5A is an API/service/frontend subject surface cut, not a database column rename.
- Full payment lifecycle writes and file upload actorization remain out of scope for this sprint.
