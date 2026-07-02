# Sprint 13L Implementation Notes

Sprint 13L is the final guard, documentation, and runtime verification closure for the actor-model refactor pass.

## Changes

- Updated `HARNESS.md` with Sprint 13J/13K outcomes and current guard/test counts.
- Updated `Actor-Refactor-Contract.md` with Project write principal/actor rules and wallet settlement-subject rules.
- Updated `backend/ACTOR-MODEL-ARCHITECTURE.md` to reflect the current registry/subject-native runtime instead of early mirror-profile transition notes.
- Re-ran final guard, frontend build, backend tests, package build, and actor baseline smoke.

## Verification

- PASS: `bash scripts/tests/actor-static-guard-test.sh`
- PASS: `bash scripts/actor-static-guard.sh backend frontend`
  - `P0=0 WARN_RUNTIME=0 WARN_HISTORY=24 WARN=24`
- PASS: `npm run build:h5`
- PASS: `mvn test`
- PASS: `mvn -pl eqochat-server -am -DskipTests package`
- PASS: `BASE_URL=http://127.0.0.1:18080 scripts/smoke/actor-baseline-smoke.sh`

## Runtime Cleanup

- Local smoke server ran on port `18080`.
- Java process was stopped after smoke.
- Final port check: no listener on `18080`.

## Closure

The main runtime actor-model path is closed for this refactor series: business viewer/actor/owner/recipient/author identity must be explicit subject identity, representation authority is registry-authoritative, wallet settlement facts are policy-owned, and static guard blocks the known regression patterns.
