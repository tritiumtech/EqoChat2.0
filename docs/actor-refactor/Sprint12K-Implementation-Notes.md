# Sprint 12K Implementation Notes: Final Guard And Smoke Closure

Sprint 12K closes the Actor Model refactor hardening run after Sprints 12F-12J.

## Scope

- Added smoke coverage that asserts the retired legacy `/api/v1/users/search` route returns HTTP 404.
- Added `NoResourceFoundException` handling so retired/unknown API paths return HTTP 404 instead of HTTP 200 with a generic system error body.
- Re-ran the full backend, frontend, static guard, package, Flyway startup, and actor baseline smoke gates.

## Final State

- Runtime subject identity is canonical `HUMAN`, `AGENT`, or `SYSTEM`; `USER` remains only in historical migrations/docs warnings.
- Frontend discovery/profile flows use `/api/v1/subjects`.
- The old public `/api/v1/users` search/profile compatibility surface is removed.
- Actor subject runtime reads are registry-only; source profile reads are limited to explicit registry sync/adapters.
- Contact relationship/tag storage uses Contact-owned `contact_relationship` and `contact_tag` physical names, with database compatibility views for old table names.

## Verification

- PASS: `mvn test`
- PASS: `npm run build:h5`
- PASS: `python -m py_compile scripts/smoke/actor-baseline-smoke.py`
- PASS: `bash -n scripts/smoke/actor-baseline-smoke.sh scripts/smoke-actor-baseline.sh scripts/actor-static-guard.sh scripts/tests/actor-static-guard-test.sh`
- PASS: `bash scripts/tests/actor-static-guard-test.sh`
- PASS: `bash scripts/actor-static-guard.sh backend frontend`
  - Final strict summary: `P0=0 WARN_RUNTIME=0 WARN_HISTORY=28 WARN=28`
- PASS: `mvn -pl eqochat-server -am -DskipTests package`
- PASS: backend started from `backend/eqochat-server/target/eqochat-server-1.0.0.jar` on `18080` with `--spring.profiles.active=local`
  - Flyway validated 26 migrations.
  - Schema `eqochat2` was current at v26.
- PASS: `BASE_URL=http://127.0.0.1:18080 scripts/smoke/actor-baseline-smoke.sh`
  - Included `PASS legacy users route retired`.

## Residual Notes

- Historical `WARN_HISTORY=28` findings are limited to old migrations and architecture notes.
- No backend process is left running on `18080` after verification.
