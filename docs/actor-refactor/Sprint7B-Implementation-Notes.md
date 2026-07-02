# Sprint 7B Implementation Notes: Contact Owner Subject Runtime Surface

Date: 2026-07-01

## Scope

Sprint 7B makes Contact read and tag endpoints accept an explicit owner subject while preserving the authenticated human session as the authorization boundary.

- `principalHumanId` remains the logged-in human from `UserContext`.
- `ownerSubjectId + ownerSubjectType` selects whose contact graph is being read or tagged.
- Missing owner parameters still default to `SubjectRef.human(principalHumanId)`.
- Agent owner access remains authorized by `LiabilityPolicyApi`; an agent owner must resolve to the current principal human.

## Non-Scope

- No new Contact storage migration.
- No `USER` compatibility alias.
- No change to contact request or approval workflows.
- No Agent switcher UI.
- No new Subject Registry.

## Key Changes

- `GET /api/v1/contacts` accepts optional `ownerSubjectId` and `ownerSubjectType`.
- `GET /api/v1/contacts/{targetType}/{targetId}` accepts optional owner subject query parameters.
- `PUT /api/v1/contacts/{targetType}/{targetId}/tags` accepts optional owner subject query parameters.
- Partial owner query parameters are rejected with `contact.subject.invalid`.
- `ContactController` keeps controller logic limited to owner subject parsing and delegates authorization to `ContactService`.
- Frontend `contactApi` methods now accept optional owner subject parameters.
- The Python actor baseline smoke now checks that agent-owned contact queries succeed and do not emit legacy `USER` subject types.

## Tests

Added `ContactControllerOwnerSubjectTest` covering:

- default owner fallback to the principal human;
- explicit Agent owner routing for list, detail, and tag update;
- rejection of partial owner subject parameters.

Extended `ContactServiceImplActorContractTest` covering:

- agent owner list queries use `findActiveFriendsByOwner(agentId, AGENT)`;
- agent owner tag updates scope tag deletion/insertion to `user_type = AGENT`;
- unauthorized agent owners are rejected with `contact.access.denied`.

## Verification Checklist

- PASS: `cd backend && mvn -pl eqochat-business/eqochat-contact-parent/eqochat-contact -am test`
  - Contact implementation module result: `Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`.
- PASS: `cd backend && mvn -pl eqochat-server -am '-DskipTests' package`.
- PASS: `cd frontend && npm run build:h5`.
- PASS: `bash scripts/tests/actor-static-guard-test.sh`.
- PASS: `bash scripts/actor-static-guard.sh`.
  - Result: `P0=0 WARN=67`, files scanned `1024`.
- PASS: `git diff --check`.
  - Output only showed Git CRLF normalization warnings; no whitespace errors were reported.
- PASS: latest backend jar started with `--spring.profiles.active=local`.
  - Flyway validated `20` migrations and reported schema version `20` up to date.
- PASS: `bash scripts/smoke/actor-baseline-smoke.sh`.
  - Runtime smoke included `contacts agent owner`.
- PASS: `bash scripts/smoke-actor-baseline.sh`.
  - Compatibility entry point also included `contacts agent owner`.
- PASS: Evaluator returned explicit `PASS`.

## Known Residuals

- Contact APIs are still human-session APIs. They allow selecting an owner subject, but they do not authenticate as that subject.
- Contact creation and friend request flows remain outside this sprint.
- The database table names still use historical `user_friend` and `user_contact_tag`; typed subject columns are the contract boundary for this sprint.
