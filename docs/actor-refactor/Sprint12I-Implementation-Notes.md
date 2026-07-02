# Sprint 12I Implementation Notes: Retire Legacy User Public Surface

Sprint 12I removes the public `/api/v1/users` compatibility surface that previously wrapped actor subject profile APIs.

## Scope

- Removed the legacy user profile/search controller under `/api/v1/users`.
- Removed `UserService` and its actor-backed compatibility implementation.
- Removed legacy `UserSearchResponse` and `UserPublicProfileResponse` DTOs.
- Removed tests that asserted the old route should remain available.
- Added a static guard rule blocking backend runtime code from reintroducing `/api/v1/users`.

## Boundary Decision

Subject discovery and public profiles now belong only to `/api/v1/subjects`. The User module continues to own authentication, human source profile writes, and registry sync, but it no longer exposes a human-only public discovery/profile API.

## Verification

- PASS: `mvn -pl eqochat-business/eqochat-user-parent/eqochat-user -am test`
- PASS: `mvn test`
- PASS: `npm run build:h5`
- PASS: `bash scripts/tests/actor-static-guard-test.sh`
- PASS: `bash scripts/actor-static-guard.sh backend frontend`
- PASS: `mvn -pl eqochat-server -am -DskipTests package`
- PASS: backend started from the latest jar on `18080`; Flyway validated 26 migrations and schema was at v26.
- PASS: `BASE_URL=http://127.0.0.1:18080 scripts/smoke/actor-baseline-smoke.sh`

## Remaining Work

- Actor registry source-table fallback remains a controlled migration bridge.
