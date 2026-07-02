# Sprint 13C Implementation Notes: World Explicit Viewer Subject Cleanup

Sprint 13C removes the remaining World read-path fallback that silently treated the authenticated human as the business viewer when a caller omitted subject identity.

## Scope

- Updated `WorldController` read endpoints to require explicit `viewerSubjectId` and `viewerSubjectType`:
  - `GET /api/v1/world/posts`
  - `GET /api/v1/world/subjects/{authorType}/{authorId}/posts`
  - `GET /api/v1/world/topics`
  - `GET /api/v1/world/topics/{name}/posts`
  - `GET /api/v1/world/mentions`
  - `GET /api/v1/world/my-posts`
  - `GET /api/v1/world/posts/{postId}/replies`
- `WorldController.requireExplicitSubject(...)` now rejects missing subject identity with `world.actor.invalid`.
- Removed legacy World service overloads that accepted only a `Long viewerId` and internally converted it to `SubjectRef.human(viewerId)`.
- Kept `principalHumanId` as the authenticated session owner and authorization anchor.
- Updated World service and controller tests to pass explicit `SubjectRef` values.
- Added controller contract coverage for:
  - missing viewer subject rejection,
  - explicit `HUMAN` viewer dispatch to the service.
- Added static guard rules blocking:
  - World service legacy human-default viewer overloads,
  - World controller implicit `SubjectRef.human(principalHumanId)` viewer fallback.

## Boundary Decision

World read APIs now require the frontend or caller to choose the active subject explicitly. This matches the product direction where a human can operate as themselves or as an owned/associated agent, and read personalization must not collapse those identities back into the login principal.

`principalHumanId` is still required for authentication and liability checks. It is not a substitute for the viewer/author/actor subject.

## Verification

- PASS: `mvn -pl eqochat-business/eqochat-world-parent/eqochat-world -am test`
- PASS: `bash scripts/tests/actor-static-guard-test.sh`
- PASS: `bash scripts/actor-static-guard.sh backend frontend`
  - Strict summary: `P0=0 WARN_RUNTIME=0 WARN_HISTORY=28 WARN=28`
- PASS: `mvn test`
- PASS: `npm run build:h5`
- PASS: `mvn -pl eqochat-server -am -DskipTests package`
- PASS: backend started from `backend/eqochat-server/target/eqochat-server-1.0.0.jar` on `18080` with `--spring.profiles.active=local --server.port=18080`
  - Flyway validated 26 migrations.
  - Schema `eqochat2` was current at v26.
- PASS: `BASE_URL=http://127.0.0.1:18080 scripts/smoke/actor-baseline-smoke.sh`
  - Included explicit World viewer and reply viewer smoke coverage.
