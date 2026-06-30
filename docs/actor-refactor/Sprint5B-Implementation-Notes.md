# Sprint 5B Implementation Notes: Project Payment Financial Audit Facts

Date: 2026-06-30

## Scope

Sprint 5B freezes Project payment wallet and liability facts at payment creation time.

- Payment earned/display recipient remains `recipientSubjectId/recipientSubjectType`.
- Direct wallet recipient is stored separately as `directRecipientSubjectId/directRecipientSubjectType`.
- Settlement destination is stored as `settlementSubjectId/settlementSubjectType`, with `settlementHumanId` when funds route through a human wallet.
- Wallet policy facts are stored as `walletRouting`, `financialAutonomy`, `walletPolicyState`, and `walletPolicyReason`.
- Liability facts are stored as `liableHumanId`, `liabilityRoute`, and `liabilityReason`.
- Listing reads persisted payment facts and must not recompute current wallet policy.

## Backend Changes

- Added `CreateProjectPaymentRequest` with canonical `recipientSubjectId/recipientSubjectType`.
- `ProjectService.createPayment` now accepts an explicit canonical payment recipient.
- `ProjectController` exposes `POST /api/v1/projects/{projectId}/payments`.
- `ProjectPayment` persists immutable wallet and liability audit columns.
- `ProjectPaymentResponse` returns the frozen audit facts.
- `ProjectServiceImpl.createPayment`:
  - rejects `SYSTEM` payment recipients;
  - verifies recipient membership by subject id and type;
  - resolves display through `SubjectDirectoryApi`;
  - resolves wallet routing through `WalletPolicyApi`;
  - resolves liability through `LiabilityPolicyApi`;
  - persists the resolved facts into the payment row.
- `ProjectServiceImpl.listSidebarPayments` maps stored payment facts directly and does not call `WalletPolicyApi`.
- `V20__project_payment_financial_audit_facts.sql` adds/backfills the audit columns and canonicalizes historical `recipient_type='USER'` rows to `HUMAN`.
- Removed the Project share-link hardcoded local URL fallback. Share links now come from typed `eqochat.project.share-url-template` configuration; invalid templates fail closed with `project.share_url_template.invalid`.
- Cleaned shared `application.yml` environment values to use environment variables or environment-neutral relative share routes. Local and example values live in profile/example config.

## Frontend Changes

- `frontend/src/api/modules/project.ts` declares the new Project payment audit fields so the UI contract matches backend responses.

## Smoke Guard Update

- `scripts/smoke/actor-baseline-smoke.sh` now asserts Project sidebar payment responses include non-null frozen wallet and liability facts.

## Verification

- `cd backend && mvn -pl eqochat-business/eqochat-project-parent/eqochat-project -am test`
  - PASS, 12 Project actor contract tests.
- `cd backend && mvn -pl eqochat-server -am -DskipTests package`
  - PASS.
- `cd frontend && npm run build:h5`
  - PASS.
- `bash -n scripts/smoke/actor-baseline-smoke.sh scripts/actor-static-guard.sh`
  - PASS.
- `scripts/actor-static-guard.sh`
  - PASS, `P0=0`, `WARN=67`.
- `git diff --check`
  - PASS.
- Backend runtime:
  - started latest `backend/eqochat-server/target/eqochat-server-1.0.0.jar` with `--spring.profiles.active=local`;
  - Flyway validated 21 migrations;
  - schema current version is v20.
- `scripts/smoke/actor-baseline-smoke.sh`
  - PASS with Project payment audit fact assertions.
- Local database audit query:
  - active `project_payment` rows: 3;
  - active rows missing required frozen audit facts: 0.
- Post-BLOCK rerun after removing hardcoded Project share-link fallback:
  - Project tests PASS, 12 tests;
  - backend package PASS;
  - frontend `npm run build:h5` PASS;
  - `bash -n scripts/smoke/actor-baseline-smoke.sh scripts/actor-static-guard.sh` PASS;
  - `scripts/actor-static-guard.sh` PASS, `P0=0`, `WARN=67`;
  - `git diff --check` PASS;
  - latest jar with `--spring.profiles.active=local` PASS, Flyway validated 21 migrations and schema v20;
  - `scripts/smoke/actor-baseline-smoke.sh` PASS;
  - active `project_payment` rows missing required frozen audit facts: 0 of 3.

## Notes

- `recipientSubjectId/recipientSubjectType` remains the earned/display recipient. Agent recipients remain agents even when settlement routes to the owner human.
- `masterWallet` is retained as a display alias for current UI usage, while `walletRouting` is the canonical stored routing fact.
- V20 contains `USER` only as historical-storage cleanup text, which is allowed by the Actor Refactor Contract.
