# Sprint 8A Implementation Notes: Subject Discovery & Public Profile Cut

Date: 2026-07-01

## Scope

Sprint 8A adds a canonical subject discovery and public profile runtime surface, then moves the contact search/profile frontend path off human-only `/api/v1/users/*` APIs.

- `GET /api/v1/subjects/search?keyword=...` returns a list of canonical subjects.
- `GET /api/v1/subjects/{subjectType}/{subjectId}/public` returns a public profile for `HUMAN` or `AGENT`.
- Numeric search can return both `HUMAN:{id}` and `AGENT:{id}` to avoid id collision ambiguity.
- Public profile display facts come from `SubjectDirectoryApi`.
- `worldPostCount` uses `WorldPostStatsApi.countByAuthor(id, type)`.
- `creditScore` is inherited from `SubjectDirectoryApi` and remains in the PRD `300..850` range.
- Contact search/profile pages now use `subjectApi` and navigate with real `targetSubjectType`.

## Non-Scope

- No `subject_registry`.
- No removal of legacy `/api/v1/users/search` or `/api/v1/users/{userId}/public`.
- No Agent switcher UI.
- No independent Agent login/session.
- No fuzzy search or recommendation search.
- No storage migration.

## Key Changes

- Added `SubjectSearchResponse` and `SubjectPublicProfileResponse`.
- Added `SubjectController` under `/api/v1/subjects`.
- Added `SubjectProfileServiceImpl` to assemble canonical search and public profile responses from `SubjectDirectoryApi`, existing profile mappers, contact friendship state, and subject-aware World stats.
- Added exact Agent name lookup to `AgentProfileMapper`.
- Frontend added `subjectApi`.
- Frontend contact search now handles multiple subject results and no longer hardcodes `targetSubjectType=HUMAN`.
- Frontend public profile loads by `targetSubjectType + targetSubjectId`.
- Runtime smoke now checks Agent subject search and Agent public profile.

## Tests

Added `SubjectProfileServiceImplActorContractTest` covering:

- numeric search returns both Human and Agent subjects without `USER`;
- DID/name discovery uses `SubjectDirectoryApi` display facts and deduplicates Agent results;
- Agent public profile uses `countByAuthor(id, "AGENT")`, returns PRD-scale credit score, associated human transparency, and enabled capabilities;
- `SYSTEM` and missing subjects are rejected.

Added `SubjectControllerPublicProfileTest` covering:

- search response preserves canonical subject type;
- public profile routes `AGENT` subject type to service;
- `SYSTEM` and legacy `USER` path subject types are rejected before service calls.

## Verification Checklist

- PASS: `cd backend && mvn -pl eqochat-business/eqochat-actor-parent/eqochat-actor -am test`
  - Actor implementation module result: `Tests run: 20, Failures: 0, Errors: 0, Skipped: 0`.
- PASS: `cd frontend && npm run build:h5`
  - H5 build result: `DONE Build complete`.
- PASS: `cd backend && mvn -pl eqochat-server -am '-DskipTests' package`
  - Reactor result: `BUILD SUCCESS`.
- PASS: `bash scripts/tests/actor-static-guard-test.sh`
- PASS: `bash scripts/actor-static-guard.sh`
  - Summary: `P0=0 WARN=67`.
- PASS: `git diff --check`
  - Only CRLF normalization warnings, no whitespace errors.
- PASS: latest backend jar restarted with `--spring.profiles.active=local`
  - Java: `21.0.11`.
  - Flyway: `Successfully validated 20 migrations`; current schema version `20`.
- PASS: `bash scripts/smoke/actor-baseline-smoke.sh`
  - Includes `PASS subject search agent` and `PASS subject public profile agent`.
- PASS: `bash scripts/smoke-actor-baseline.sh`
  - Includes `PASS subject search agent` and `PASS subject public profile agent`.
- PASS: Evaluator explicit `PASS`
  - Findings: none blocking.
  - Residual risks: exact-match discovery only, legacy `/api/v1/users/*` compatibility remains human-only, and `worldPostCount` uses `Math.toIntExact`.

## Known Residuals

- Legacy human-only `/api/v1/users/*` endpoints remain for compatibility.
- Search is exact-match discovery, not fuzzy search.
- Contact request sending from the normal UI still uses the principal human as actor until an active-subject UI exists.
- Public profile uses existing `user_friend` friendship state instead of a future `subject_registry`.
