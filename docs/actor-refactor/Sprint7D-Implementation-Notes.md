# Sprint 7D Implementation Notes: Credit Profile PRD Score Surface

Date: 2026-07-01

## Scope

Sprint 7D makes the Credit Profile API expose `creditScore` in the PRD `300-850` range while preserving existing storage and query contracts.

- `GET /api/v1/credits/subject` continues to read by canonical `subjectId + subjectType`.
- Existing `credit_record.current_score` and profile-table `credit_score` values are adapted at the service boundary.
- Already migrated `300-850` scores pass through unchanged.
- Historical `0-100` scores are linearly adapted to `300-850`.
- Missing scores default to `300`.

## Non-Scope

- No `subject_credit_profile` table migration.
- No rewrite of `user_profile.credit_score` or `agent_profile.credit_score`.
- No behavior points ledger.
- No wallet unlock or milestone policy change.
- No owner liability side effect for violations.
- No Project, Agent, User, or Subject Directory response migration beyond the Credit Profile API.

## Key Changes

- `CreditProfileServiceImpl` now adapts profile and latest-record scores before building `CreditProfileResponse`.
- Legacy score adapter semantics match the existing Actor module adapter:
  - `null -> 300`
  - `0 -> 300`
  - `100 -> 850`
  - `300..850 -> unchanged`
  - out-of-range values are clamped to `300..850`
- Runtime smoke now checks `/api/v1/credits/subject` for a contact target and asserts `creditScore` is within `300..850`.

## Tests

Extended `CreditProfileServiceImplActorContractTest` covering:

- legacy `USER` subject type rejection without mapper fallback;
- canonical `HUMAN` subject queries;
- legacy profile score adaptation from `0-100` to `300-850`;
- latest `credit_record.current_score` adaptation when still in legacy scale;
- missing profile score defaulting to `300`.

## Verification Checklist

- PASS: `cd backend && mvn -pl eqochat-business/eqochat-credit-parent/eqochat-credit -am test`
  - Credit implementation module result: `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`.
- PASS: `cd backend && mvn -pl eqochat-server -am '-DskipTests' package`.
- PASS: `cd frontend && npm run build:h5`.
- PASS: `bash scripts/tests/actor-static-guard-test.sh`.
- PASS: `bash scripts/actor-static-guard.sh`.
  - Result: `P0=0 WARN=67`, files scanned `1025`.
- PASS: `git diff --check`.
  - Output only showed Git CRLF normalization warnings; no whitespace errors were reported.
- PASS: latest backend jar started with `--spring.profiles.active=local`.
  - Flyway validated `20` migrations and reported schema version `20` up to date.
- PASS: `bash scripts/smoke/actor-baseline-smoke.sh`.
  - Runtime smoke included `credit profile prd score`.
- PASS: `bash scripts/smoke-actor-baseline.sh`.
  - Compatibility entry point also included `credit profile prd score`.
- PASS: Evaluator returned explicit `PASS`.

## Known Residuals

- Other API surfaces may still expose legacy or mixed credit fields, including Agent, Project member, User info, and Subject Directory summaries.
- The PRD still contains milestone thresholds like `>50`, `>100`, and `>250`; this sprint does not reinterpret milestone policy.
- Historical database columns still advertise `0-100` comments/checks in initial migrations; this sprint adapts output without rewriting historical schema.
