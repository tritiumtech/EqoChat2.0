# Sprint 12J Implementation Notes: Registry-Only Runtime Reads

Sprint 12J removes Actor subject runtime read-through from source profile tables.

## Scope

- Changed `SubjectDirectoryServiceImpl.getSubject` for `HUMAN` and `AGENT` to return registry rows only.
- Kept `refreshHuman` and `refreshAgent` as explicit sync paths used by `SubjectRegistrySyncApi`.
- Updated Actor contract tests so registry misses return `null` at runtime, while explicit refresh still reads source tables and upserts registry rows.
- Added a static guard rule blocking `getSubject` from reintroducing `return refreshHuman(...)` or `return refreshAgent(...)` source fallback.

## Boundary Decision

`subject_registry` is now the runtime read model for subject display/discovery/profile resolution. `user_profile` and `agent_profile` remain source-of-truth write tables, but source reads are only allowed inside explicit registry sync/adapters.

## Verification

- PASS: `mvn -pl eqochat-business/eqochat-actor-parent/eqochat-actor -am test`
- PASS: `mvn test`
- PASS: `npm run build:h5`
- PASS: `bash scripts/tests/actor-static-guard-test.sh`
- PASS: `bash scripts/actor-static-guard.sh backend frontend`
- PASS: `mvn -pl eqochat-server -am -DskipTests package`
- PASS: backend started from the latest jar on `18080`; Flyway validated 26 migrations and schema was at v26.
- PASS: `BASE_URL=http://127.0.0.1:18080 scripts/smoke/actor-baseline-smoke.sh`

## Remaining Work

- Final guard/smoke closure.
